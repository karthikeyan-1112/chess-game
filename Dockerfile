FROM openjdk:24-jdk
COPY target/chess-game-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
