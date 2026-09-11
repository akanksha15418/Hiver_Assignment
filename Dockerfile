# Stage 1: Build the application (Alpine is fine for compiling Java)
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
# MUST use a Debian/glibc-based image (NOT alpine) because ONNX Runtime
# native libraries require glibc (libstdc++, ld-linux-x86-64.so.2).
# Alpine uses musl libc which is fundamentally incompatible.
FROM eclipse-temurin:17-jre
WORKDIR /app

# Expose port 8080 (the default port for Spring Boot)
EXPOSE 8080

# Copy the built jar from the build stage
COPY --from=build /app/target/ai-0.0.1-SNAPSHOT.jar app.jar

# Run the jar file
# -Xms: initial heap, -Xmx: max heap
# Render free tier has 512MB RAM; we give Java 400MB to load the ONNX embedding model
ENTRYPOINT ["java", "-Xms128m", "-Xmx400m", "-jar", "app.jar"]
