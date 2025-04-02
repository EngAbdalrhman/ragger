package com.example.springai.controller;

import com.example.springai.service.OcrService;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    private final OcrService ocrService;

    public OcrController(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    @PostMapping("/extract-text")
    public ResponseEntity<String> extractText(@RequestParam("file") MultipartFile file) {
        try {
            String text = ocrService.extractText(file);
            return ResponseEntity.ok(text);
        } catch (IOException | TesseractException e) {
            return ResponseEntity.internalServerError().body("Error processing image: " + e.getMessage());
        }
    }
}
