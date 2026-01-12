# LLM-Powered Database Chat System (Multi-Agent Architecture)

A learning project demonstrating how to build a production-ready system using a **multi-agent architecture** inspired by Microsoft Semantic Kernel and AutoGen. The system allows users to ask questions about a database in natural language using specialized AI agents that work together.

## ⭐ What's New: Agentic Framework

This project has been **refactored to use an agentic framework architecture**:

- **🤖 Specialized AI Agents**: SQL Generator, Database Executor, Result Analyzer
- **🔧 Tool Registry**: Agents use well-defined tools to accomplish tasks
- **🔄 Multi-Agent Orchestration**: Coordinated workflow between agents
- **💾 Memory Management**: Agents maintain conversation context
- **📐 Modular Design**: Each agent has a clear, focused responsibility

For detailed information about the agentic framework, see [AGENTIC_FRAMEWORK.md](AGENTIC_FRAMEWORK.md).

## 🎯 Project Goals

Learn how to:
- Build a Clojure application with real-world dependencies
- **Implement a multi-agent AI system from first principles**
- Use PostgreSQL for data storage
- Run a local LLM (without relying on external APIs)
- Integrate LLM with structured data (text-to-SQL)
- Create a terminal-based chat interface
- Build a production-ready architecture
- **Apply Microsoft Semantic Kernel and AutoGen principles in Clojure**

## 🏗️ Architecture

### Multi-Agent System Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     User (Terminal)                         │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              Clojure Application (study-llm)                │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │            Agentic Framework (agent.clj)             │  │
│  │  • Agent Protocol  • Tool Registry  • Orchestrator   │  │
│  │  • Memory Management                                 │  │
│  └──────────────────────────────────────────────────────┘  │
│         │                  │                  │             │
│         ▼                  ▼                  ▼             │
│  ┌─────────────┐  ┌───────────────┐  ┌──────────────┐     │
│  │ SQL Gen     │  │ DB Executor   │  │ Result       │     │
│  │ Agent       │  │ Agent         │  │ Analyzer     │     │
│  │             │  │               │  │ Agent        │     │
│  └─────────────┘  └───────────────┘  └──────────────┘     │
│         │                  │                  │             │
│         └──────────┬───────┴────────┬─────────┘             │
│                    │                │                       │
│            ┌───────▼──────┐  ┌──────▼─────┐                │
│            │  LLM Client  │  │ DB Client  │                │
│            │  (llm.clj)   │  │ (db.clj)   │                │
│            └──────────────┘  └────────────┘                │
└────────────────┬──────────────────┬──────────────────────────┘
                 │                  │
                 ▼                  ▼
┌──────────────────┐  ┌──────────────────┐
│   Ollama         │  │   PostgreSQL     │
│   (LLM Runtime)  │  │   (Database)     │
│   Port: 11434    │  │   Port: 5432     │
└──────────────────┘  └──────────────────┘
```

### Agent Pipeline

When you ask a question, it flows through specialized agents:

1. **SQL Generator Agent** (`sql_generator.clj`)
   - Converts natural language to SQL
   - Uses LLM with temperature=0.1 for accuracy
   - Tool: `generate-sql`

2. **Database Executor Agent** (`database_executor.clj`)
   - Executes SQL queries safely
   - Manages connection pooling
   - Tools: `execute-query`, `get-schema`

3. **Result Analyzer Agent** (`result_analyzer.clj`)
   - Interprets query results
   - Provides insights and summaries
   - Uses LLM with temperature=0.3 for creativity
   - Tool: `analyze-results`

4. **Orchestrator** (`agent.clj`)
   - Coordinates the agent pipeline
   - Manages context passing between agents
   - Maintains conversation memory

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

Once the application starts, you can ask questions in natural language. The multi-agent system will process your request:

```
You: What are the top 5 customers by total spent?

🤖 Multi-Agent System Processing...

Step 1: SQL Generator Agent - Converting question to SQL...
Generated SQL:
   SELECT name, total_spent FROM customers ORDER BY total_spent DESC LIMIT 5

Step 2: Database Executor Agent - Executing query...
✅ Query executed successfully!
Found 5 result(s)

