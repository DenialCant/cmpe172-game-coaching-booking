FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY src ./src
COPY docker/HealthProbe.java ./docker/HealthProbe.java
RUN mvn -B verify && javac -d /healthcheck docker/HealthProbe.java

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /build/target/game-coaching-booking-0.0.1-SNAPSHOT.jar /app/app.jar
COPY --from=build /healthcheck /app/healthcheck
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=8s --start-period=45s --retries=3     CMD ["java", "-cp", "/app/healthcheck", "HealthProbe"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
