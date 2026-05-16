# =========================
# Stage 1 - Build
# =========================
FROM maven:3.9.9-eclipse-temurin-25 AS builder

WORKDIR /app

# Copy Maven wrapper and pom first (better cache usage)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Download dependencies first
RUN mvn dependency:go-offline -B

# Copy source
COPY src src

# Build application
RUN mvn clean package -DskipTests

# =========================
# Stage 2 - Runtime
# =========================
FROM eclipse-temurin:25-jre

WORKDIR /app

# Security: run as non-root user
RUN useradd -r -s /bin/false spring

COPY --from=builder /app/target/*.jar app.jar

ENV OPENWEATHER_API_KEY=""

USER spring

EXPOSE 7070

ENTRYPOINT ["java","-jar","app.jar"]