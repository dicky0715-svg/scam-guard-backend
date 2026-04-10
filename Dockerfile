FROM eclipse-temurin:17-jdk-alpine
COPY app.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "-Dserver.port=${PORT}", "-Dserver.address=0.0.0.0", "app.jar"]