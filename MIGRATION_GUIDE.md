# Migration Guide: Agent-Based Architecture

## What Changed?

This project has been **refactored** from a monolithic architecture to an **agent-based architecture** inspired by Microsoft's Agentic Framework (Semantic Kernel and AutoGen).

## Quick Summary

### Before
```
User → Chat → LLM + DB (tightly coupled)
```

### After
```
User → Chat → Orchestrator → [SQL Agent, Query Agent, Analysis Agent] → Tools → LLM + DB
```

## What You Need to Know

### For Users

**Nothing changes for you!** The application works exactly the same way:
- Start with: `clj -M:run` or `./setup.sh`
- Ask questions in natural language
- Get SQL generation, query execution, and analysis

The only difference you'll notice:
- The help text mentions "agent-based architecture"
- The output says "Using agent-based orchestrator"

### For Developers

**Everything is better organized:**

1. **Clear Responsibilities**: Each agent has one job
   - SQL Agent: Convert questions to SQL
   - Query Agent: Execute SQL
   - Analysis Agent: Analyze results
   
2. **Easy Testing**: Test each agent independently

3. **Easy Extension**: Add new agents without modifying existing code

4. **Better Debugging**: Clear boundaries make issues easier to find

## Architecture Overview

### Core Components

```
src/study_llm/
├── agent.clj              # NEW: Core agent framework
├── tools.clj              # NEW: Tool registry
├── orchestrator.clj       # NEW: Coordinates agents
├── agents/
│   ├── sql_agent.clj     # NEW: SQL generation
│   ├── query_agent.clj   # NEW: Query execution
│   └── analysis_agent.clj # NEW: Result analysis
├── chat.clj              # UPDATED: Uses orchestrator
├── core.clj              # UNCHANGED
├── db.clj                # UNCHANGED
└── llm.clj               # UNCHANGED
```

### How It Works

```
1. User asks: "What are the top 5 customers?"
   ↓
2. Orchestrator creates context and agents
   ↓
3. SQL Agent:
   - Gets database schema (tool)
   - Generates SQL via LLM (tool)
   - Returns: "SELECT name, total_spent FROM customers..."
   ↓
4. Query Agent:
   - Executes SQL (tool)
   - Returns: [{name: "Alice", total_spent: 1250}, ...]
   ↓
5. Analysis Agent:
   - Analyzes results via LLM (tool)
   - Returns: "The top 5 customers are..."
   ↓
6. User sees formatted analysis
```

## Key Concepts from Microsoft Agentic Framework

### 1. Agents
Self-contained units with:
- A clear role
- Specific tools they can use
- Ability to execute tasks independently

**Example:**
```clojure
(def sql-agent
  (create-agent
    "sql-generator"
    "Converts natural language to SQL"
    [get-schema-tool, generate-text-tool]
    sql-generation-logic))
```

### 2. Tools
Reusable functions that agents invoke:

**Example:**
```clojure
(def query-database-tool
  (create-tool
    "query-database"
    "Execute SQL against PostgreSQL"
    {:sql "SQL query string"}
    execute-query-fn))
```

### 3. Orchestration
Coordinates agents to complete complex tasks:

**Example:**
```clojure
(defn orchestrate-question-answering [question]
  ;; 1. SQL Agent generates SQL
  ;; 2. Query Agent executes query
  ;; 3. Analysis Agent analyzes results
  ;; All coordinated automatically!
  )
```

### 4. Context
State that flows between agents:

**Example:**
```clojure
{:history [...previous steps...]
 :shared-state {:schema-info ...}
 :session-id "uuid-123"}
```

## Why This Architecture?

### Benefits

✅ **Modularity**: Each piece is independent and testable
✅ **Extensibility**: Add new agents/tools without changing existing code
✅ **Reusability**: Tools can be shared across agents
✅ **Maintainability**: Easier to understand and debug
✅ **Testability**: Test agents in isolation
✅ **Flexibility**: Different orchestration patterns possible

### Comparison to Microsoft's Frameworks

| Feature | Microsoft Semantic Kernel | Our Implementation |
|---------|--------------------------|-------------------|
| Language | C# / Python | Clojure |
| Agents | ✅ | ✅ |
| Tools/Plugins | ✅ | ✅ (as Tools) |
| Orchestration | ✅ | ✅ |
| Context/Memory | ✅ | ✅ |
| Multi-Agent | ✅ (AutoGen) | ✅ (Extendable) |

