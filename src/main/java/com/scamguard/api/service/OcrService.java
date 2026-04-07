package com.scamguard.api.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Service
public class OcrService {

    private final Tesseract tesseract;

    public OcrService() {
        this.tesseract = new Tesseract();

        // 使用 Alpine 安裝嘅 Tesseract 默認路徑
        this.tesseract.setDatapath("/usr/share/tessdata/");

        // 設置識別語言：繁體中文 + 英文（手動下載語言包後可用）
        this.tesseract.setLanguage("chi_tra+eng");

        // OCR 引擎模式
        this.tesseract.setOcrEngineMode(3);
        this.tesseract.setPageSegMode(6);

        System.out.println("=== OCR Service initialized with tessdata path: /usr/share/tessdata/");
    }

    public String extractTextFromImage(MultipartFile imageFile) {
        try {
            BufferedImage image = ImageIO.read(imageFile.getInputStream());
            String result = tesseract.doOCR(image);
            return result.trim();
        } catch (IOException e) {
            System.err.println("圖片讀取失敗: " + e.getMessage());
            e.printStackTrace();
            return "圖片讀取失敗";
        } catch (TesseractException e) {
            System.err.println("OCR 識別失敗: " + e.getMessage());
            e.printStackTrace();
            return "文字識別失敗，請確保圖片清晰";
        }
    }
}