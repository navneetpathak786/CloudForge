FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY api-service/pom.xml api-service/pom.xml
COPY scheduler-service/pom.xml scheduler-service/pom.xml
COPY worker-agent/pom.xml worker-agent/pom.xml

COPY common/src common/src
COPY worker-agent/src worker-agent/src

RUN mvn -q -pl common,worker-agent -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/worker-agent/target/worker-agent.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
