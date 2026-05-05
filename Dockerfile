# ============================================
# FAZA 1: BUILD
# ============================================
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests


# ============================================
# FAZA 2: RUN
# ============================================
FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

RUN mkdir -p ./uploads/profile-photos ./logs

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
