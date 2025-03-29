package com.example.springai.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalModelMessagingService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.request}")
    private String requestRoutingKey;

    private final Map<String, CompletableFuture<ModelResponse>> pendingRequests
            = new ConcurrentHashMap<>();

    public CompletableFuture<String> processModelRequest(String input, String userId) {
        String requestId = UUID.randomUUID().toString();

        ModelRequest request = new ModelRequest();
        request.setRequestId(requestId);
        request.setUserId(userId);
        request.setInput(input);

        CompletableFuture<ModelResponse> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        try {
            rabbitTemplate.convertAndSend(
                    exchange,
                    requestRoutingKey,
                    objectMapper.writeValueAsString(request)
            );

            return future.thenApply(ModelResponse::getOutput)
                    .orTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .whenComplete((response, error)
                            -> pendingRequests.remove(requestId));
        } catch (Exception e) {
            pendingRequests.remove(requestId);
            log.error("Error sending model request", e);
            throw new RuntimeException("Failed to process model request", e);
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.response}")
    public void handleModelResponse(String responseJson) {
        try {
            ModelResponse response = objectMapper.readValue(responseJson, ModelResponse.class);
            CompletableFuture<ModelResponse> future = pendingRequests.get(response.getRequestId());

            if (future != null) {
                future.complete(response);
            } else {
                log.warn("Received response for unknown request: {}", response.getRequestId());
            }
        } catch (Exception e) {
            log.error("Error handling model response", e);
        }
    }

    @lombok.Data
    public static class ModelRequest {

        private String requestId;
        private String userId;
        private String input;
        private Map<String, Object> parameters;
    }

    @lombok.Data
    public static class ModelResponse {

        private String requestId;
        private String output;
        private boolean success;
        private String errorMessage;
        private Map<String, Object> metadata;
    }

    public void cleanup() {
        // Complete any pending requests with an error
        pendingRequests.forEach((requestId, future) -> {
            ModelResponse errorResponse = new ModelResponse();
            errorResponse.setRequestId(requestId);
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage("Request timed out or service shutting down");
            future.complete(errorResponse);
        });
        pendingRequests.clear();
    }
}
