# LLM-Powered Database Chat System

A learning project demonstrating how to build a production-ready system that allows users to ask questions about a database in natural language. The system uses an LLM to convert questions to SQL, executes queries against PostgreSQL, and provides AI-powered analysis of the results.

## 🎯 Project Goals

Learn how to:
- Build a Clojure application with real-world dependencies
- Use PostgreSQL for data storage
- Run a local LLM (without relying on external APIs)
- Integrate LLM with structured data (text-to-SQL)
- Create a terminal-based chat interface
- Build a production-ready architecture

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     User (Terminal)                         │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              Clojure Application (study-llm)                │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │  Chat UI    │  │  LLM Client  │  │   DB Client      │   │
│  │  (chat.clj) │──│  (llm.clj)   │──│   (db.clj)       │   │
│  └─────────────┘  └──────────────┘  └──────────────────┘   │
└────────────┬────────────────┬──────────────────────────────┘
             │                │
             ▼                ▼
┌──────────────────┐  ┌──────────────────┐
│   Ollama         │  │   PostgreSQL     │
│   (LLM Runtime)  │  │   (Database)     │
│   Port: 11434    │  │   Port: 5432     │
└──────────────────┘  └──────────────────┘
```

## 🚀 Quick Start

### Prerequisites

- Docker and Docker Compose
- Java 11 or higher (for Clojure)
- Clojure CLI tools (`clj` command)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd study-llm
   ```

2. **Start the services (PostgreSQL and Ollama)**
   ```bash
   docker-compose up -d
   ```
   
   This will:
   - Start PostgreSQL on port 5432
   - Create the database and populate with sample data
   - Start Ollama (LLM runtime) on port 11434

3. **Wait for services to be healthy** (30-60 seconds)
   ```bash
   docker-compose ps
   ```
   
   Both services should show "healthy" status.

4. **Pull the LLM model** (first time only, ~4GB download)
   ```bash
   docker exec -it study-llm-ollama ollama pull llama2
   ```
   
   This downloads the Llama2 model. It takes a few minutes depending on your internet speed.

5. **Run the application**
   ```bash
   clj -M:run
   ```

### Usage

Once the application starts, you can ask questions in natural language:

```
You: What are the top 5 customers by total spent?

🤔 Thinking...

Step 1: Converting your question to SQL...
Generated SQL:
   SELECT name, total_spent FROM customers ORDER BY total_spent DESC LIMIT 5

Step 2: Executing query against database...
✅ Query executed successfully!
Found 5 result(s)

Step 3: Analyzing results...

────────────────────────────────────────────────────────────────────────────────
📊 Analysis:

The top 5 customers by total spent are:
1. Grace Lee - $3,210.00
2. Charlie Brown - $2,340.75
3. Eve Davis - $1,890.00
4. Iris Chen - $1,560.25
5. Alice Johnson - $1,250.00

Grace Lee is the highest spending customer with more than $3,000 in total purchases.
────────────────────────────────────────────────────────────────────────────────
```

Commands:
- `help` - Show help information
- `schema` - View database schema
- `exit` or `quit` - Exit the application

## 📚 Learning Guide

### Why These Technologies?

#### **Clojure**
- **Functional programming**: Immutable data structures, pure functions
- **JVM ecosystem**: Access to mature Java libraries
- **REPL-driven development**: Interactive programming experience
- **Concurrency**: Built-in support for concurrent programming
- **Production use**: Used by companies like Nubank, Walmart, Apple

**Key Clojure concepts in this project:**
- Namespaces (`ns`) for code organization
- Threading macros (`->`, `->>`) for data transformation
- Atoms for state management
- Higher-order functions (`map`, `mapv`, `doseq`)

#### **PostgreSQL**
- **ACID compliance**: Reliable transactions
- **Rich data types**: JSON, arrays, and more
- **Performance**: Excellent for analytical queries
- **Production-ready**: Battle-tested, used everywhere
- **Open source**: Free and community-supported

**Database concepts demonstrated:**
- Schema design (customers, products, orders, order_items)
- Foreign key relationships
- Indexes for query optimization
- Connection pooling (HikariCP)

