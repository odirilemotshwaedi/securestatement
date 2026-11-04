FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests -Dspring.flyway.enabled=false package

FROM eclipse-temurin:25-jre
RUN addgroup --system spring && adduser --system --ingroup spring spring
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
COPY --chown=spring:spring data ./data

USER spring:spring
EXPOSE 8080
# Clean, recommended format
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
