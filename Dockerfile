# 使用 Eclipse Temurin JDK 17 作為基礎鏡像
FROM eclipse-temurin:17-jdk-alpine

# 安裝 Tesseract OCR 同必要工具
RUN apk add --no-cache tesseract-ocr wget

# 手動下載繁體中文語言包（從 GitHub）
RUN wget -P /usr/share/tessdata/ https://github.com/tesseract-ocr/tessdata/raw/main/chi_tra.traineddata
RUN wget -P /usr/share/tessdata/ https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata

# 設定工作目錄
WORKDIR /app

# 複製 Maven wrapper 同 pom.xml
COPY .mvn ./.mvn
COPY mvnw .
COPY pom.xml .

# 下載依賴
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# 複製源代碼
COPY src ./src

# 打包應用程式
RUN ./mvnw clean package -DskipTests

# 暴露端口
EXPOSE 8080

# 運行 JAR 檔案
CMD ["java", "-jar", "-Dserver.port=${PORT}", "-Dserver.address=0.0.0.0", "target/api-0.0.1-SNAPSHOT.jar"]