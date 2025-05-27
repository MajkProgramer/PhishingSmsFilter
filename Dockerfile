FROM openjdk:11-jre-slim

WORKDIR /app

COPY target/scala-2.13/PhishingSmsFilter-assembly-0.1.0-SNAPSHOT.jar app.jar
COPY src/main/resources/application.conf application.conf

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]