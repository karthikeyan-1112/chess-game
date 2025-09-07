FROM eclipse-temurin:24-jre

WORKDIR /app

# Copy JAR
COPY target/*.jar app.jar

# Copy Stockfish binary into container
COPY src/main/resources/static/engine/stockfish /app/engine/stockfish
RUN chmod +x /app/engine/stockfish

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
