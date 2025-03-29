package com.example.springai.conversation;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
public class ConversationState {

    private Long conversationId;
    private ConversationService.Intent currentIntent;
    private String appName;
    private String appIdentifier;
    private String currentModule;
    private Map<String, List<Field>> moduleFields = new HashMap<>();
    private Map<String, String> privileges = new HashMap<>();
    private LocalDateTime lastUpdated;
    private List<String> validationErrors = new ArrayList<>();
    private AnalysisContext analysisContext;
    private static final Logger log = LoggerFactory.getLogger(ConversationState.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public ConversationState() {
        this.lastUpdated = LocalDateTime.now();
    }

    public void addModule(String moduleName) {
        if (!moduleFields.containsKey(moduleName)) {
            moduleFields.put(moduleName, new ArrayList<>());
        }
        this.currentModule = moduleName;
        updateTimestamp();
    }

    public void addField(String moduleName, String fieldName, String fieldType) {
        moduleFields.computeIfAbsent(moduleName, k -> new ArrayList<>())
                .add(new Field(fieldName, fieldType));
        updateTimestamp();
    }

    public void setPrivileges(Map<String, String> privileges) {
        this.privileges.putAll(privileges);
        updateTimestamp();
    }

    public boolean hasRequiredAppInfo() {
        return appName != null && appIdentifier != null;
    }

    public boolean hasModules() {
        return !moduleFields.isEmpty();
    }

    public boolean hasFields(String moduleName) {
        return moduleFields.containsKey(moduleName)
                && !moduleFields.get(moduleName).isEmpty();
    }

    public List<String> validateState() {
        List<String> missing = new ArrayList<>();

        if (appName == null) {
            missing.add("App name is required");
        }
        if (moduleFields.isEmpty()) {
            missing.add("At least one module is required");
        }
        for (Map.Entry<String, List<Field>> entry : moduleFields.entrySet()) {
            if (entry.getValue().isEmpty()) {
                missing.add("Module '" + entry.getKey() + "' needs at least one field");
            }
        }

        return missing;
    }

    public void addValidationError(String error) {
        validationErrors.add(error);
    }

    public void clearValidationErrors() {
        validationErrors.clear();
    }

    public boolean hasValidationErrors() {
        return !validationErrors.isEmpty();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> state = new HashMap<>();
        state.put("conversationId", conversationId);
        state.put("currentIntent", currentIntent);
        state.put("appName", appName);
        state.put("appIdentifier", appIdentifier);
        state.put("currentModule", currentModule);
        state.put("moduleFields", moduleFields);
        state.put("privileges", privileges);
        state.put("lastUpdated", lastUpdated);
        state.put("validationErrors", validationErrors);
        if (analysisContext != null) {
            state.put("analysisContext", analysisContext.toMap());
        }
        return state;
    }

    private void updateTimestamp() {
        this.lastUpdated = LocalDateTime.now();
    }

    @Data
    public static class Field {

        private final String name;
        private final String type;
        private Map<String, Object> attributes = new HashMap<>();

        public void addAttribute(String key, Object value) {
            attributes.put(key, value);
        }
    }

    @Data
    public static class AnalysisContext {

        private String documentName;
        private LocalDateTime analysisTime;
        private List<String> missingInformation = new ArrayList<>();
        private Map<String, Object> extractedData = new HashMap<>();

        public Map<String, Object> toMap() {
            Map<String, Object> context = new HashMap<>();
            context.put("documentName", documentName);
            context.put("analysisTime", analysisTime);
            context.put("missingInformation", missingInformation);
            context.put("extractedData", extractedData);
            return context;
        }

        public void addMissingInfo(String info) {
            missingInformation.add(info);
        }

        public void addExtractedData(String key, Object value) {
            extractedData.put(key, value);
        }

        public boolean hasMissingInformation() {
            return !missingInformation.isEmpty();
        }
    }

    public void initializeAnalysisContext(String documentName) {
        this.analysisContext = new AnalysisContext();
        this.analysisContext.setDocumentName(documentName);
        this.analysisContext.setAnalysisTime(LocalDateTime.now());
    }

    public void updateFromAnalysis(Map<String, Object> analysisResult) {
        if (analysisResult.containsKey("appName")) {
            this.appName = (String) analysisResult.get("appName");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modules
                = (List<Map<String, Object>>) analysisResult.get("modules");
        if (modules != null) {
            for (Map<String, Object> module : modules) {
                String moduleName = (String) module.get("name");
                addModule(moduleName);

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> fields
                        = (List<Map<String, Object>>) module.get("fields");
                if (fields != null) {
                    for (Map<String, Object> field : fields) {
                        addField(moduleName,
                                (String) field.get("name"),
                                (String) field.get("type"));
                    }
                }
            }
        }

        updateTimestamp();
    }

    public boolean isExpired(int timeoutMinutes) {
        return lastUpdated.plusMinutes(timeoutMinutes)
                .isBefore(LocalDateTime.now());
    }

    public void clear() {
        appName = null;
        appIdentifier = null;
        currentModule = null;
        moduleFields.clear();
        privileges.clear();
        validationErrors.clear();
        analysisContext = null;
        updateTimestamp();
    }

    public void saveConversationState(String userId, Map<String, Object> state) {
        try {
            String stateJson = objectMapper.writeValueAsString(state); // Convert state to JSON
            jdbcTemplate.update(
                    "INSERT INTO conversation_states (user_id, state) VALUES (?, ?::json) "
                    + "ON CONFLICT (user_id) DO UPDATE SET state = ?::json",
                    userId, stateJson, stateJson
            );
        } catch (Exception e) {
            log.error("Error saving conversation state", e);
        }
    }

    public Map<String, Object> getConversationState(String userId) {
        try {
            String stateJson = jdbcTemplate.queryForObject(
                    "SELECT state FROM conversation_states WHERE user_id = ?",
                    String.class, userId
            );
            return objectMapper.readValue(stateJson, Map.class); // Convert JSON back to Map
        } catch (Exception e) {
            log.error("Error retrieving conversation state", e);
            return null; // Return null if no state exists or on error
        }
    }
}
