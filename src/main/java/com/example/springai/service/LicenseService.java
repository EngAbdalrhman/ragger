package com.example.springai.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LicenseService {

    private final TokenService tokenService;
    private final ConcurrentHashMap<String, AtomicInteger> tokenStore = new ConcurrentHashMap<>();

    @Autowired
    public LicenseService(TokenService tokenService) {
        this.tokenService = tokenService;
        // Initialize with demo tokens
        tokenStore.put("DEMO-KEY", new AtomicInteger(1000));
    }

    public boolean validateLicense(String licenseKey) {
        // Validate the license key
        return true; // Dummy validation
    }

    public boolean canAddNewModel(String licenseKey) {
        // Check if a new model can be added under the license
        return true; // Dummy check
    }

    public int getActiveModelCount() {
        // Return the count of active models
        return 5; // Dummy count
    }

    public int getMaxAllowedModels(String licenseKey) {
        // Return the maximum allowed models for the license
        return 10; // Dummy max
    }

    public boolean canMakeRequest(String licenseKey, String text) {
        if (!validateLicense(licenseKey)) {
            return false;
        }
        int tokensRequired = tokenService.calculateTokens(text);
        return tokenStore.get(licenseKey).get() >= tokensRequired;
    }

    public int getRemainingTokens(String licenseKey) {
        return validateLicense(licenseKey)
                ? tokenStore.get(licenseKey).get() : 0;
    }

    public void deductTokens(String licenseKey, int tokens) {
        if (validateLicense(licenseKey)) {
            tokenStore.get(licenseKey).addAndGet(-tokens);
        }
    }

    public void addTokens(String licenseKey, int tokens) {
        tokenStore.computeIfAbsent(licenseKey, k -> new AtomicInteger(0))
                .addAndGet(tokens);
    }
}
