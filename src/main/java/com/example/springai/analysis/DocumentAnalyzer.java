package com.example.springai.analysis;

import com.example.springai.rag.DocumentProcessor;
import com.example.springai.rag.TextChunk;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentAnalyzer {

    private final DocumentProcessor documentProcessor;
    private final ObjectMapper objectMapper;

    public AnalysisResult analyzeDocument(MultipartFile file) throws Exception {
        List<TextChunk> chunks = documentProcessor.processDocument(file);
        String content = String.join("\n", chunks.stream()
                .map(TextChunk::getContent)
                .toList());

        AnalysisResult result = new AnalysisResult();

        // Extract app information
        extractAppInfo(content, result);

        // Extract modules
        extractModules(content, result);

        // Extract fields/UDAs
        extractFields(content, result);

        // Extract relationships
        extractRelationships(content, result);

        // Extract business rules
        extractBusinessRules(content, result);

        return result;
    }

    private void extractAppInfo(String content, AnalysisResult result) {
        // Look for app name patterns
        Pattern appNamePattern = Pattern.compile(
                "(?i)(application|app|system|platform)\\s+(?:name|called|titled)?[:\\s]+([\\w\\s]+)",
                Pattern.MULTILINE);
        Matcher matcher = appNamePattern.matcher(content);
        if (matcher.find()) {
            result.setAppName(matcher.group(2).trim());
        }

        // Look for app description
        Pattern descPattern = Pattern.compile(
                "(?i)(?:description|overview|about)[:\\s]+([^\\n.]+)[.\\n]");
        matcher = descPattern.matcher(content);
        if (matcher.find()) {
            result.setDescription(matcher.group(1).trim());
        }
    }

    private void extractModules(String content, AnalysisResult result) {
        List<ModuleInfo> modules = new ArrayList<>();

        // Look for module/entity patterns
        Pattern modulePattern = Pattern.compile(
                "(?im)^(?:module|entity|table|component)\\s+([\\w\\s]+)\\s*:?\\s*([^\\n]+)?");
        Matcher matcher = modulePattern.matcher(content);

        while (matcher.find()) {
            ModuleInfo module = new ModuleInfo();
            module.setName(matcher.group(1).trim());
            if (matcher.groupCount() > 1 && matcher.group(2) != null) {
                module.setDescription(matcher.group(2).trim());
            }
            modules.add(module);
        }

        result.setModules(modules);
    }

    private void extractFields(String content, AnalysisResult result) {
        Map<String, List<FieldInfo>> moduleFields = new HashMap<>();

        // Current module context
        String currentModule = null;

        // Split content into lines
        String[] lines = content.split("\\n");
        for (String line : lines) {
            // Check for module definition
            Matcher moduleMatcher = Pattern.compile(
                    "(?i)^(?:module|entity|table)\\s+([\\w\\s]+)")
                    .matcher(line);
            if (moduleMatcher.find()) {
                currentModule = moduleMatcher.group(1).trim();
                moduleFields.put(currentModule, new ArrayList<>());
                continue;
            }

            // Look for field definitions if we're in a module context
            if (currentModule != null) {
                Matcher fieldMatcher = Pattern.compile(
                        "(?i)\\b([\\w\\s]+)\\s*:\\s*(text|number|date|boolean|string|integer)\\b")
                        .matcher(line);
                if (fieldMatcher.find()) {
                    FieldInfo field = new FieldInfo();
                    field.setName(fieldMatcher.group(1).trim());
                    field.setType(mapFieldType(fieldMatcher.group(2)));
                    moduleFields.get(currentModule).add(field);
                }
            }
        }

        result.setModuleFields(moduleFields);
    }

    private void extractRelationships(String content, AnalysisResult result) {
        List<RelationshipInfo> relationships = new ArrayList<>();

        // Look for relationship patterns
        Pattern relationPattern = Pattern.compile(
                "(?i)(one|many)\\s+to\\s+(one|many)\\s+(?:relationship\\s+)?between\\s+([\\w\\s]+)\\s+and\\s+([\\w\\s]+)");
        Matcher matcher = relationPattern.matcher(content);

        while (matcher.find()) {
            RelationshipInfo rel = new RelationshipInfo();
            rel.setFromModule(matcher.group(3).trim());
            rel.setToModule(matcher.group(4).trim());
            rel.setType(matcher.group(1) + "-to-" + matcher.group(2));
            relationships.add(rel);
        }

        result.setRelationships(relationships);
    }

    private void extractBusinessRules(String content, AnalysisResult result) {
        List<String> rules = new ArrayList<>();

        // Look for business rule patterns
        Pattern rulePattern = Pattern.compile(
                "(?i)(?:rule|validation|constraint|requirement)\\s*:?\\s*([^\\n.]+)[.\\n]");
        Matcher matcher = rulePattern.matcher(content);

        while (matcher.find()) {
            rules.add(matcher.group(1).trim());
        }

        result.setBusinessRules(rules);
    }

    private String mapFieldType(String sourceType) {
        return switch (sourceType.toLowerCase()) {
            case "string", "text" ->
                "text";
            case "number", "integer", "decimal" ->
                "numeric";
            case "date", "datetime" ->
                "date";
            case "boolean" ->
                "boolean";
            default ->
                "text";
        };
    }

    @lombok.Data
    public static class AnalysisResult {

        private String appName;
        private String description;
        private List<ModuleInfo> modules = new ArrayList<>();
        private Map<String, List<FieldInfo>> moduleFields = new HashMap<>();
        private List<RelationshipInfo> relationships = new ArrayList<>();
        private List<String> businessRules = new ArrayList<>();
        private List<String> missingInformation = new ArrayList<>();

        public void addMissingInfo(String info) {
            missingInformation.add(info);
        }

        public ObjectNode toJson(ObjectMapper mapper) {
            ObjectNode json = mapper.createObjectNode();
            json.put("appName", appName);
            json.put("description", description);

            ArrayNode modulesNode = json.putArray("modules");
            for (ModuleInfo module : modules) {
                ObjectNode moduleNode = modulesNode.addObject();
                moduleNode.put("name", module.getName());
                moduleNode.put("description", module.getDescription());

                ArrayNode fieldsNode = moduleNode.putArray("fields");
                List<FieldInfo> fields = moduleFields.get(module.getName());
                if (fields != null) {
                    for (FieldInfo field : fields) {
                        ObjectNode fieldNode = fieldsNode.addObject();
                        fieldNode.put("name", field.getName());
                        fieldNode.put("type", field.getType());
                    }
                }
            }

            ArrayNode relationshipsNode = json.putArray("relationships");
            for (RelationshipInfo rel : relationships) {
                ObjectNode relNode = relationshipsNode.addObject();
                relNode.put("fromModule", rel.getFromModule());
                relNode.put("toModule", rel.getToModule());
                relNode.put("type", rel.getType());
            }

            ArrayNode rulesNode = json.putArray("businessRules");
            businessRules.forEach(rulesNode::add);

            ArrayNode missingNode = json.putArray("missingInformation");
            missingInformation.forEach(missingNode::add);

            return json;
        }
    }

    @lombok.Data
    public static class ModuleInfo {

        private String name;
        private String description;
    }

    @lombok.Data
    public static class FieldInfo {

        private String name;
        private String type;
    }

    @lombok.Data
    public static class RelationshipInfo {

        private String fromModule;
        private String toModule;
        private String type;
    }
}
