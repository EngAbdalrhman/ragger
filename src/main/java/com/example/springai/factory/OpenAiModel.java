package com.example.springai.factory;

import org.springframework.stereotype.Component;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiModel implements AiModel {
    private final ChatClient chatClient;

    @Override
    public String execute(String input) {
        log.info("Processing input with OpenAI model: {}", input);
        return chatClient.call(new Prompt(input)).getResult().getOutput().getContent();
    }
}