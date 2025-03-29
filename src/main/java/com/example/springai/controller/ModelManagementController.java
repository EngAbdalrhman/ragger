package com.example.springai.controller;

import com.example.springai.model.ModelConfigEntity;
import com.example.springai.service.LicenseService;
import com.example.springai.service.ModelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/models")
public class ModelManagementController {

    private final ModelService modelService;
    private final LicenseService licenseService;

    public ModelManagementController(ModelService modelService,
            LicenseService licenseService) {
        this.modelService = modelService;
        this.licenseService = licenseService;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> listModels() {
        try {
            Map<String, String> models = modelService.listModels();
            return ResponseEntity.ok(models);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to list models"));
        }
    }

    @PostMapping
    public ResponseEntity<String> addModel(
            @RequestHeader("X-License-Key") String licenseKey,
            @RequestBody ModelConfigEntity config) {

        if (!licenseService.validateLicense(licenseKey)) {
            return ResponseEntity.badRequest()
                    .body("{\"error\":\"Invalid license\"}");
        }

        if (!licenseService.canAddNewModel(licenseKey)) {
            return ResponseEntity.badRequest()
                    .body(String.format("{\"error\":\"License limit reached (%d/%d models)\"}",
                            licenseService.getActiveModelCount(),
                            licenseService.getMaxAllowedModels(licenseKey)));
        }

        // Assuming modelService.addModel() is a method to add a model
        try {
            modelService.addModel(config);
            return ResponseEntity.ok(String.format("{\"status\":\"Model %s configuration received\"}",
                    config.getModelName()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("{\"error\":\"Failed to add model\"}");
        }
    }

    @GetMapping("/license")
    public ResponseEntity<String> checkLicense(
            @RequestHeader("X-License-Key") String licenseKey) {

        try {
            boolean isActive = licenseService.validateLicense(licenseKey);
            int activeModels = licenseService.getActiveModelCount();
            int maxModels = licenseService.getMaxAllowedModels(licenseKey);

            return ResponseEntity.ok(String.format("{\"active\":%b, \"models\":%d, \"max\":%d}",
                    isActive, activeModels, maxModels));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("{\"error\":\"Failed to check license\"}");
        }
    }

    @GetMapping("/tokens")
    public ResponseEntity<String> calculateTokens(@RequestParam String text) {
        try {
            int tokenCount = modelService.calculateTokens(text);
            return ResponseEntity.ok(String.format("{\"token_count\":%d}", tokenCount));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("{\"error\":\"Failed to calculate tokens\"}");
        }
    }
}
