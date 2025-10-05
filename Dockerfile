# Docker configuration (if needed for testing)
FROM openjdk:21-jdk-slim

LABEL maintainer="Astra Runner Team"
LABEL version="1.0.0"
LABEL description="Astra Runner - Automated execution of versioned Java libraries"

# Install required tools
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Create application directory
WORKDIR /app

# Copy application files
COPY app/build/libs/astra-runner.jar /app/astra-runner.jar
COPY astra.sh /app/astra.sh

# Make scripts executable
RUN chmod +x /app/astra.sh

# Create directories
RUN mkdir -p /app/workspace /app/reports

# Expose port for REST API
EXPOSE 8080

# Set environment variables
ENV JAVA_OPTS=""
ENV ARTIFACTORY_URL=""
ENV ARTIFACTORY_USER=""
ENV ARTIFACTORY_PASSWORD=""

# Default command
CMD ["java", "-jar", "/app/astra-runner.jar", "--rest"]