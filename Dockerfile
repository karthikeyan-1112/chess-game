FROM eclipse-temurin:24-jre

WORKDIR /app

# Copy the JAR
COPY target/*.jar app.jar

# Copy Stockfish binary into container
COPY src/main/resources/engine/stockfish /app/engine/stockfish
RUN chmod +x /app/engine/stockfish

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
