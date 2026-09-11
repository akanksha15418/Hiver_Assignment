# Stage 1: Build the application
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy the pom.xml and download dependencies
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .

# We download dependencies first to cache them in Docker layer
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B || true

# Copy the source code and build the application
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Expose port 8080 (the default port for Spring Boot)
EXPOSE 8080

# Copy the built jar from the build stage
COPY --from=build /app/target/ai-0.0.1-SNAPSHOT.jar app.jar

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
