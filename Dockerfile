# ---- build stage: compile the Spring Boot jar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- run stage: small JRE image ----
FROM eclipse-temurin:21-jre
WORKDIR /app
# Respect the container's memory limit (Render free tier = 512 MB)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
COPY --from=build /app/target/prescreener-1.0.0.jar app.jar
# The app binds to $PORT (Render sets it); defaults to 8081 locally.
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
