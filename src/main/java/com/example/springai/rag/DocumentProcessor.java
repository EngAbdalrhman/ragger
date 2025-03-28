package com.example.springai.rag;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class DocumentProcessor {

    public List<TextChunk> processDocument(MultipartFile file) throws IOException {
        String content = extractText(file);
        return splitIntoChunks(content);
    }

    private String extractText(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename().toLowerCase();

        if (fileName.endsWith(".pdf")) {
            try (PDDocument document = PDDocument.load(file.getInputStream())) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        } else if (fileName.endsWith(".docx")) {
            try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
                XWPFWordExtractor extractor = new XWPFWordExtractor(document);
                return extractor.getText();
            }
        } else if (fileName.endsWith(".txt")) {
            return new String(file.getBytes());
        } else {
            throw new IllegalArgumentException("Unsupported file format");
        }
    }

    private List<TextChunk> splitIntoChunks(String content) {
        List<TextChunk> chunks = new ArrayList<>();
        String[] paragraphs = content.split("\\n\\n+");

        StringBuilder currentChunk = new StringBuilder();
        int chunkSize = 0;
        int maxChunkSize = 1000; // Maximum characters per chunk

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) {
                continue;
            }

            if (chunkSize + paragraph.length() > maxChunkSize && chunkSize > 0) {
                // Store current chunk and start a new one
                chunks.add(new TextChunk(currentChunk.toString()));
                currentChunk = new StringBuilder();
                chunkSize = 0;
            }

            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(paragraph);
            chunkSize += paragraph.length();
        }

        if (currentChunk.length() > 0) {
            chunks.add(new TextChunk(currentChunk.toString()));
        }

        return chunks;
    }
}
