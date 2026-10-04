FROM eclipse-temurin:21-jre

RUN groupadd -r portal && useradd -r -g portal portal

WORKDIR /app
COPY target/*.jar app.jar
RUN chown portal:portal /app/app.jar
USER portal

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]