# syntax=docker/dockerfile:1
FROM gradle:8.14-jdk21 AS build
WORKDIR /src
COPY . .
RUN gradle bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre AS runtime
RUN useradd --system --uid 10001 app
WORKDIR /app
COPY --from=build /src/build/libs/*.jar app.jar
USER app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
