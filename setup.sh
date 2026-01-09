#!/bin/bash
# Setup script for the LLM Database Chat System

set -e

echo "╔════════════════════════════════════════════════════════════╗"
echo "║   LLM Database Chat System - Setup Script                 ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo

# Check prerequisites
echo "🔍 Checking prerequisites..."
echo

# Check Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    echo "   Visit: https://docs.docker.com/get-docker/"
    exit 1
fi
echo "✅ Docker found: $(docker --version)"

# Check Docker Compose
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    echo "   Visit: https://docs.docker.com/compose/install/"
    exit 1
fi
echo "✅ Docker Compose found: $(docker-compose --version)"

# Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 11 or higher."
    echo "   Visit: https://adoptium.net/"
    exit 1
fi
echo "✅ Java found: $(java -version 2>&1 | head -n 1)"

# Check Clojure CLI
if ! command -v clj &> /dev/null; then
    echo "❌ Clojure CLI is not installed. Please install Clojure CLI tools."
    echo "   Visit: https://clojure.org/guides/install_clojure"
    exit 1
fi
echo "✅ Clojure found: $(clj --version 2>&1)"

echo
echo "✅ All prerequisites satisfied!"
echo

# Start Docker services
echo "🐳 Starting Docker services..."
echo
docker-compose up -d

echo
echo "⏳ Waiting for services to be healthy..."
echo

# Wait for PostgreSQL
max_attempts=30
attempt=0
while [ $attempt -lt $max_attempts ]; do
    if docker-compose exec -T postgres pg_isready -U studyuser -d studydb &> /dev/null; then
        echo "✅ PostgreSQL is ready!"
        break
    fi
    attempt=$((attempt + 1))
    if [ $attempt -eq $max_attempts ]; then
        echo "❌ PostgreSQL failed to start within expected time"
        echo "   Check logs: docker-compose logs postgres"
        exit 1
    fi
    sleep 2
done

# Wait for Ollama
attempt=0
while [ $attempt -lt $max_attempts ]; do
    if curl -s http://localhost:11434/api/tags &> /dev/null; then
        echo "✅ Ollama is ready!"
        break
    fi
    attempt=$((attempt + 1))
    if [ $attempt -eq $max_attempts ]; then
        echo "❌ Ollama failed to start within expected time"
        echo "   Check logs: docker-compose logs ollama"
        exit 1
    fi
    sleep 2
done

echo
echo "🤖 Checking for Llama2 model..."
echo

# Check if model exists
if docker exec study-llm-ollama ollama list | grep -q llama2; then
    echo "✅ Llama2 model already downloaded"
else
    echo "📥 Downloading Llama2 model (this may take several minutes)..."
    echo "   Model size: ~4GB"
    echo
    docker exec -it study-llm-ollama ollama pull llama2
    echo
    echo "✅ Llama2 model downloaded successfully!"
fi

echo
echo "╔════════════════════════════════════════════════════════════╗"
echo "║   Setup Complete! 🎉                                       ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo
echo "You can now run the application with:"
echo "  clj -M:run"
echo
echo "For more information, see README.md"
echo
