# -------- Build stage --------
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build

WORKDIR /app
COPY pom.xml . 
COPY src ./src

RUN mvn clean package -DskipTests

# -------- Runtime stage --------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Install necessary packages
RUN apk add --no-cache bash dos2unix curl

# Copy and prepare wait-for-it.sh
COPY wait-for-it.sh /wait-for-it.sh
RUN dos2unix /wait-for-it.sh && \
    chmod +x /wait-for-it.sh && \
    mkdir -p /tmp/spring-app

# Add health check (using curl instead of wget for Alpine)
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8090/actuator/health || exit 1

# Run app with wait-for-it
ENTRYPOINT ["bash", "/wait-for-it.sh", "mysql:3306", "--timeout=60", "--", "java", "-jar", "app.jar"]
