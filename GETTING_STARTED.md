# Getting Started with the LLM Database Chat System

This guide will walk you through setting up and running the LLM-powered database chat system for the first time.

## Quick Start (5 minutes)

### 1. Prerequisites Check

Before starting, ensure you have:

```bash
# Check Docker
docker --version
# Expected: Docker version 20.x or higher

# Check Java
java -version
# Expected: Java 11 or higher

# Check Clojure
clj --version  
# Expected: Clojure CLI 1.10.x or higher
```

If any are missing, install them first:
- **Docker**: https://docs.docker.com/get-docker/
- **Java**: https://adoptium.net/
- **Clojure CLI**: https://clojure.org/guides/install_clojure

### 2. Clone and Setup

```bash
git clone <repository-url>
cd study-llm

# Automated setup (recommended)
./setup.sh

# OR manual setup:
docker compose up -d
docker exec -it study-llm-ollama ollama pull llama2
```

The setup script will:
- Start PostgreSQL and Ollama containers
- Wait for services to be healthy
- Download the Llama2 model (~4GB, takes a few minutes)

### 3. Run the Application

```bash
clj -M:run
```

You should see:

```
╔════════════════════════════════════════════════════════════╗
║   LLM-Powered Database Chat System - Study Project        ║
║   Using: Clojure + PostgreSQL + Ollama (Local LLM)        ║
╚════════════════════════════════════════════════════════════╝

🔍 Checking system dependencies...

  PostgreSQL database... ✅ Connected
  Ollama LLM service... ✅ Running
     Available models: 1

🤖 Checking for required model...
✅ Model llama2 is available

✅ All systems ready!
```

### 4. Ask Your First Question

```
You: How many customers do we have?

🤔 Thinking...

Step 1: Converting your question to SQL...
Generated SQL:
   SELECT COUNT(*) FROM customers

Step 2: Executing query against database...
✅ Query executed successfully!
Found 1 result(s)

Step 3: Analyzing results...

────────────────────────────────────────────────────────────
📊 Analysis:

There are 10 customers in the database.
────────────────────────────────────────────────────────────
```

## Understanding the Output

Every question goes through 3 steps:

1. **SQL Generation**: The LLM converts your natural language question to SQL
2. **Query Execution**: The SQL runs against PostgreSQL
3. **Result Analysis**: The LLM analyzes and explains the results

## Example Questions to Try

### Basic Queries

```
You: Show me all product categories

You: What's the total revenue?

You: How many orders are pending?
```

### Analytical Questions

```
You: Which country has the most customers?

You: What are the top 5 selling products?

You: Show me the average order value

You: Which customers haven't ordered anything?
```

### Complex Queries

```
You: Compare the number of completed vs pending orders

You: Show me monthly revenue trends

You: Which products are low in stock?

You: Who are my best customers this year?
```

## Commands

While in the chat interface:

- `help` - Show help information
- `schema` - View the database structure
- `exit` or `quit` - Exit the program

## Troubleshooting

### "Connection refused" to PostgreSQL

```bash
# Check if container is running
docker compose ps

# View logs
docker compose logs postgres

# Restart if needed
docker compose restart postgres
```

### "Connection refused" to Ollama

```bash
# Check if container is running
docker compose ps

# View logs  
docker compose logs ollama

# Restart if needed
docker compose restart ollama
```

### LLM generates incorrect SQL

This can happen! The LLM is not perfect. Try:
- Rephrasing your question more specifically
- Using simpler language
- Checking the database schema with `schema` command
- Looking at example questions in the README

### First request is very slow

- **Normal**: The first request loads the model into memory (can take 10-30 seconds)
- **Subsequent requests**: Much faster (2-10 seconds depending on query complexity)

### Model download fails

```bash
# Try pulling manually
docker exec -it study-llm-ollama ollama pull llama2

# Check available space (model needs ~4GB)
df -h
```

## Understanding the Technology

### Why Ollama?

Ollama runs AI models locally:
- ✅ **Privacy**: Your data never leaves your machine
- ✅ **No API costs**: Free to use, no rate limits
- ✅ **Offline**: Works without internet (after model download)
- ✅ **Control**: You choose which model to use

### Why PostgreSQL?

PostgreSQL is an industry-standard database:
- ✅ **ACID compliance**: Reliable and consistent
- ✅ **Rich features**: Advanced SQL capabilities  
- ✅ **Performance**: Excellent for analytical queries
- ✅ **Production-ready**: Used by millions of applications

### Why Clojure?

Clojure is a modern Lisp for the JVM:
- ✅ **Functional**: Immutable data, pure functions
- ✅ **Interactive**: REPL-driven development
- ✅ **Mature**: Access to Java ecosystem
- ✅ **Production-ready**: Used in financial services, e-commerce

## Next Steps

### 1. Explore the Code

```bash
# Database layer
view src/study_llm/db.clj

# LLM integration
view src/study_llm/llm.clj

# Chat interface
view src/study_llm/chat.clj
```

### 2. Try Different Models

```bash
# Pull a different model
docker exec -it study-llm-ollama ollama pull mistral

# Update src/study_llm/llm.clj
# Change: {:model "llama2"} to {:model "mistral"}
```

Available models:
- `llama2` - Good general purpose (default)
- `mistral` - Often faster, similar quality
- `codellama` - Better for technical queries
- See more: https://ollama.com/library

### 3. Customize the Database

Edit `init.sql` to add your own:
- Tables
- Sample data  
- Indexes

Then recreate the database:

```bash
docker compose down -v  # Removes existing data!
docker compose up -d
```

### 4. Add Features

Ideas for extending the system:
- Add caching for common questions
- Implement query validation
- Support for INSERT/UPDATE queries
- Export results to CSV
- Multi-turn conversations
- Web UI instead of terminal

## Learning Resources

### Clojure
- [Clojure for the Brave and True](https://www.braveclojure.com/)
- [ClojureDocs](https://clojuredocs.org/)
- [Official Clojure Guides](https://clojure.org/guides/getting_started)

### PostgreSQL
- [PostgreSQL Tutorial](https://www.postgresqltutorial.com/)
- [SQL Style Guide](https://www.sqlstyle.guide/)

### LLM & AI
- [Prompt Engineering Guide](https://www.promptingguide.ai/)
- [Ollama Documentation](https://github.com/ollama/ollama/blob/main/docs/README.md)

### Text-to-SQL
- [Spider Dataset](https://yale-lily.github.io/spider) - Text-to-SQL benchmark
- [Text-to-SQL Papers](https://arxiv.org/abs/2301.07069)

## Support

If you encounter issues:

1. Check the [Troubleshooting section](#troubleshooting)
2. View logs: `docker compose logs`
3. Check service health: `docker compose ps`
4. Review the [README.md](README.md) for detailed information

## Clean Up

When done experimenting:

```bash
# Stop services (data persists)
docker compose down

# Stop services and remove data
docker compose down -v

# Remove downloaded models
docker volume rm study-llm_ollama_data
```

Happy learning! 🚀
