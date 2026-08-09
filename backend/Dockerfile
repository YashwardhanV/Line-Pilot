FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S linepilot && adduser -S linepilot -G linepilot
WORKDIR /app
COPY --from=build /workspace/target/linepilot-backend-0.1.0.jar app.jar
USER linepilot
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
