# ---------- Stage 1: Build JAR ----------
FROM maven:3.9.11-eclipse-temurin-24 AS build

WORKDIR /app

# Copy pom.xml first (better Docker caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:24-jre

WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Copy Stockfish binary into container
COPY src/main/resources/engine/stockfish /app/engine/stockfish
RUN chmod +x /app/engine/stockfish

# Expose port
EXPOSE 8080

# Run app
ENTRYPOINT ["java", "-jar", "app.jar"]
