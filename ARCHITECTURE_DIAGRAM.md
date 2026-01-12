# Multi-Agent Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           USER INPUT                                    │
│                     "What are top 5 customers?"                         │
└────────────────────────────┬────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         ORCHESTRATOR                                    │
│                      (agent.clj)                                        │
│                                                                         │
│  Strategy: Sequential                                                   │
│  Memory: Conversation History                                           │
│  Context: {:schema [...], :question "..."}                              │
└─────────────────────────────┬───────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
              ▼               ▼               ▼
┌─────────────────┐  ┌────────────────┐  ┌──────────────────┐
│ SQL GENERATOR   │  │  DATABASE      │  │  RESULT          │
│ AGENT           │  │  EXECUTOR      │  │  ANALYZER        │
│                 │  │  AGENT         │  │  AGENT           │
│ sql_generator   │  │  database_     │  │  result_         │
│ .clj            │  │  executor.clj  │  │  analyzer.clj    │
├─────────────────┤  ├────────────────┤  ├──────────────────┤
│ Tools:          │  │ Tools:         │  │ Tools:           │
│ • generate-sql  │  │ • execute-     │  │ • analyze-       │
│                 │  │   query        │  │   results        │
│ Config:         │  │ • get-schema   │  │                  │
│ • temp: 0.1     │  │                │  │ Config:          │
│ • model: llama2 │  │ Config:        │  │ • temp: 0.3      │
│                 │  │ • timeout: 30s │  │ • model: llama2  │
│ Memory:         │  │ • max: 1000    │  │                  │
│ • SQL history   │  │                │  │ Memory:          │
│                 │  │ Memory:        │  │ • Analysis       │
│                 │  │ • Query hist.  │  │   history        │
└────────┬────────┘  └────────┬───────┘  └────────┬─────────┘
         │                    │                    │
         │                    │                    │
         └──────────┬─────────┴─────────┬──────────┘
                    │                   │
                    ▼                   ▼
         ┌──────────────────┐  ┌────────────────┐
         │  LLM CLIENT      │  │  DB CLIENT     │
         │  (llm.clj)       │  │  (db.clj)      │
         │                  │  │                │
         │ • Ollama API     │  │ • HikariCP     │
         │ • Prompts        │  │ • JDBC         │
         │ • Temperature    │  │ • Pooling      │
         └────────┬─────────┘  └────────┬───────┘
                  │                     │
                  ▼                     ▼
         ┌──────────────────┐  ┌────────────────┐
         │  OLLAMA          │  │  POSTGRESQL    │
         │  Container       │  │  Container     │
         │  Port: 11434     │  │  Port: 5432    │
         └──────────────────┘  └────────────────┘
```

## Data Flow Example

### Step 1: SQL Generator Agent
```
Input: 
  Question: "What are top 5 customers?"
  Context: {:schema [{:table "customers" :columns [...]}]}

Process:
  1. Create prompt with schema
  2. Call LLM (temp=0.1)
  3. Extract SQL from response

Output:
  SQL: "SELECT name, total_spent FROM customers ORDER BY total_spent DESC LIMIT 5"
  Updated Context: {:generated-sql "SELECT..."}
```

### Step 2: Database Executor Agent
```
Input:
  SQL: "SELECT name, total_spent FROM customers..."
  Context: {:schema [...], :generated-sql "SELECT..."}

Process:
  1. Get connection from pool
  2. Execute prepared statement
  3. Format results

Output:
  Results: [{:name "Grace Lee" :total_spent 3210.00}, ...]
  Updated Context: {:query-results [...], :result-count 5}
```

### Step 3: Result Analyzer Agent
```
Input:
  Question: "What are top 5 customers?"
  Context: {:query-results [...]}

Process:
  1. Create analysis prompt with results
  2. Call LLM (temp=0.3)
  3. Extract analysis

Output:
  Analysis: "The top 5 customers by spending are: 1. Grace Lee - $3,210..."
  Updated Context: {:analysis "The top 5..."}
```

## Agent Protocol

```clojure
(defprotocol Agent
  (execute [this input context]
    "Execute agent with input and context.
    Returns: {:status :success/:error
             :result <agent-output>
             :updated-context <context-updates>}"))
```

## Tool Pattern

```clojure
(defn create-tool [name description fn]
  {:name name
   :description description  ; Used for agent planning
   :fn fn})                 ; Actual implementation

(defn invoke-tool [tool & args]
  (apply (:fn tool) args))
```

## Orchestration Flow

```clojure
;; Simplified orchestration
(defn orchestrate-sequential [orchestrator input context]
  (reduce
    (fn [ctx agent]
      (let [result (execute agent input ctx)]
        (if (= :success (:status result))
          (merge ctx (:updated-context result))
          (reduced {:error result}))))
    context
    (:agents orchestrator)))
```

## Memory System

```clojure
;; Create memory
(def memory (create-memory :conversation))

;; Add entry
(add-to-memory memory 
  {:input "question"
   :result "answer"
   :timestamp (now)})

;; Retrieve entries
(get-memory memory :limit 5)
;; => [{:input "..." :result "..." :timestamp ...}]
```

## Context Passing

```
Initial Context:
  {:schema [...]}
      ↓
After SQL Generator:
  {:schema [...], :generated-sql "SELECT..."}
      ↓
After DB Executor:
  {:schema [...], :generated-sql "...", :query-results [...], :result-count 5}
      ↓
After Analyzer:
  {:schema [...], :generated-sql "...", :query-results [...], :analysis "..."}
```

## Comparison: Before vs After

### Before (Monolithic)
```clojure
(defn process-question [q]
  (let [sql (generate-sql q)           ; Single function
        results (execute-query sql)     ; Single function
        analysis (analyze results)]     ; Single function
    analysis))
```

### After (Agentic)
```clojure
(defn process-question [q]
  (let [agents [(create-sql-agent)      ; Specialized agent
                (create-db-agent)       ; Specialized agent
                (create-analyzer-agent)] ; Specialized agent
        orchestrator (create-orchestrator agents)]
    (orchestrate orchestrator q {:schema [...]})))
```

## Benefits Illustrated

```
┌──────────────────┐
│  MODULARITY      │
│  Each box is     │
│  independent     │
└──────────────────┘

┌──────────────────┐
│  REUSABILITY     │
│  Agents can be   │
│  used elsewhere  │
└──────────────────┘

┌──────────────────┐
│  TESTABILITY     │
│  Test each       │
│  agent in        │
│  isolation       │
└──────────────────┘

┌──────────────────┐
│  OBSERVABILITY   │
│  Track each      │
│  agent's perf.   │
└──────────────────┘

┌──────────────────┐
│  EXTENSIBILITY   │
│  Add new agents  │
│  easily          │
└──────────────────┘
```

## Future Enhancements

### Parallel Orchestration
```
                    ┌─ SQL Validator Agent
User → Orchestrator ┼─ SQL Generator Agent  ─→ Merge → DB Agent
                    └─ Query Optimizer Agent
```

### Dynamic Planning
```
User → Planning Agent → [Determines which agents to use] → Orchestrator
```

### Agent Communication
```
Agent A ←─→ Agent B
   ↓           ↓
   └───→ Agent C
```

---

For detailed implementation, see:
- `src/study_llm/agent.clj` - Core framework
- `src/study_llm/agents/` - Specialized agents
- `AGENTIC_FRAMEWORK.md` - Detailed architecture
- `REFACTORING_DECISIONS.md` - Decision rationale
