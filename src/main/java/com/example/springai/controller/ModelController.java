package com.example.springai.controller;

import com.example.springai.service.ModelService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/models")
@Tag(name = "Model API", description = "Endpoints for AI model processing")
public class ModelController {

    private final ModelService modelService;

    @Autowired
    public ModelController(ModelService modelService) {
        this.modelService = modelService;
    }

    @Operation(
            summary = "Process request",
            description = "Process input using specified AI model",
            responses = {
                @ApiResponse(responseCode = "200", description = "Request processed successfully"),
                @ApiResponse(responseCode = "403", description = "Invalid license or insufficient tokens")
            }
    )
    @PostMapping("/process")
    public String processRequest(
            @Parameter(description = "License key", required = true)
            @RequestHeader("X-License-Key") String licenseKey,
            @Parameter(description = "Model type (local or rest)", example = "local")
            @RequestParam(required = false, defaultValue = "local") String modelType,
            @Parameter(description = "Input text to process", required = true)
            @RequestBody String input) {

        return modelService.processRequest(licenseKey, modelType, input);
    }

    @Operation(
            summary = "Get remaining tokens",
            description = "Check remaining tokens for a license key"
    )
    @GetMapping("/tokens")
    public int getRemainingTokens(
            @Parameter(description = "License key", required = true)
            @RequestHeader("X-License-Key") String licenseKey) {

        return modelService.getRemainingTokens(licenseKey);
    }
}
