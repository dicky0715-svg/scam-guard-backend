FROM eclipse-temurin:17-jdk-alpine

# 安裝 Tesseract OCR
RUN apk add --no-cache tesseract-ocr

# 手動下載繁體中文語言包
RUN wget -P /usr/share/tessdata/ https://github.com/tesseract-ocr/tessdata/raw/main/chi_tra.traineddata
RUN wget -P /usr/share/tessdata/ https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata

COPY app.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "-Dserver.port=${PORT}", "-Dserver.address=0.0.0.0", "app.jar"]