Step 3: Result Analyzer Agent - Analyzing results...

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
- `help` - Show help information (including agent architecture)
- `schema` - View database schema
- `exit` or `quit` - Exit the application

## 📚 Learning Guide

### Multi-Agent Architecture

This project demonstrates a **production-ready agentic framework** inspired by Microsoft's AI frameworks:

#### **Why Agentic Architecture?**

Traditional monolithic approach:
```
User Question → Single Function → SQL → Database → Results → Analysis
```

Agentic approach (this project):
```
User Question → Orchestrator → [SQL Agent → DB Agent → Analyzer Agent] → Results
                     ↓
               Context & Memory Management
```

**Benefits**:
- **Modularity**: Each agent has a single, clear responsibility
- **Reusability**: Agents can be reused in different workflows
- **Testability**: Test each agent independently
- **Maintainability**: Easy to update or replace individual agents
- **Scalability**: Add new agents without modifying existing ones
- **Observability**: Track agent execution and performance

#### **Framework Inspiration**

This Clojure-native framework is inspired by:

1. **Microsoft Semantic Kernel**
   - Plugin/tool architecture
   - Planner and orchestrator patterns
   - Memory management

2. **Microsoft AutoGen**
   - Multi-agent conversations
   - Specialized agent roles
   - Collaborative problem solving

3. **LangChain**
   - Tool/function abstraction
   - Sequential chains
   - Composable components

4. **LangGraph**
   - Graph-based workflows
   - Multi-agent orchestration
   - State management patterns

