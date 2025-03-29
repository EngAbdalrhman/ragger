package com.example.springai.factory;

import com.example.springai.token.TokenManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalAiModel implements AiModel {

    private final TokenManager tokenManager;

    @Override
    public String generateContent(String input) {
        String userId = getCurrentUserId();

        // Check if user has enough tokens
        if (!tokenManager.hasEnoughTokens(userId, input)) {
            throw new TokenManager.InsufficientTokensException(
                    "Not enough tokens. Please purchase more tokens to continue using the service.");
        }

        try {
            // Process the input using local model logic
            String result = processLocalModel(input);

            // Consume tokens for both input and output
            tokenManager.consumeTokens(userId, input);
            tokenManager.consumeTokens(userId, result);

            return result;
        } catch (Exception e) {
            log.error("Error processing input with local model", e);
            throw new RuntimeException("Failed to process input with local model", e);
        }
    }

    private String processLocalModel(String input) {
        // Implement your local model logic here
        // This is a placeholder implementation
        return "Processed by local model: " + input;
    }

    private String getCurrentUserId() {
        // In a real application, get this from SecurityContext or request context
        return "default-user";
    }
}
