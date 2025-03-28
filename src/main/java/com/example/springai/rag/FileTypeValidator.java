package com.example.springai.rag;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.util.Set;
import java.util.HashSet;

@Component
public class FileTypeValidator {

    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(Set.of(
            "pdf", "docx", "txt", "doc", "rtf", "md", "csv", "json", "xml"
    ));

    private static final Set<String> SUPPORTED_MIME_TYPES = new HashSet<>(Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "application/msword",
            "application/rtf",
            "text/markdown",
            "text/csv",
            "application/json",
            "application/xml"
    ));

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String extension = getFileExtension(fileName).toLowerCase();
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Unsupported file type: " + extension);
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !SUPPORTED_MIME_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException("Unsupported MIME type: " + mimeType);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 50MB");
        }
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            throw new IllegalArgumentException("File has no extension");
        }
        return fileName.substring(lastDotIndex + 1);
    }
}
