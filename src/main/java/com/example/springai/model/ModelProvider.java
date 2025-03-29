package com.example.springai.model;

public interface ModelProvider {

    String getProviderName();

    String processRequest(String input);

    String generateContent(String prompt);

    int calculateTokens(String text);
}
