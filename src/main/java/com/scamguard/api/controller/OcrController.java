package com.scamguard.api.controller;

import com.scamguard.api.service.OcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ocr")
@CrossOrigin(origins = "*")  // ← 改成咗，允許所有來源
public class OcrController {

    @Autowired
    private OcrService ocrService;

    @PostMapping("/extract")
    public ResponseEntity<Map<String, Object>> extractText(@RequestParam("image") MultipartFile imageFile) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 檢查檔案是否為空
            if (imageFile.isEmpty()) {
                response.put("success", false);
                response.put("message", "請選擇圖片檔案");
                return ResponseEntity.badRequest().body(response);
            }

            // 檢查檔案類型
            String contentType = imageFile.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                response.put("success", false);
                response.put("message", "只支援圖片檔案");
                return ResponseEntity.badRequest().body(response);
            }

            // 調用 OCR 服務提取文字
            String extractedText = ocrService.extractTextFromImage(imageFile);

            response.put("success", true);
            response.put("extractedText", extractedText);
            response.put("message", "文字識別成功");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "OCR 處理失敗: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}