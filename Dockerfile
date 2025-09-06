# ================================
# Build Stage
# ================================
FROM maven:3.9.11-eclipse-temurin-24 AS build
WORKDIR /app

# Copy only the POM first to cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Now copy the actual source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# ================================
# Runtime Stage
# ================================
FROM eclipse-temurin:24-jre
WORKDIR /app

# Copy only the built jar from build stage
COPY --from=build /app/target/chess-game-0.0.1-SNAPSHOT.jar app.jar

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]