We implement the **core concepts** without the FFI complexity!

## Migration Impact

### No Breaking Changes

✅ All existing functionality works
✅ No changes to configuration
✅ No changes to Docker setup
✅ No changes to database schema
✅ Same commands to run the application

### What's New

➕ Better code organization
➕ Easier to test
➕ Easier to extend
➕ Better logging and debugging
➕ Comprehensive documentation

## Developer Guide

### Adding a New Agent

```clojure
;; 1. Create the agent namespace
(ns study-llm.agents.my-agent
  (:require [study-llm.agent :as agent]
            [study-llm.tools :as tools]))

;; 2. Define execution logic
(defn my-agent-execute [agent input context]
  (let [result (do-something input)]
    (agent/format-agent-response result context)))

;; 3. Create agent constructor
(defn create-my-agent []
  (agent/create-agent
    "my-agent-name"
    "What this agent does"
    [tool1 tool2]  ; tools it uses
    my-agent-execute))

;; 4. Use in orchestrator
(let [my-agent (create-my-agent)]
  (agent/execute my-agent input context))
```

### Adding a New Tool

```clojure
;; In tools.clj
(def my-new-tool
  (agent/create-tool
    "my-tool"
    "What this tool does"
    {:param1 "Description of param1"}
    (fn [params context]
      ;; Tool logic here
      {:status :success
       :result result-value})))

;; Register in tool registry
(def all-tools
  {:my-category {:my-tool my-new-tool}})
```

### Testing Agents

```clojure
(deftest sql-agent-test
  (testing "SQL generation"
    (let [agent (create-sql-agent)
          context (agent/create-context)
          input {:question "How many customers?"}
          result (agent/execute agent input context)]
      (is (= :success (:status result)))
      (is (string? (:result result))))))
```

## Documentation

### Read More

1. **AGENT_ARCHITECTURE.md** - Comprehensive architecture guide
   - Detailed explanation of all components
   - Design decisions and rationale
   - Future enhancements
   - References to Microsoft resources

2. **README.md** - Updated with agent architecture info

3. **Code Comments** - Extensive documentation in the code

## Frequently Asked Questions

### Why not use Microsoft's framework directly?

**Answer**: Microsoft's frameworks are Python/.NET. Using them in Clojure would require:
- Complex FFI (Foreign Function Interface)
- Running Python/C# runtime alongside JVM
- Significant complexity and maintenance burden

Instead, we **implemented the core concepts** in idiomatic Clojure, giving us:
- All the benefits of agent-based architecture
- Clean, simple Clojure code
- Full control and understanding
- No additional runtime dependencies

### Will this make the application slower?

**Answer**: No! The agent architecture adds **minimal overhead**:
- Agent calls compile to direct method calls
- No network calls or serialization
- Context is just a map passed by reference

In fact, it enables **future optimizations**:
- Easy to add caching at the tool level
- Can parallelize independent agents
- Better profiling and optimization

### Can I still use the old code?

**Answer**: The old code is replaced, but:
- All functionality is preserved
- The behavior is identical
- The architecture is just better organized
- You can see the old code in git history

### How do I contribute a new agent?

**Answer**: Follow these steps:
1. Create a new file in `src/study_llm/agents/`
2. Implement the agent using the base framework
3. Add it to the orchestrator
4. Write tests
5. Update documentation
6. Submit a pull request

See **AGENT_ARCHITECTURE.md** for detailed examples.

## Next Steps

1. **Try it out**: Run the application and see it works the same
2. **Read the docs**: Check out AGENT_ARCHITECTURE.md
3. **Explore the code**: Look at the new agent files
4. **Extend it**: Try adding your own agent or tool
5. **Share feedback**: Let us know what you think!

## Support

If you have questions about the refactoring:
1. Read **AGENT_ARCHITECTURE.md** for detailed explanations
2. Check the code comments - they're comprehensive
3. Look at the examples in the documentation
4. Open an issue on GitHub

---

**Summary**: Better architecture, same functionality, more opportunities! 🚀
