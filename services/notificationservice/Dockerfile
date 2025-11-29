FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY services/ services/
RUN mvn -pl services/notificationservice -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/services/notificationservice/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
