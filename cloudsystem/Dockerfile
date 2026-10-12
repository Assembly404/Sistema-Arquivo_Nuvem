FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn ./.mvn
COPY mvnw .
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw package -DskipTests -B

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd -m appuser
COPY --from=build /app/target/cloudsystem-0.0.1-SNAPSHOT.jar app.jar
# Copia o JAR também como cloudsystem-node.jar para download do agente
COPY --from=build /app/target/cloudsystem-0.0.1-SNAPSHOT.jar cloudsystem-node.jar
RUN chown appuser:appuser app.jar cloudsystem-node.jar
USER appuser
EXPOSE 5000
ENTRYPOINT ["java", "-jar", "app.jar"]
