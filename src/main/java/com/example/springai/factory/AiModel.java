package com.example.springai.factory;

/**
 * Interface defining the contract for AI model implementations
 */
public interface AiModel {
    /**
     * Execute the AI model with the given input
     * @param input The input text to process
     * @return The processed result
     */
    String execute(String input);
}