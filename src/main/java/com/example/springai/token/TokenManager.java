package com.example.springai.token;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenManager {

    private final Map<String, UserTokenInfo> userTokens = new ConcurrentHashMap<>();
    private static final int DEFAULT_TOKEN_LIMIT = 1000000; // 1M tokens
    private static final int TOKENS_PER_WORD = 4; // Approximate tokens per word

    public boolean hasEnoughTokens(String userId, String text) {
        UserTokenInfo tokenInfo = userTokens.computeIfAbsent(userId,
                k -> new UserTokenInfo(DEFAULT_TOKEN_LIMIT));

        int estimatedTokens = estimateTokenCount(text);
        return tokenInfo.getRemainingTokens() >= estimatedTokens;
    }

    public void consumeTokens(String userId, String text) {
        UserTokenInfo tokenInfo = userTokens.computeIfAbsent(userId,
                k -> new UserTokenInfo(DEFAULT_TOKEN_LIMIT));

        int estimatedTokens = estimateTokenCount(text);
        if (!tokenInfo.consumeTokens(estimatedTokens)) {
            throw new InsufficientTokensException("Not enough tokens available");
        }
    }

    public void addTokens(String userId, int tokens) {
        UserTokenInfo tokenInfo = userTokens.computeIfAbsent(userId,
                k -> new UserTokenInfo(DEFAULT_TOKEN_LIMIT));
        tokenInfo.addTokens(tokens);
    }

    public int getRemainingTokens(String userId) {
        UserTokenInfo tokenInfo = userTokens.get(userId);
        return tokenInfo != null ? tokenInfo.getRemainingTokens() : 0;
    }

    private int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // Count words and multiply by average tokens per word
        return text.split("\\s+").length * TOKENS_PER_WORD;
    }

    @lombok.Data
    private static class UserTokenInfo {

        private int remainingTokens;
        private final int maxTokens;

        public UserTokenInfo(int maxTokens) {
            this.maxTokens = maxTokens;
            this.remainingTokens = maxTokens;
        }

        public synchronized boolean consumeTokens(int tokens) {
            if (remainingTokens >= tokens) {
                remainingTokens -= tokens;
                return true;
            }
            return false;
        }

        public synchronized void addTokens(int tokens) {
            remainingTokens = Math.min(remainingTokens + tokens, maxTokens);
        }
    }

    public static class InsufficientTokensException extends RuntimeException {

        public InsufficientTokensException(String message) {
            super(message);
        }
    }
}
