#!/bin/bash

# Function to check if a service is ready
wait_for_service() {
    local service=$1
    local url=$2
    local max_attempts=30
    local attempt=1

    echo "Waiting for $service to be ready..."
    while ! curl -s "$url" > /dev/null; do
        if [ $attempt -eq $max_attempts ]; then
            echo "Failed to connect to $service after $max_attempts attempts"
            exit 1
        fi
        echo "Attempt $attempt: $service is not ready yet..."
        sleep 2
        attempt=$((attempt + 1))
    done
    echo "$service is ready!"
}

# Check if .env file exists
if [ ! -f .env ]; then
    echo "Error: .env file not found!"
    echo "Please create a .env file with required environment variables:"
    echo "OPENAI_API_KEY=your_key_here"
    echo "ELASTICSEARCH_HOST=http://localhost:9200"
    echo "KAFKA_BOOTSTRAP_SERVERS=localhost:9092"
    exit 1
fi

# Load environment variables
export $(cat .env | xargs)

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Error: Docker is not running!"
    exit 1
fi

# Start dependencies with Docker Compose
echo "Starting dependencies with Docker Compose..."
docker-compose up -d

# Wait for services to be ready
wait_for_service "Elasticsearch" "http://localhost:9200"
wait_for_service "Kafka UI" "http://localhost:8081"

echo "All services are ready!"

# Build and run the application
echo "Building the application..."
mvn clean install

echo "Starting the application..."
mvn spring-boot:run 