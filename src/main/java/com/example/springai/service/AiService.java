package com.example.springai.service;

import com.example.springai.factory.ModelFactory;
import com.example.springai.factory.AiModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {
    private final ModelFactory modelFactory;

    public String executeModel(String modelName, String input) {
        log.info("Executing AI model: {} with input: {}", modelName, input);
        AiModel model = modelFactory.getAiModel(modelName);
        return model.execute(input);
    }
}