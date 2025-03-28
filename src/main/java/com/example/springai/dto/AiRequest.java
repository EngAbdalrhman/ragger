package com.example.springai.dto;

import lombok.Data;

@Data
public class AiRequest {
    private String modelName;
    private String input;
}