**Why not use those frameworks directly?**
- Semantic Kernel: No official Clojure support (C#/Python/Java)
- AutoGen: Python-only implementation
- LangChain/LangGraph: Python/JavaScript focused, heavy dependencies
- **Our Solution**: Native Clojure implementation with:
  - No bridge overhead (pure JVM)
  - Functional programming patterns
  - Full control and customization
  - Educational value (learn agentic principles)
  - Minimal dependencies

**Pattern Convergence**: All successful agentic frameworks share core patterns—our implementation demonstrates these universal principles in Clojure.

See [AGENTIC_FRAMEWORK.md](AGENTIC_FRAMEWORK.md) for detailed architecture documentation and [LANGCHAIN_LANGGRAPH_COMPARISON.md](LANGCHAIN_LANGGRAPH_COMPARISON.md) for in-depth comparisons with LangChain and LangGraph.

### Why These Technologies?

#### **Clojure**
- **Functional programming**: Immutable data structures, pure functions
- **JVM ecosystem**: Access to mature Java libraries
- **REPL-driven development**: Interactive programming experience
- **Concurrency**: Built-in support for concurrent programming
- **Production use**: Used by companies like Nubank, Walmart, Apple
- **Perfect for agents**: Pure functions make agents predictable and composable

**Key Clojure concepts in this project:**
- Namespaces (`ns`) for code organization
- Protocols for agent abstraction
- Records for agent implementations
- Atoms for state management (agent memory)
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

#### Agentic Framework Core

##### `src/study_llm/agent.clj` - Core Framework (~230 lines)

The heart of the agentic architecture:

```clojure
;; Agent Protocol - All agents implement this
(defprotocol Agent
  (execute [this input context]))

;; Tool System
(defn create-tool [name description fn & {:keys [schema]}])
(defn invoke-tool [tool & args])

;; Memory Management
(defn create-memory [type])
(defn add-to-memory [memory entry])
(defn get-memory [memory & opts])

;; Multi-Agent Orchestration
(defn create-orchestrator [agents & {:keys [strategy]}])
(defn orchestrate [orchestrator input context])
```

**Key Features**:
- Agent abstraction via protocols
- Tool registry for agent capabilities
- Conversation memory with filters
- Sequential orchestration (parallel support planned)
- Context passing between agents

##### `src/study_llm/agents/sql_generator.clj` - SQL Agent (~70 lines)

Specialized agent for text-to-SQL conversion:

```clojure
(defn create-sql-generator-agent []
  (agent/create-llm-agent
    "sql-generator"
    "Converts natural language to SQL"
    {:generate (generate-sql-tool)}
    :config {:temperature 0.1}))
```

**Capabilities**:
- Schema-aware SQL generation
- Low temperature for accuracy
- Handles table relationships
- PostgreSQL optimization

##### `src/study_llm/agents/database_executor.clj` - Database Agent (~60 lines)

Specialized agent for query execution:

```clojure
(defn create-database-executor-agent []
  (agent/create-database-agent
    "database-executor"
    "Executes SQL queries"
    {:query (execute-query-tool)
     :schema (get-schema-tool)}))
```

**Capabilities**:
- Safe query execution
- Connection pooling
- Error handling
- Result formatting

##### `src/study_llm/agents/result_analyzer.clj` - Analyzer Agent (~60 lines)

Specialized agent for result interpretation:

```clojure
(defn create-result-analyzer-agent []
  (agent/create-llm-agent
    "result-analyzer"
    "Analyzes query results"
    {:analyze (analyze-results-tool)}
    :config {:temperature 0.3}))
```

**Capabilities**:
- Natural language summaries
- Insight generation
- Higher temperature for creativity
- Factual accuracy

#### Supporting Modules

##### `src/study_llm/chat.clj` - User Interface (~220 lines)

Orchestrates the multi-agent workflow:

```clojure
(defn process-question [question schema-info]
  ;; Create agents
  (let [sql-agent (sql-gen/create-sql-generator-agent)
        db-agent (db-exec/create-database-executor-agent)
        analyzer-agent (analyzer/create-result-analyzer-agent)
        
        ;; Create orchestrator
        orchestrator (agent/create-orchestrator
                       [sql-agent db-agent analyzer-agent])]
    
    ;; Execute agent pipeline
    (agent/orchestrate orchestrator question {:schema schema-info})))
```

**Agent Coordination**:
- Creates specialized agents
- Sets up orchestrator
- Passes context between agents
- Displays results

##### `deps.edn` - Dependency Management
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

##### `src/study_llm/db.clj` - Database Layer (~110 lines)

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

#### `src/study_llm/llm.clj` - LLM Integration (~140 lines)

**Note**: This module is retained for backward compatibility. The new agentic framework wraps these functions within specialized agents.

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

**Agent Integration**:
- SQL Generator Agent uses these functions with temperature=0.1
- Result Analyzer Agent uses these functions with temperature=0.3
- Both agents wrap LLM calls in the agent framework

#### `src/study_llm/core.clj` - Application Entry Point (~100 lines)

Application lifecycle with agent initialization:
```clojure
(defn -main [& args]
  ;; 1. Check dependencies
  (check-dependencies)
  ;; 2. Ensure model is available
  (ensure-model-available)
  ;; 3. Initialize multi-agent system
  (println "🤖 Multi-agent system initialized")
  ;; 4. Start chat interface
  (chat/start-chat)
  ;; 5. Cleanup (in finally block)
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

## 🔧 How It Works - Multi-Agent Architecture

### The Complete Agent Flow

1. **User asks a question** in the terminal
   ```
   "What are the top 5 customers by total spent?"
   ```

2. **Orchestrator initializes the agent pipeline**
   - Creates three specialized agents
   - Sets up context with database schema
   - Prepares memory for conversation tracking

3. **SQL Generator Agent processes the question**
   - **Input**: Natural language question + database schema
   - **Tool**: `generate-sql` (uses LLM with temperature=0.1)
   - **Process**: Creates optimized prompt with schema context
   - **Output**: SQL query
   ```sql
   SELECT name, total_spent FROM customers ORDER BY total_spent DESC LIMIT 5
   ```
   - **Updates Context**: Adds generated SQL for next agent

4. **Database Executor Agent runs the query**
   - **Input**: SQL query from previous agent
   - **Tool**: `execute-query` (uses HikariCP connection pool)
   - **Process**: Executes prepared statement against PostgreSQL
   - **Output**: Query results (array of maps)
   - **Updates Context**: Adds results and result count

5. **Result Analyzer Agent interprets the data**
   - **Input**: Original question + query results
   - **Tool**: `analyze-results` (uses LLM with temperature=0.3)
   - **Process**: Creates analysis prompt with results as JSON
   - **Output**: Human-readable summary with insights
   - **Updates Context**: Adds analysis text

6. **Orchestrator returns final result**
   - Combines outputs from all agents
   - Stores interaction in memory
   - Returns to chat interface for display

### Agent Coordination Example

```clojure
;; Simplified orchestration flow
(defn orchestrate-sequential [orchestrator input context]
  (loop [agents [sql-agent db-agent analyzer-agent]
         context {:schema [...], :question "..."}
         results []]
    (if (empty? agents)
      {:status :success, :results results}
      (let [agent (first agents)
            ;; Execute agent with current context
            result (agent/execute agent input context)
            ;; Merge agent's output into context
            updated-context (merge context (:updated-context result))]
        ;; Continue with next agent
        (recur (rest agents) updated-context (conj results result))))))
```

### Text-to-SQL: How Does It Work?

The SQL Generator Agent makes text-to-SQL work by:

1. **Providing context**: Database schema in the prompt
2. **Clear instructions**: "Generate SQL, nothing else"
3. **Examples in training**: LLMs are trained on SQL examples
4. **Low temperature**: Makes output more deterministic (0.1)
5. **Agent encapsulation**: Consistent prompt engineering

Common challenges and agent benefits:
- **Complex queries**: Agent can be enhanced with few-shot examples
- **Ambiguous questions**: Agent memory can track clarifications
- **SQL dialects**: Agent config specifies PostgreSQL
- **Error handling**: Agent wraps LLM calls with try-catch
- **Reusability**: Same agent can be used in different workflows

Improvements enabled by agentic architecture:
- **Few-shot learning**: Add examples to agent's tool
- **Query validation**: Add validation tool to SQL agent
- **Feedback loop**: Agent memory can store corrections
- **Multi-model**: Different agents can use different LLM models
- **Testing**: Test each agent independently

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
- **Agentic Architecture**: Modular, maintainable, testable
- **Agent Abstraction**: Clear separation of concerns
- **Tool Registry**: Extensible capability system
- **Memory Management**: Conversation context tracking
- Connection pooling (HikariCP)
- Prepared statements (SQL injection prevention)
- Error handling and logging
- Health checks in Docker
- Resource cleanup (connection pool shutdown)

✅ **Agentic Framework Benefits**:
- **Modularity**: Easy to add/modify agents
- **Testability**: Test each agent independently
- **Reusability**: Agents can be used in different workflows
- **Observability**: Track agent execution and performance
- **Scalability**: Horizontal scaling of stateless agents

⚠️ **What's missing for production:**
- Authentication and authorization
- Rate limiting (per agent)
- Input validation and sanitization
- Monitoring and metrics (agent performance tracking)
- Agent execution tracing
- Advanced orchestration strategies (parallel, dynamic)
- Long-term memory persistence
- Multi-model support per agent
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

# In REPL - Test core modules
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

# Test Agentic Framework
(require '[study-llm.agent :as agent])
(require '[study-llm.agents.sql-generator :as sql-gen])
(require '[study-llm.agents.database-executor :as db-exec])
(require '[study-llm.agents.result-analyzer :as analyzer])

;; Create agents
(def sql-agent (sql-gen/create-sql-generator-agent))
(def db-agent (db-exec/create-database-executor-agent))

;; Test individual agent
(agent/execute sql-agent 
               "How many customers?" 
               {:schema (db/get-schema-info)})

;; Test orchestration
(def pipeline (agent/create-orchestrator [sql-agent db-agent]))
(agent/orchestrate pipeline 
                   "How many customers?" 
                   {:schema (db/get-schema-info)})

;; Check agent memory
(agent/get-memory (:memory sql-agent) :limit 5)
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

**"Password authentication failed" for PostgreSQL**

**This issue has been fixed** in the latest version. The code now uses an explicit JDBC URL that ignores environment variables like `PGUSER`, `PGPASSWORD`, etc.

If you're using an older version and see this error, the application was picking up your system username. Update to the latest version or manually unset environment variables:

```bash
# For older versions only:
unset PGUSER PGPASSWORD PGHOST PGDATABASE PGPORT
clj -M:run
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

### Agentic AI Resources
- **[AGENTIC_FRAMEWORK.md](AGENTIC_FRAMEWORK.md)** - Detailed documentation of our framework
- **[LANGCHAIN_LANGGRAPH_COMPARISON.md](LANGCHAIN_LANGGRAPH_COMPARISON.md)** - How our architecture compares to LangChain and LangGraph
- [Microsoft Semantic Kernel](https://github.com/microsoft/semantic-kernel) - Official Microsoft framework
- [Microsoft AutoGen](https://github.com/microsoft/autogen) - Multi-agent conversation framework
- [LangChain](https://github.com/langchain-ai/langchain) - Python/JS framework for LLM applications
- [LangGraph](https://github.com/langchain-ai/langgraph) - Graph-based multi-agent workflows
- [Semantic Kernel Documentation](https://learn.microsoft.com/en-us/semantic-kernel/)
- [LangChain Documentation](https://python.langchain.com/docs/)
- [LangGraph Documentation](https://langchain-ai.github.io/langgraph/)

### Clojure Resources
- [Clojure for the Brave and True](https://www.braveclojure.com/) - Free book
- [ClojureDocs](https://clojuredocs.org/) - Community documentation
- [Clojure Style Guide](https://guide.clojure.style/)
- [Clojure Protocols](https://clojure.org/reference/protocols) - For understanding agent abstraction

### LLM Resources
- [Prompt Engineering Guide](https://www.promptingguide.ai/)
- [Ollama Documentation](https://github.com/ollama/ollama)
- [Text-to-SQL Research](https://arxiv.org/abs/2301.07069)
- [LLM Agent Patterns](https://www.anthropic.com/index/building-effective-agents)

### Database Resources
- [PostgreSQL Tutorial](https://www.postgresqltutorial.com/)
- [Use The Index, Luke](https://use-the-index-luke.com/) - SQL indexing
- [SQL Style Guide](https://www.sqlstyle.guide/)

## 🤝 Contributing

This is a learning project. Feel free to:
- Experiment with different LLM models
- Add new specialized agents
- Implement parallel orchestration
- Add more sophisticated prompt engineering
- Improve agent memory management
- Add agent performance monitoring
- Create new tools for existing agents
- Try different databases

## 📝 License

This is a study project for learning purposes.

## 🎯 Next Steps

Ideas to extend this project using the agentic framework:

**New Agents**:
1. **Chart Generator Agent** - Visualize query results
2. **Query Validator Agent** - Validate SQL before execution
3. **Schema Optimizer Agent** - Suggest index improvements
4. **Conversation Manager Agent** - Handle multi-turn dialogues
5. **Error Recovery Agent** - Fix failed queries automatically

**Framework Enhancements**:
6. **Parallel Orchestration** - Run independent agents concurrently
7. **Dynamic Planning** - LLM decides which agents to use
8. **Agent Communication** - Direct agent-to-agent messaging
9. **Persistent Memory** - Store conversation history in database
10. **Tool Discovery** - Automatic tool registration and selection

**Application Features**:
11. Add a web UI (React + ClojureScript)
12. Implement query result caching
13. Support for CREATE/UPDATE operations (with validation agent)
14. User authentication and multi-user support
15. Export results to CSV/JSON
16. Real-time agent execution monitoring
17. A/B testing different agent configurations
18. Multi-model support (different LLMs per agent)

**Production Readiness**:
19. Add comprehensive test suite (unit tests for each agent)
20. Implement distributed tracing for agent pipelines
21. Add metrics collection (agent latency, success rate)
22. Circuit breakers for agent failures
23. Rate limiting per agent
24. Agent versioning and rollback

Happy learning! 🚀

---

**Built with**: Multi-Agent Architecture inspired by Microsoft Semantic Kernel  
**Framework**: Clojure-native agentic implementation  
**Technologies**: Clojure, PostgreSQL, Ollama, Docker  
**Learning Value**: High - covers AI agents, functional programming, production patterns