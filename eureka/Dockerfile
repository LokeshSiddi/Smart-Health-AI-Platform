# Stage 1: Cached Dependencies & Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# 1. Copy Maven Wrapper & Config
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw

# 2. Copy Parent POM and ALL module POMs to leverage Docker layer caching
COPY pom.xml .
COPY eureka/pom.xml eureka/
COPY configserver/pom.xml configserver/
COPY gateway/pom.xml gateway/
COPY userservice/pom.xml userservice/
COPY activityservice/pom.xml activityservice/
COPY aiservice/pom.xml aiservice/

# 3. Download all dependencies offline (This layer stays cached unless ANY pom.xml changes)
RUN ./mvnw dependency:go-offline -B || true

# ARG to specify which service to build (passed from docker-compose)
ARG SERVICE_NAME

# 4. Copy source code for the specific service being built
COPY ${SERVICE_NAME}/src ${SERVICE_NAME}/src

# 5. Build ONLY the requested microservice (-pl: project list, -am: also make parent dependencies)
RUN ./mvnw clean package -pl ${SERVICE_NAME} -am -DskipTests

# Stage 2: Minimal Runtime Image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ARG SERVICE_NAME
ARG PORT=8080
EXPOSE ${PORT}

# Copy the built JAR from the builder stage
COPY --from=builder /app/${SERVICE_NAME}/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]