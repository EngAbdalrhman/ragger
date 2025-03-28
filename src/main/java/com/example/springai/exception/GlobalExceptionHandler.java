package com.example.springai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.dao.OptimisticLockingFailureException;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;

import com.example.springai.exception.RagException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RagException.class)
    public ResponseEntity<Map<String, Object>> handleRagException(RagException ex) {
        log.error("RAG error: {}", ex.getMessage(), ex);

        Map<String, Object> response = createErrorResponse(
                ex.getErrorCode().toString(),
                ex.getMessage(),
                determineHttpStatus(ex.getErrorCode())
        );

        return ResponseEntity
                .status(determineHttpStatus(ex.getErrorCode()))
                .body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.error("File size exceeded: {}", ex.getMessage());

        Map<String, Object> response = createErrorResponse(
                "FILE_TOO_LARGE",
                "The uploaded file exceeds the maximum allowed size",
                HttpStatus.BAD_REQUEST
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLockingFailure(OptimisticLockingFailureException ex) {
        log.error("Concurrent modification error: {}", ex.getMessage());

        Map<String, Object> response = createErrorResponse(
                "VERSION_CONFLICT",
                "The document was modified by another user. Please retry the operation",
                HttpStatus.CONFLICT
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Invalid argument: {}", ex.getMessage());

        Map<String, Object> response = createErrorResponse(
                "INVALID_ARGUMENT",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        Map<String, Object> response = createErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR
        );

        return ResponseEntity.internalServerError().body(response);
    }

    private Map<String, Object> createErrorResponse(String code, String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("code", code);
        response.put("message", message);
        return response;
    }

    private HttpStatus determineHttpStatus(RagException.ErrorCode errorCode) {
        return switch (errorCode) {
            case INVALID_FILE_TYPE, FILE_TOO_LARGE ->
                HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED_ACCESS ->
                HttpStatus.FORBIDDEN;
            case DOCUMENT_NOT_FOUND ->
                HttpStatus.NOT_FOUND;
            case VERSION_CONFLICT ->
                HttpStatus.CONFLICT;
            case COLLECTION_LIMIT_EXCEEDED, VERSION_LIMIT_EXCEEDED ->
                HttpStatus.UNPROCESSABLE_ENTITY;
            case PROCESSING_ERROR, EMBEDDING_GENERATION_ERROR, CACHE_ERROR, DATABASE_ERROR, INVALID_CONFIGURATION ->
                HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
