FROM maven:3-eclipse-temurin-17 AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre AS run
WORKDIR /app
COPY --from=build /app/target/finalyearproject-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
