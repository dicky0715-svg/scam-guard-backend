# 使用 Eclipse Temurin JDK 17 作為基礎鏡像
FROM eclipse-temurin:17-jdk-alpine

# 安裝 Tesseract OCR 同必要工具
RUN apk add --no-cache tesseract-ocr wget

# 手動下載繁體中文語言包（從 GitHub）
RUN wget -P /usr/share/tessdata/ https://github.com/tesseract-ocr/tessdata/raw/main/chi_tra.traineddata
RUN wget -P /usr/share/tessdata/ https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata

# 設定工作目錄
WORKDIR /app

# 複製所有檔案
COPY . .

# 確保 mvnw 可執行
RUN chmod +x mvnw

# 顯示 Maven 詳細錯誤（-e 顯示錯誤堆疊，-X 顯示 debug 信息）
RUN ./mvnw clean package -DskipTests -e -X

# 暴露端口
EXPOSE 8080

# 用 java -jar 運行 JAR 檔案
CMD ["java", "-jar", "-Dserver.port=${PORT}", "-Dserver.address=0.0.0.0", "target/api-0.0.1-SNAPSHOT.jar"]