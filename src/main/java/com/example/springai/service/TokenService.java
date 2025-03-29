package com.example.springai.service;

import org.springframework.stereotype.Service;

@Service
public class TokenService {

    public int calculateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / 4.0);
    }

    public void addTokens(String userId, int amount) {
        // Logic to add tokens to a user
    }

    public int getRemainingTokens(String userId) {
        // Logic to get remaining tokens for a user
        return 100; // Dummy value
    }
}
