package com.example.springai.controller;

import com.example.springai.token.TokenManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
public class TokenController {

    private final TokenManager tokenManager;

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getTokenBalance(
            @RequestHeader("X-User-Id") String userId) {
        int remainingTokens = tokenManager.getRemainingTokens(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("remainingTokens", remainingTokens);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/purchase")
    public ResponseEntity<Map<String, Object>> purchaseTokens(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam int amount) {
        if (amount <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Token amount must be greater than 0"));
        }

        try {
            // Here you would typically integrate with a payment system
            // For now, we'll just add the tokens directly
            tokenManager.addTokens(userId, amount);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("purchasedTokens", amount);
            response.put("newBalance", tokenManager.getRemainingTokens(userId));
            response.put("status", "success");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error purchasing tokens for user: {}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process token purchase"));
        }
    }

    @GetMapping("/estimate")
    public ResponseEntity<Map<String, Object>> estimateTokens(
            @RequestParam String text) {
        try {
            // Estimate tokens needed for the text
            int estimatedTokens = text.split("\\s+").length * 4; // Approximate calculation

            Map<String, Object> response = new HashMap<>();
            response.put("text", text);
            response.put("estimatedTokens", estimatedTokens);
            response.put("estimatedCost", estimatedTokens * 0.0001); // Example cost calculation

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error estimating tokens", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to estimate tokens"));
        }
    }

    @PostMapping("/transfer")
    public ResponseEntity<Map<String, Object>> transferTokens(
            @RequestHeader("X-User-Id") String fromUserId,
            @RequestParam String toUserId,
            @RequestParam int amount) {
        if (amount <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Transfer amount must be greater than 0"));
        }

        try {
            int fromBalance = tokenManager.getRemainingTokens(fromUserId);
            if (fromBalance < amount) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Insufficient tokens for transfer"));
            }

            // Perform the transfer
            tokenManager.consumeTokens(fromUserId, "Transfer: " + amount);
            tokenManager.addTokens(toUserId, amount);

            Map<String, Object> response = new HashMap<>();
            response.put("fromUser", fromUserId);
            response.put("toUser", toUserId);
            response.put("transferredAmount", amount);
            response.put("newFromBalance", tokenManager.getRemainingTokens(fromUserId));
            response.put("newToBalance", tokenManager.getRemainingTokens(toUserId));
            response.put("status", "success");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error transferring tokens from {} to {}", fromUserId, toUserId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process token transfer"));
        }
    }
}