#### **Ollama**
- **Local execution**: No external API dependencies
- **Privacy**: Data never leaves your machine
- **Multiple models**: Supports Llama2, Mistral, CodeLlama, etc.
- **Production-ready**: Can be deployed in containers
- **Simple API**: RESTful HTTP interface

**LLM integration patterns:**
- Prompt engineering for text-to-SQL
- Context management (passing schema information)
- Temperature control (0.1 for SQL, 0.3 for analysis)
- Error handling and retries

### Code Structure Explanation

#### `deps.edn` - Dependency Management
```clojure
{:paths ["src" "resources"]
 :deps {
   ;; Clojure itself
   org.clojure/clojure {:mvn/version "1.11.1"}
   
   ;; Database: JDBC driver + connection library + pool
   org.postgresql/postgresql {:mvn/version "42.7.1"}
   com.github.seancorfield/next.jdbc {:mvn/version "1.3.909"}
   com.zaxxer/HikariCP {:mvn/version "5.1.0"}
   
   ;; HTTP client for calling Ollama API
   clj-http/clj-http {:mvn/version "3.12.3"}
   
   ;; JSON parsing
   cheshire/cheshire {:mvn/version "5.12.0"}
 }
 :aliases {:run {:main-opts ["-m" "study-llm.core"]}}}
```

Why these dependencies?
- `next.jdbc`: Modern, simple JDBC wrapper (successor to clojure.java.jdbc)
- `HikariCP`: Fastest connection pool, production standard
- `clj-http`: De facto HTTP client for Clojure
- `cheshire`: Fast JSON library built on Jackson

#### `src/study_llm/db.clj` - Database Layer

Key patterns:
```clojure
;; Atom for managing state (connection pool)
(defonce datasource (atom nil))

;; Connection pooling (not creating new connections each time)
(defn start-db-pool! []
  (reset! datasource (connection/->pool HikariDataSource db-config)))

;; Prepared statements (SQL injection prevention)
(defn execute-query! [sql-vec]
  (jdbc/execute! ds sql-vec))
```

**Production patterns demonstrated:**
1. **Connection pooling**: Reusing database connections for performance
2. **Prepared statements**: Using parameterized queries to prevent SQL injection
3. **Error handling**: Try-catch blocks with logging
4. **Resource management**: Proper cleanup in `stop-db-pool!`

#### `src/study_llm/llm.clj` - LLM Integration

Key patterns:
```clojure
;; Prompt engineering for text-to-SQL
(defn create-sql-prompt [user-question schema-info]
  (str "You are a SQL expert. Given the following database schema..."
       "Return ONLY the SQL query, nothing else."))

;; Temperature control for different tasks
(generate-completion prompt :temperature 0.1)  ; Low for SQL (more precise)
(generate-completion prompt :temperature 0.3)  ; Higher for analysis (more creative)
```

**LLM best practices:**
1. **Clear instructions**: Tell the LLM exactly what to do
2. **Schema in context**: Provide database structure
3. **Temperature tuning**: Lower for factual, higher for creative
4. **Output formatting**: Specify exact output format needed

#### `src/study_llm/chat.clj` - User Interface

Terminal UI patterns:
```clojure
;; Read-eval-print loop
(loop []
  (print "You: ")
  (flush)
  (when-let [input (read-line)]
    (process-question input)
    (recur)))
```

#### `src/study_llm/core.clj` - Application Entry Point

Application lifecycle:
```clojure
(defn -main [& args]
  ;; 1. Check dependencies
  (check-dependencies)
  ;; 2. Ensure model is available
  (ensure-model-available)
  ;; 3. Start chat
  (chat/start-chat)
  ;; 4. Cleanup (in finally block)
  (db/stop-db-pool!))
```

### Database Schema

The sample database represents an e-commerce system:

**Customers Table**
- Customer information (name, email, country)
- Total spending tracking

**Products Table**
- Product catalog with categories
- Inventory tracking (stock_quantity)

**Orders Table**
- Order records with status
- Links to customers

