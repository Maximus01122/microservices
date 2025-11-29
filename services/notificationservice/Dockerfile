FROM eclipse-temurin:21-jre
WORKDIR /opt/app

# Falls du mit Maven baust:
#   mvn -pl orderservice -am -DskipTests package
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# Profil "docker" kannst du optional setzen; hier nur per Env möglich
ENTRYPOINT ["java","-jar","/opt/app/app.jar"]
