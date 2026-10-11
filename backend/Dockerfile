FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY frontend/pom.xml frontend/pom.xml
COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
RUN mvn -B -pl backend -am package -DskipTests
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN mkdir logs && chown 10001:10001 logs
COPY --from=build /workspace/backend/target/backend-1.0.0.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