**Order Items Table**
- Individual items in each order
- Links orders to products

This schema supports various analytical questions:
- Who are the top customers?
- What products sell best?
- How many orders are pending?
- Which country generates most revenue?

## 🔧 How It Works

### The Complete Flow

1. **User asks a question** in the terminal
   ```
   "What are the top 5 customers by total spent?"
   ```

2. **System loads database schema** 
   - Queries information_schema to get table and column definitions
   - Builds a schema description for the LLM

3. **LLM generates SQL** (Text-to-SQL)
   - Prompt includes: instructions + schema + user question
   - LLM returns SQL query
   ```sql
   SELECT name, total_spent FROM customers ORDER BY total_spent DESC LIMIT 5
   ```

4. **Execute SQL against PostgreSQL**
   - Using connection pool for performance
   - Prepared statement for security
   - Returns structured data

5. **LLM analyzes results**
   - Prompt includes: user question + query results
   - LLM provides human-readable summary with insights

6. **Display to user**
   - Shows SQL generated (educational)
   - Shows analysis and insights

### Text-to-SQL: How Does It Work?

The LLM doesn't "know" your database. We make it work by:

1. **Providing context**: Database schema in the prompt
2. **Clear instructions**: "Generate SQL, nothing else"
3. **Examples in training**: LLMs are trained on SQL examples
4. **Low temperature**: Makes output more deterministic

Common challenges:
- **Complex queries**: May need prompt refinement
- **Ambiguous questions**: LLM might guess wrong
- **SQL dialects**: Need to specify PostgreSQL-specific features

Improvements possible:
- Few-shot learning (provide example questions + SQL)
- Query validation before execution
- Feedback loop (if query fails, try again with error context)

## 🐳 Docker Services

### PostgreSQL Container

```yaml
postgres:
  image: postgres:16-alpine
  environment:
    POSTGRES_USER: studyuser
    POSTGRES_PASSWORD: studypass
    POSTGRES_DB: studydb
  volumes:
    - postgres_data:/var/lib/postgresql/data  # Persist data
    - ./init.sql:/docker-entrypoint-initdb.d/init.sql  # Initialize on first start
```

**Why Alpine?** Smaller image size (40MB vs 130MB for regular)

### Ollama Container

```yaml
ollama:
  image: ollama/ollama:latest
  ports:
    - "11434:11434"
  volumes:
    - ollama_data:/root/.ollama  # Store downloaded models
```

**Models available:**
- `llama2` (7B) - Good general purpose, used in this project
- `mistral` (7B) - Often faster, good quality
- `codellama` (7B) - Specialized for code
- `llama2:13b` - Better quality, slower

Change model in `src/study_llm/llm.clj`:
```clojure
(def ollama-config
  {:model "mistral"})  ; Change here
```

## 🎓 Production Considerations

### What's Production-Ready in This Project?

✅ **Good practices:**
- Connection pooling (HikariCP)
- Prepared statements (SQL injection prevention)
- Error handling and logging
- Health checks in Docker
- Resource cleanup (connection pool shutdown)

⚠️ **What's missing for production:**
- Authentication and authorization
- Rate limiting
- Input validation and sanitization
- Monitoring and metrics
- Retry logic and circuit breakers
- Configuration management (environment variables)
- Automated tests
- API instead of terminal UI
- Query result caching
- SQL query validation before execution

### Security Considerations

**Current implementation:**
- ✅ Uses prepared statements (prevents SQL injection from user input)
- ✅ LLM generates SQL, not user directly typing it
- ❌ No validation of LLM-generated SQL before execution
- ❌ No query timeout limits
- ❌ No rate limiting

**For production:**
```clojure
;; Add SQL validation
(defn validate-sql [sql]
  ;; Check for dangerous operations
  (when (re-find #"(?i)(DROP|DELETE|TRUNCATE|ALTER)" sql)
    (throw (ex-info "Potentially dangerous SQL operation" {:sql sql})))
  ;; Limit to SELECT only
  (when-not (re-find #"(?i)^SELECT" (str/trim sql))
    (throw (ex-info "Only SELECT queries allowed" {:sql sql}))))

;; Add query timeout
(jdbc/execute! ds sql-vec {:timeout 5000})  ; 5 second timeout
```

