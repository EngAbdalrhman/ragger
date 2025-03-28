package com.example.springai.factory;

import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertex.VertexAiChatClient;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiModel implements AiModel {

    private final VertexAiChatClient vertexAiChatClient;

    @Override
    public String execute(String input) {
        log.info("Processing input with Gemini model: {}", input);
        return vertexAiChatClient.call(new Prompt(input)).getResult().getOutput().getContent();
    }
}
