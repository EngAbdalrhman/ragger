package com.example.springai.conversation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final Map<String, ConversationState> userStates = new HashMap<>();

    public String processUserInput(String userId, String input) {
        try {
            ConversationState state = userStates.computeIfAbsent(userId,
                    k -> new ConversationState());

            // Analyze intent
            Intent intent = analyzeIntent(input);
            state.setCurrentIntent(intent);

            switch (intent) {
                case CREATE_APP:
                    return handleCreateApp(state, input);
                case ADD_MODULE:
                    return handleAddModule(state, input);
                case ADD_UDA:
                    return handleAddUda(state, input);
                case SET_PRIVILEGES:
                    return handleSetPrivileges(state, input);
                case UNKNOWN:
                    return "I'm not sure what you want to do. Would you like to create an app, "
                            + "add a module, add a UDA, or set privileges?";
            }

            return "How can I help you?";
        } catch (Exception e) {
            log.error("Error processing user input", e);
            return "Sorry, I encountered an error. Please try again.";
        }
    }

    private Intent analyzeIntent(String input) {
        input = input.toLowerCase();

        if (input.contains("create") && (input.contains("app") || input.contains("application"))) {
            return Intent.CREATE_APP;
        }
        if (input.contains("add") && input.contains("module")) {
            return Intent.ADD_MODULE;
        }
        if (input.contains("add") && input.contains("uda")) {
            return Intent.ADD_UDA;
        }
        if (input.contains("set") && input.contains("privilege")) {
            return Intent.SET_PRIVILEGES;
        }

        return Intent.UNKNOWN;
    }

    private String handleCreateApp(ConversationState state, String input) {
        // Extract app name if provided
        String appName = extractAppName(input);
        if (appName != null) {
            state.setAppName(appName);
            state.setAppIdentifier(generateIdentifier(appName));

            // If we have all required info, create the app
            if (state.hasRequiredAppInfo()) {
                ObjectNode payload = createAppPayload(state);
                // Send to main backend
                sendToMainBackend("/api/apps", payload);

                // Clear state
                state.clear();
                return "I've created the app '" + appName + "' for you. "
                        + "Would you like to add a module to it?";
            }
        }

        // Ask for missing information
        if (state.getAppName() == null) {
            return "What would you like to name your app?";
        }

        return "I couldn't understand the app details. Please try again.";
    }

    private String handleAddModule(ConversationState state, String input) {
        // Extract module name if provided
        String moduleName = extractModuleName(input);
        if (moduleName != null) {
            state.setModuleName(moduleName);

            // If we have all required info, create the module
            if (state.hasRequiredModuleInfo()) {
                ObjectNode payload = createModulePayload(state);
                // Send to main backend
                sendToMainBackend("/api/modules", payload);

                // Clear state
                state.clear();
                return "I've added the module '" + moduleName + "'. "
                        + "Would you like to add UDAs to it?";
            }
        }

        // Ask for missing information
        if (state.getAppName() == null) {
            return "Which app would you like to add the module to?";
        }
        if (state.getModuleName() == null) {
            return "What would you like to name your module?";
        }

        return "I couldn't understand the module details. Please try again.";
    }

    private String handleAddUda(ConversationState state, String input) {
        // Extract UDA details if provided
        Map<String, String> udaDetails = extractUdaDetails(input);
        if (!udaDetails.isEmpty()) {
            state.setUdaName(udaDetails.get("name"));
            state.setUdaType(udaDetails.get("type"));

            // If we have all required info, create the UDA
            if (state.hasRequiredUdaInfo()) {
                ObjectNode payload = createUdaPayload(state);
                // Send to main backend
                sendToMainBackend("/api/udas", payload);

                // Clear state
                state.clear();
                return "I've added the UDA '" + udaDetails.get("name") + "'. "
                        + "Would you like to add another UDA?";
            }
        }

        // Ask for missing information
        if (state.getModuleName() == null) {
            return "Which module would you like to add the UDA to?";
        }
        if (state.getUdaName() == null) {
            return "What would you like to name your UDA?";
        }
        if (state.getUdaType() == null) {
            return "What type of UDA would you like to create? (text, numeric, date, etc.)";
        }

        return "I couldn't understand the UDA details. Please try again.";
    }

    private String handleSetPrivileges(ConversationState state, String input) {
        // Extract privilege details if provided
        Map<String, String> privilegeDetails = extractPrivilegeDetails(input);
        if (!privilegeDetails.isEmpty()) {
            state.setPrivilegeType(privilegeDetails.get("type"));
            state.setPrivilegeValue(privilegeDetails.get("value"));

            // If we have all required info, set the privileges
            if (state.hasRequiredPrivilegeInfo()) {
                ObjectNode payload = createPrivilegePayload(state);
                // Send to main backend
                sendToMainBackend("/api/privileges", payload);

                // Clear state
                state.clear();
                return "I've set the privileges as requested. "
                        + "Is there anything else you'd like to do?";
            }
        }

        // Ask for missing information
        if (state.getAppName() == null) {
            return "Which app would you like to set privileges for?";
        }
        if (state.getPrivilegeType() == null) {
            return "What type of privilege would you like to set? (read, write, admin)";
        }

        return "I couldn't understand the privilege details. Please try again.";
    }

    private String extractAppName(String input) {
        Pattern pattern = Pattern.compile("(?:create|make|new)\\s+(?:an?\\s+)?(?:app|application)\\s+(?:called|named)?\\s*['\"]?([\\w\\s-]+)['\"]?",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String extractModuleName(String input) {
        Pattern pattern = Pattern.compile("(?:add|create)\\s+(?:a\\s+)?module\\s+(?:called|named)?\\s*['\"]?([\\w\\s-]+)['\"]?",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private Map<String, String> extractUdaDetails(String input) {
        Map<String, String> details = new HashMap<>();

        // Extract UDA name
        Pattern namePattern = Pattern.compile("(?:add|create)\\s+(?:a\\s+)?uda\\s+(?:called|named)?\\s*['\"]?([\\w\\s-]+)['\"]?",
                Pattern.CASE_INSENSITIVE);
        Matcher nameMatcher = namePattern.matcher(input);
        if (nameMatcher.find()) {
            details.put("name", nameMatcher.group(1).trim());
        }

        // Extract UDA type
        Pattern typePattern = Pattern.compile("(?:of\\s+)?type\\s+([\\w]+)",
                Pattern.CASE_INSENSITIVE);
        Matcher typeMatcher = typePattern.matcher(input);
        if (typeMatcher.find()) {
            details.put("type", typeMatcher.group(1).trim().toLowerCase());
        }

        return details;
    }

    private Map<String, String> extractPrivilegeDetails(String input) {
        Map<String, String> details = new HashMap<>();

        // Extract privilege type
        Pattern typePattern = Pattern.compile("(?:set|give)\\s+([\\w]+)\\s+privileges?",
                Pattern.CASE_INSENSITIVE);
        Matcher typeMatcher = typePattern.matcher(input);
        if (typeMatcher.find()) {
            details.put("type", typeMatcher.group(1).trim().toLowerCase());
        }

        // Extract privilege value/level
        Pattern valuePattern = Pattern.compile("(?:to|level)\\s+([\\w]+)",
                Pattern.CASE_INSENSITIVE);
        Matcher valueMatcher = valuePattern.matcher(input);
        if (valueMatcher.find()) {
            details.put("value", valueMatcher.group(1).trim().toLowerCase());
        }

        return details;
    }

    private String generateIdentifier(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private void sendToMainBackend(String endpoint, ObjectNode payload) {
        try {
            restTemplate.postForEntity(endpoint, payload, String.class);
        } catch (Exception e) {
            log.error("Error sending request to main backend", e);
            throw e;
        }
    }

    private ObjectNode createAppPayload(ConversationState state) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("appName", state.getAppName());
        payload.put("appIdentifier", state.getAppIdentifier());
        payload.put("description", "Generated by AI agent");
        payload.put("icon", "fa fa-cube");
        payload.put("borderColor", true);
        payload.put("backgroundColor", true);
        return payload;
    }

    private ObjectNode createModulePayload(ConversationState state) {
        ObjectNode payload = objectMapper.createObjectNode();

        ObjectNode objects = payload.putObject("objects");
        objects.put("objectName", state.getModuleName());
        objects.put("udaTableName", state.getAppIdentifier() + "U" + state.getModuleName());
        objects.put("moduleType", 1);

        ObjectNode parentMenu = payload.putObject("parentMenu");
        parentMenu.put("recId", 1);
        parentMenu.put("order", 0);
        parentMenu.put("path", "pages");

        return payload;
    }

    private ObjectNode createUdaPayload(ConversationState state) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("appName", state.getAppName());
        payload.put("appIdentifier", state.getAppIdentifier());

        ArrayNode panels = payload.putArray("panels");
        ObjectNode panel = panels.addObject();
        panel.put("panelName", "Details");
        panel.put("panelType", "Accordion");

        ArrayNode udas = panel.putArray("udas");
        ObjectNode uda = udas.addObject();
        uda.put("udaType", state.getUdaType());
        uda.put("udaName", state.getUdaName());

        return payload;
    }

    private ObjectNode createPrivilegePayload(ConversationState state) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("appName", state.getAppName());
        payload.put("privilegeType", state.getPrivilegeType());
        payload.put("privilegeValue", state.getPrivilegeValue());
        return payload;
    }

    public enum Intent {
        CREATE_APP,
        ADD_MODULE,
        ADD_UDA,
        SET_PRIVILEGES,
        UNKNOWN
    }

    @lombok.Data
    private static class ConversationState {

        private Intent currentIntent;
        private String appName;
        private String appIdentifier;
        private String moduleName;
        private String udaName;
        private String udaType;
        private String privilegeType;
        private String privilegeValue;

        public void clear() {
            currentIntent = null;
            appName = null;
            appIdentifier = null;
            moduleName = null;
            udaName = null;
            udaType = null;
            privilegeType = null;
            privilegeValue = null;
        }

        public boolean hasRequiredAppInfo() {
            return appName != null && appIdentifier != null;
        }

        public boolean hasRequiredModuleInfo() {
            return appName != null && moduleName != null;
        }

        public boolean hasRequiredUdaInfo() {
            return moduleName != null && udaName != null && udaType != null;
        }

        public boolean hasRequiredPrivilegeInfo() {
            return appName != null && privilegeType != null && privilegeValue != null;
        }
    }
}