### Scaling Considerations

**Database:**
- Read replicas for read-heavy workloads
- Connection pool sizing (HikariCP has good defaults)
- Query optimization (EXPLAIN ANALYZE)
- Caching (Redis for frequent queries)

**LLM:**
- Ollama can run on GPU for faster inference
- Can load-balance across multiple Ollama instances
- Consider caching text-to-SQL for common questions
- For high scale: hosted LLM API (OpenAI, Anthropic)

**Application:**
- Stateless design allows horizontal scaling
- Connection pool per instance
- Load balancer in front

## 📊 Sample Questions to Try

**Simple aggregations:**
- How many customers do we have?
- What is the average order value?
- How many products are in stock?

**Top N queries:**
- Show me the top 5 products by price
- What are the 3 most recent orders?
- Who are the top customers by spending?

**Grouping and analysis:**
- How many customers per country?
- What's the total revenue by product category?
- Show order count by status

**Joins:**
- Which products have never been ordered?
- Show me customer names with their order count
- List all orders with customer and product details

## 🛠️ Development

### Running with REPL

```bash
# Start REPL
clj

# In REPL
(require '[study-llm.core :as core])
(require '[study-llm.db :as db])
(require '[study-llm.llm :as llm])

;; Test database connection
(db/test-connection)

;; Check schema
(db/get-schema-info)

;; Test LLM
(llm/check-ollama-health)
(llm/generate-completion "What is 2+2?")
```

### Troubleshooting

**"Connection refused" to PostgreSQL**
```bash
# Check if container is running
docker-compose ps

# Check logs
docker-compose logs postgres

# Restart
docker-compose restart postgres
```

**"Connection refused" to Ollama**
```bash
# Check if container is running
docker-compose ps

# Check logs
docker-compose logs ollama

# Restart
docker-compose restart ollama
```

**LLM generates incorrect SQL**
- Try rephrasing your question more specifically
- Check if the question matches your data
- Review the schema with `schema` command
- Lower temperature for more predictable output

**Ollama is slow**
- First request is slower (model loading)
- Consider using a smaller model (llama2:7b vs llama2:13b)
- On Linux with NVIDIA GPU, uncomment GPU section in docker-compose.yml

### Cleanup

```bash
# Stop services
docker-compose down

# Remove volumes (deletes data!)
docker-compose down -v

# Remove downloaded models
docker volume rm study-llm_ollama_data
```

## 📖 Further Learning

### Clojure Resources
- [Clojure for the Brave and True](https://www.braveclojure.com/) - Free book
- [ClojureDocs](https://clojuredocs.org/) - Community documentation
- [Clojure Style Guide](https://guide.clojure.style/)

### LLM Resources
- [Prompt Engineering Guide](https://www.promptingguide.ai/)
- [Ollama Documentation](https://github.com/ollama/ollama)
- [Text-to-SQL Research](https://arxiv.org/abs/2301.07069)

### Database Resources
- [PostgreSQL Tutorial](https://www.postgresqltutorial.com/)
- [Use The Index, Luke](https://use-the-index-luke.com/) - SQL indexing
- [SQL Style Guide](https://www.sqlstyle.guide/)

## 🤝 Contributing

This is a learning project. Feel free to:
- Experiment with different LLM models
- Add more sample data
- Improve prompts
- Add features (caching, validation, etc.)
- Try different databases

## 📝 License

This is a study project for learning purposes.

## 🎯 Next Steps

Ideas to extend this project:
1. Add a web UI (React + ClojureScript)
2. Implement query caching
3. Add support for chart generation
4. Multi-turn conversations (follow-up questions)
5. Query explanation (EXPLAIN ANALYZE)
6. Support for CREATE/UPDATE operations
7. User authentication
8. Save conversation history
9. Export results to CSV/JSON
10. Add more sophisticated prompt engineering

Happy learning! 🚀