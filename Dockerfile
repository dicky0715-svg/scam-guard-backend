# 使用 OpenJDK 17 作為基礎鏡像
FROM openjdk:17-jdk-slim

# 設定工作目錄
WORKDIR /app

# 複製 Maven  wrapper 同 pom.xml
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
CMD ["java", "-jar", "target/api-0.0.1-SNAPSHOT.jar"]