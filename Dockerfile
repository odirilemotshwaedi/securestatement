FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre
RUN addgroup --system --gid 10001 spring \
    && adduser --system --uid 10001 --ingroup spring spring \
    && mkdir -p /app/statements \
    && chown spring:spring /app/statements
WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar
COPY --chown=spring:spring data ./data

USER spring:spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
