# Use an official OpenJDK runtime as a parent image
FROM openjdk:11-jre-slim

# Set the working directory in the container
WORKDIR /app

# Copy the application JAR file to the working directory
COPY target/waaw-0.0.1-SNAPSHOT.jar /app/waaw-0.0.1-SNAPSHOT.jar

# Expose the port the application runs on
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "/app/waaw-0.0.1-SNAPSHOT.jar"]