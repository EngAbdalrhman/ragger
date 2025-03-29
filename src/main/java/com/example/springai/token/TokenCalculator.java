package com.example.springai.token;

public class TokenCalculator {

    public int calculateTokens(String text) {
        // Simple token calculation logic
        return text.split("\\s+").length * 4; // Approximate calculation
    }
}
