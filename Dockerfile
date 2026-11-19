# Этап 1: сборка
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -q package -DskipTests

# Этап 2: лёгкий образ только с JRE
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S bot && adduser -S bot -G bot && mkdir -p /app/logs && chown bot:bot /app/logs
USER bot
COPY --from=build /app/target/travel-planner-bot-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
