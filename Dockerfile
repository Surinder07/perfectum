# Use the official OpenJDK 11 runtime as a base image
FROM openjdk:11-jre-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the application JAR file to the working directory
COPY target/my-spring-boot-app.jar /app/my-spring-boot-app.jar

# Expose the port the application runs on
EXPOSE 8080

# Set the entry point to run the application
ENTRYPOINT ["java", "-jar", "/app/my-spring-boot-app.jar"]