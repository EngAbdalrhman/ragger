package com.example.springai.conversation;

import com.example.springai.analysis.DocumentAnalyzer;
import com.example.springai.analysis.DocumentAnalyzer.AnalysisResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {
    private final DocumentAnalyzer documentAnalyzer;
    private final ConversationLogger conversationLogger;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final Map<String, ConversationState> userStates = new HashMap<>();
    private static final int SESSION_TIMEOUT_MINUTES = 30;

    public String processUserInput(String userId, String input) {
        ConversationState state = userStates.computeIfAbsent(userId, k -> new ConversationState());
        
        // Check for session timeout
        if (state.isExpired(SESSION_TIMEOUT_MINUTES)) {
            state.clear();
            log.info("Session expired for user: {}", userId);
        }

        try {
            // Handle reset command
            if (input.toLowerCase().equals("reset")) {
                state.clear();
                userStates.remove(userId);
                return "Conversation reset successfully.";
            }

            // Analyze intent
            Intent intent = analyzeIntent(input.toLowerCase());
            state.setCurrentIntent(intent);

            // Process input based on intent
            String response = handleIntent(userId, input, state);

            // Log successful interaction
            conversationLogger.logConversation(
                userId, input, response, intent.toString(), state.toMap(), true, null
            );

            return response;
        } catch (Exception e) {
            log.error("Error processing user input", e);
            conversationLogger.logConversation(
                userId, input, null, state.getCurrentIntent() != null ? 
                    state.getCurrentIntent().toString() : "UNKNOWN",
                state.toMap(), false, e.getMessage()
            );
            return "I encountered an error. Please try again or rephrase your request.";
        }
    }

    public String processDocument(String userId, MultipartFile file) {
        try {
            // Analyze document
            AnalysisResult analysis = documentAnalyzer.analyzeDocument(file);
            
            // Get or create conversation state
            ConversationState state = userStates.computeIfAbsent(userId, k -> new ConversationState());
            state.initializeAnalysisContext(file.getOriginalFilename());
            
            // Update state with analysis results
            updateStateFromAnalysis(state, analysis);

            // Log document analysis
            ObjectNode analysisResult = analysis.toJson(objectMapper); // Your ObjectNode
            Map<String, Object> analysisMap = objectMapper.convertValue(analysisResult, Map.class);
            conversationLogger.logDocumentAnalysis(
                userId,                      // String
                file.getOriginalFilename(),  // String
                analysisMap,                 // Map<String, Object>
                analysis.getMissingInformation(), // List<String>
                true,                       // boolean
                null                        // String
            );

            // Generate response based on analysis
            StringBuilder response = new StringBuilder();
            response.append("I've analyzed your document and found:\n");
            if (analysis.getAppName() != null) {
                response.append("- App Name: ").append(analysis.getAppName()).append("\n");
                state.setAppName(analysis.getAppName());
                state.setAppIdentifier(generateIdentifier(analysis.getAppName()));
            }
            if (analysis.getModules() != null && !analysis.getModules().isEmpty()) {
                response.append("- Modules:\n");
                analysis.getModules().forEach(module -> {
                    response.append("  * ").append(module.getName()).append("\n");
                    state.addModule(module.getName());
                });
            }

            // Add missing information prompts
            if (!analysis.getMissingInformation().isEmpty()) {
                response.append("\nI need some clarification:\n");
                analysis.getMissingInformation().forEach(info -> 
                    response.append("- ").append(info).append("\n"));
            } else {
                response.append("\nWould you like to add UDAs to any of these modules? (e.g., 'Add a text UDA called Description to module X')");
                state.setCurrentIntent(Intent.ADD_UDA);
            }

            return response.toString();
        } catch (Exception e) {
            log.error("Error processing document", e);
            conversationLogger.logDocumentAnalysis(
                userId, file.getOriginalFilename(), null, null, false, e.getMessage()
            );
            return "I encountered an error analyzing the document. Please ensure it's in a supported format and try again.";
        }
    }

    private String handleIntent(String userId, String input, ConversationState state) {
        return switch (state.getCurrentIntent()) {
            case CREATE_APP -> handleCreateApp(userId, input, state);
            case ADD_MODULE -> handleAddModule(userId, input, state);
            case ADD_UDA -> handleAddUda(userId, input, state);
            case SET_PRIVILEGES -> handleSetPrivileges(userId, input, state);
            case BUILD -> handleBuild(userId, state);
            case UNKNOWN -> "I'm not sure what you want to do. Would you like to create an app, " +
                          "add a module, add UDAs, or set privileges?";
        };
    }

    private String handleCreateApp(String userId, String input, ConversationState state) {
        String appName = extractAppName(input);
        if (appName != null) {
            // Validate app name
            if (!isValidAppName(appName)) {
                conversationLogger.logValidation(state.getConversationId(), 
                    "appName", appName, false, "Invalid app name format");
                return "App name can only contain letters, numbers, and hyphens. Please try again.";
            }

            state.setAppName(appName);
            state.setAppIdentifier(generateIdentifier(appName));
            
            conversationLogger.logValidation(state.getConversationId(), 
                "appName", appName, true, null);

            return "Great! I'll create an app called '" + appName + 
                   "'. What modules would you like to add to it?";
        }

        return "What would you like to name your app?";
    }

    private String handleAddModule(String userId, String input, ConversationState state) {
        if (state.getAppName() == null) {
            return "First, I need to know which app you're working with. " +
                   "Please tell me the app name.";
        }

        String moduleName = extractModuleName(input);
        if (moduleName != null) {
            if (!isValidModuleName(moduleName)) {
                conversationLogger.logValidation(state.getConversationId(), 
                    "moduleName", moduleName, false, "Invalid module name format");
                return "Module name can only contain letters and numbers. Please try again.";
            }

            state.addModule(moduleName);
            
            conversationLogger.logValidation(state.getConversationId(), 
                "moduleName", moduleName, true, null);

            return "I've added the '" + moduleName + "' module. " +
                   "What fields would you like to add to this module?";
        }

        return "What would you like to name your module?";
    }

    private String handleAddUda(String userId, String input, ConversationState state) {
        if (state.getCurrentModule() == null) {
            return "Which module would you like to add fields to?";
        }

        Map<String, String> fieldInfo = extractFieldInfo(input);
        if (!fieldInfo.isEmpty()) {
            String fieldName = fieldInfo.get("name");
            String fieldType = fieldInfo.get("type");

            if (!isValidFieldName(fieldName)) {
                conversationLogger.logValidation(state.getConversationId(), 
                    "fieldName", fieldName, false, "Invalid field name format");
                return "Field name can only contain letters and numbers. Please try again.";
            }

            state.addField(state.getCurrentModule(), fieldName, fieldType);
            
            conversationLogger.logValidation(state.getConversationId(), 
                "field", fieldName + ":" + fieldType, true, null);

            return "Added field '" + fieldName + "' of type '" + fieldType + 
                   "'. Would you like to add another field?";
        }

        return "What field would you like to add? Please specify the name and type " +
               "(e.g., 'Add a name field of type text')";
    }

    private String handleSetPrivileges(String userId, String input, ConversationState state) {
        if (state.getAppName() == null) {
            return "Which app would you like to set privileges for?";
        }

        Map<String, String> privilegeInfo = extractPrivilegeInfo(input);
        if (!privilegeInfo.isEmpty()) {
            state.setPrivileges(privilegeInfo);
            return "I've set the privileges as specified. Would you like to build the app now?";
        }

        return "What privileges would you like to set? (e.g., 'Set read access for users')";
    }

    private String handleBuild(String userId, ConversationState state) {
        try {
            List<String> missing = validateRequiredInfo(state);
            if (!missing.isEmpty()) {
                return "Before building, I need the following information:\n" +
                       String.join("\n", missing);
            }

            ObjectNode payload = createFinalPayload(state);
            restTemplate.postForEntity("http://main-backend:8080/api/apps/create", payload, String.class);

            userStates.remove(userId);
            return "Successfully built your application!";
        } catch (Exception e) {
            log.error("Error building application", e);
            return "Failed to build the application. Please try again.";
        }
    }

    private Intent analyzeIntent(String input) {
        if (input.contains("build") || input.contains("create it") || input.contains("make it")) {
            return Intent.BUILD;
        }
        if (Pattern.compile("create\\s+(app|application)", Pattern.CASE_INSENSITIVE).matcher(input).find()) {
            return Intent.CREATE_APP;
        }
        if (Pattern.compile("add\\s+module", Pattern.CASE_INSENSITIVE).matcher(input).find()) {
            return Intent.ADD_MODULE;
        }
        if (Pattern.compile("add\\s+(field|uda)", Pattern.CASE_INSENSITIVE).matcher(input).find()) {
            return Intent.ADD_UDA;
        }
        if (Pattern.compile("set\\s+privilege", Pattern.CASE_INSENSITIVE).matcher(input).find()) {
            return Intent.SET_PRIVILEGES;
        }
        return Intent.UNKNOWN;
    }

    private String extractAppName(String input) {
        Matcher matcher = Pattern.compile("create\\s+(?:an\\s+)?(?:app|application)\\s+(?:called\\s+)?([\\w\\s-]+)", Pattern.CASE_INSENSITIVE).matcher(input);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String extractModuleName(String input) {
        Matcher matcher = Pattern.compile("add\\s+(?:a\\s+)?module\\s+(?:called\\s+)?([\\w\\s]+)", Pattern.CASE_INSENSITIVE).matcher(input);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private Map<String, String> extractFieldInfo(String input) {
        Map<String, String> fieldInfo = new HashMap<>();
        Matcher matcher = Pattern.compile("add\\s+(?:a\\s+)?(\\w+)\\s+(?:field|uda)\\s+(?:called\\s+)?([\\w\\s]+)", Pattern.CASE_INSENSITIVE).matcher(input);
        if (matcher.find()) {
            fieldInfo.put("type", matcher.group(1).trim());
            fieldInfo.put("name", matcher.group(2).trim());
        }
        return fieldInfo;
    }

    private Map<String, String> extractPrivilegeInfo(String input) {
        Map<String, String> privileges = new HashMap<>();
        if (input.toLowerCase().contains("read")) privileges.put("read", "true");
        if (input.toLowerCase().contains("write")) privileges.put("write", "true");
        return privileges;
    }

    private boolean isValidAppName(String appName) {
        return appName != null && appName.matches("^[a-zA-Z0-9-]+$");
    }

    private boolean isValidModuleName(String moduleName) {
        return moduleName != null && moduleName.matches("^[a-zA-Z0-9]+$");
    }

    private boolean isValidFieldName(String fieldName) {
        return fieldName != null && fieldName.matches("^[a-zA-Z0-9]+$");
    }

    private String generateIdentifier(String appName) {
        return appName != null ? appName.substring(0, Math.min(3, appName.length())).toUpperCase() : "APP";
    }

    private List<String> validateRequiredInfo(ConversationState state) {
        List<String> missing = new ArrayList<>();
        if (state.getAppName() == null) missing.add("App name is required");
        if (!state.hasModules()) missing.add("At least one module is required");
        for (Map.Entry<String, List<ConversationState.Field>> entry : state.getModuleFields().entrySet()) {
            if (entry.getValue().isEmpty()) {
                missing.add("Module '" + entry.getKey() + "' needs at least one field");
            }
        }
        return missing;
    }

    private ObjectNode createFinalPayload(ConversationState state) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("appName", state.getAppName());
        payload.put("appIdentifier", state.getAppIdentifier());
        return payload;
    }

    private void updateStateFromAnalysis(ConversationState state, AnalysisResult analysis) {
        ObjectNode objectNode = analysis.toJson(objectMapper).deepCopy();
        Map<String, Object> analysisMap = objectMapper.convertValue(objectNode, Map.class);
        state.updateFromAnalysis(analysisMap);
    }

    public enum Intent {
        CREATE_APP,
        ADD_MODULE,
        ADD_UDA,
        SET_PRIVILEGES,
        BUILD,
        UNKNOWN
    }

}