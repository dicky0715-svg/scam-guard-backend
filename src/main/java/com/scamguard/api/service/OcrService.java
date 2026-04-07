package com.scamguard.api.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
public class OcrService {

    private final Tesseract tesseract;

    public OcrService() {
        this.tesseract = new Tesseract();

        // 設置語言數據包路徑（指向 tessdata 資料夾）
        String tessDataPath = new File("tessdata").getAbsolutePath();
        this.tesseract.setDatapath(tessDataPath);

        // 設置識別語言：繁體中文 + 英文
        this.tesseract.setLanguage("chi_tra+eng");

        // OCR 引擎模式：3 係默認模式（LSTM+Legacy）
        this.tesseract.setOcrEngineMode(3);

        // 頁面分割模式：6 係「假設一個均勻文字區塊」
        this.tesseract.setPageSegMode(6);

        System.out.println("=== OCR Service initialized with tessdata path: " + tessDataPath);
    }

    /**
     * 從圖片文件提取文字
     */
    public String extractTextFromImage(MultipartFile imageFile) {
        try {
            // 將 MultipartFile 轉換為 BufferedImage
            BufferedImage image = ImageIO.read(imageFile.getInputStream());

            // 調用 Tesseract 進行 OCR
            String result = tesseract.doOCR(image);

            // 去除多餘空格和換行
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