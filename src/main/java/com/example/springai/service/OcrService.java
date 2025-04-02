package com.example.springai.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

@Service
public class OcrService {

    private final Tesseract tesseract;

    public OcrService() {
        tesseract = new Tesseract();
        tesseract.setDatapath("tessdata"); // Path to tessdata directory
        tesseract.setLanguage("eng"); // Set language to English
    }

    public String extractText(MultipartFile file) throws IOException, TesseractException {
        BufferedImage image = ImageIO.read(file.getInputStream());
        return tesseract.doOCR(image);
    }
}
