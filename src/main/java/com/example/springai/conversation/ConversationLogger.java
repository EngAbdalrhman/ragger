package com.example.springai.conversation;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationLogger {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        createTablesIfNotExist();
    }

    private void createTablesIfNotExist() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS conversation_logs (
                id SERIAL PRIMARY KEY,
                user_id VARCHAR(255) NOT NULL,
                timestamp TIMESTAMP NOT NULL,
                input TEXT,
                response TEXT,
                intent VARCHAR(50),
                state JSON,
                success BOOLEAN,
                error_message TEXT
            )
        """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS document_analysis_logs (
                id SERIAL PRIMARY KEY,
                user_id VARCHAR(255) NOT NULL,
                timestamp TIMESTAMP NOT NULL,
                file_name VARCHAR(255),
                analysis_result JSON,
                missing_info JSON,
                success BOOLEAN,
                error_message TEXT
            )
        """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS validation_logs (
                id SERIAL PRIMARY KEY,
                conversation_id BIGINT REFERENCES conversation_logs(id),
                field_name VARCHAR(255),
                value TEXT,
                valid BOOLEAN,
                error_message TEXT
            )
        """);
    }

    public void logConversation(String userId, String input, String response,
            String intent, Map<String, Object> state, boolean success, String errorMessage) {
        try {
            String stateJson = objectMapper.writeValueAsString(state);

            jdbcTemplate.update("""
                INSERT INTO conversation_logs 
                (user_id, timestamp, input, response, intent, state, success, error_message)
                VALUES (?, ?, ?, ?, ?, ?::json, ?, ?)
                """,
                    userId,
                    LocalDateTime.now(),
                    input,
                    response,
                    intent,
                    stateJson,
                    success,
                    errorMessage
            );
        } catch (Exception e) {
            log.error("Error logging conversation", e);
        }
    }

    public void logDocumentAnalysis(String userId, String fileName,
            Map<String, Object> analysisResult, List<String> missingInfo,
            boolean success, String errorMessage) {
        try {
            String resultJson = objectMapper.writeValueAsString(analysisResult);
            String missingInfoJson = objectMapper.writeValueAsString(missingInfo);

            jdbcTemplate.update("""
                INSERT INTO document_analysis_logs 
                (user_id, timestamp, file_name, analysis_result, missing_info, success, error_message)
                VALUES (?, ?, ?, ?::json, ?::json, ?, ?)
                """,
                    userId,
                    LocalDateTime.now(),
                    fileName,
                    resultJson,
                    missingInfoJson,
                    success,
                    errorMessage
            );
        } catch (Exception e) {
            log.error("Error logging document analysis", e);
        }
    }

    public void logValidation(Long conversationId, String fieldName,
            String value, boolean valid, String errorMessage) {
        try {
            jdbcTemplate.update("""
                INSERT INTO validation_logs 
                (conversation_id, field_name, value, valid, error_message)
                VALUES (?, ?, ?, ?, ?)
                """,
                    conversationId,
                    fieldName,
                    value,
                    valid,
                    errorMessage
            );
        } catch (Exception e) {
            log.error("Error logging validation", e);
        }
    }

    public List<Map<String, Object>> getConversationHistory(String userId) {
        return jdbcTemplate.queryForList("""
            SELECT * FROM conversation_logs 
            WHERE user_id = ? 
            ORDER BY timestamp DESC
            """,
                userId
        );
    }

    public List<Map<String, Object>> getDocumentAnalysisHistory(String userId) {
        return jdbcTemplate.queryForList("""
            SELECT * FROM document_analysis_logs 
            WHERE user_id = ? 
            ORDER BY timestamp DESC
            """,
                userId
        );
    }

    public List<Map<String, Object>> getValidationHistory(Long conversationId) {
        return jdbcTemplate.queryForList("""
            SELECT * FROM validation_logs 
            WHERE conversation_id = ? 
            ORDER BY id ASC
            """,
                conversationId
        );
    }

    public Map<String, Object> getConversationStats(String userId) {
        return jdbcTemplate.queryForMap("""
            SELECT 
                COUNT(*) as total_conversations,
                SUM(CASE WHEN success THEN 1 ELSE 0 END) as successful_conversations,
                SUM(CASE WHEN NOT success THEN 1 ELSE 0 END) as failed_conversations,
                COUNT(DISTINCT intent) as unique_intents
            FROM conversation_logs 
            WHERE user_id = ?
            """,
                userId
        );
    }

    public void cleanupOldLogs(int daysToKeep) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysToKeep);

        jdbcTemplate.update("""
            DELETE FROM validation_logs 
            WHERE conversation_id IN (
                SELECT id FROM conversation_logs 
                WHERE timestamp < ?
            )
            """,
                cutoff
        );

        jdbcTemplate.update("""
            DELETE FROM conversation_logs 
            WHERE timestamp < ?
            """,
                cutoff
        );

        jdbcTemplate.update("""
            DELETE FROM document_analysis_logs 
            WHERE timestamp < ?
            """,
                cutoff
        );
    }
}
