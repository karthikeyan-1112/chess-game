# Stage 1: Build the app using Maven
FROM maven:3.9.9-eclipse-temurin-24 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies first (for caching)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the full source and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the app with JRE
FROM eclipse-temurin:24-jre

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Copy Stockfish binary into the image
COPY src/main/resources/static/engine/stockfish /app/engine/stockfish
RUN chmod +x /app/engine/stockfish

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
