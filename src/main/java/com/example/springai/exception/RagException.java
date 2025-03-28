package com.example.springai.exception;

public class RagException extends RuntimeException {

    private final ErrorCode errorCode;

    public RagException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public RagException(String message, ErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        INVALID_FILE_TYPE,
        FILE_TOO_LARGE,
        PROCESSING_ERROR,
        VERSION_CONFLICT,
        UNAUTHORIZED_ACCESS,
        COLLECTION_LIMIT_EXCEEDED,
        VERSION_LIMIT_EXCEEDED,
        DOCUMENT_NOT_FOUND,
        EMBEDDING_GENERATION_ERROR,
        CACHE_ERROR,
        DATABASE_ERROR,
        INVALID_CONFIGURATION
    }
}
