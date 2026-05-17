FROM eclipse-temurin:17-jdk-alpine

# 安裝 Tesseract OCR
RUN apk add --no-cache tesseract-ocr

# 手動下載繁體中文語言包
RUN wget -P /usr/share/tessdata/ https://github.com/tesseract-ocr/tessdata/raw/main/chi_tra.traineddata
RUN wget -P /usr/share/tessdata/ https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata

# 設定工作目錄
WORKDIR /app

# 複製所有 source code
COPY . .

# 確保 mvnw 可執行
RUN chmod +x mvnw

# 強制重新打包（跳過測試）
RUN ./mvnw clean package -DskipTests

# 暴露端口
EXPOSE 8080

# 運行 JAR 檔案
CMD ["java", "-jar", "-Dserver.port=${PORT}", "-Dserver.address=0.0.0.0", "target/api-0.0.1-SNAPSHOT.jar"]