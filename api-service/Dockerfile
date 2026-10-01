FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY api-service/pom.xml api-service/pom.xml
COPY scheduler-service/pom.xml scheduler-service/pom.xml
COPY worker-agent/pom.xml worker-agent/pom.xml

COPY common/src common/src
COPY api-service/src api-service/src

RUN mvn -q -pl common,api-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/api-service/target/api-service.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
