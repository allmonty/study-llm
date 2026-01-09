# Agent-Based Architecture Refactoring

## Overview

This document explains the refactoring of the LLM-powered database chat system to use an **agent-based architecture** inspired by Microsoft's Agentic Framework principles.

## Why Agent-Based Architecture?

### Problem with Original Implementation

The original implementation was a **monolithic approach** where:
- All logic was tightly coupled in a few files
- Difficult to test individual components
- Hard to extend with new capabilities
- Limited reusability of components
- No clear separation of concerns

### Benefits of Agent-Based Architecture

The new architecture provides:

1. **Modularity**: Each agent has a single, well-defined responsibility
2. **Composability**: Agents can be combined in different ways
3. **Extensibility**: New agents can be added without modifying existing code
4. **Testability**: Each agent can be tested independently
5. **Maintainability**: Easier to understand and modify
6. **Reusability**: Agents and tools can be reused across different workflows

## Microsoft Agentic Framework Inspiration

### What is Microsoft Agentic Framework?

Microsoft has developed several frameworks for building AI agents:

1. **Semantic Kernel**: A framework for orchestrating AI plugins and agents
2. **AutoGen**: A framework for building multi-agent conversational systems

While these frameworks are primarily Python/.NET-based, we've implemented the **core concepts** in Clojure:

### Core Concepts Implemented

#### 1. Agent Abstraction

**Microsoft's Approach**: Agents are autonomous entities with specific roles and capabilities.

**Our Implementation**:
```clojure
(defprotocol Agent
  (get-name [this])
  (get-description [this])
  (get-tools [this])
  (execute [this input context]))
```

Each agent:
- Has a clear name and description
- Declares its capabilities (tools)
- Executes its specialized task
- Maintains context across interactions

#### 2. Tool/Function Calling

**Microsoft's Approach**: Tools are discrete functions that agents can invoke to perform actions.

**Our Implementation**:
```clojure
(defprotocol Tool
  (get-tool-name [this])
  (get-tool-description [this])
  (get-tool-parameters [this])
  (invoke-tool [this params context]))
```

Tools are:
- Reusable across different agents
- Self-describing (name, description, parameters)
- Stateless and composable

#### 3. Planning & Orchestration

**Microsoft's Approach**: An orchestrator coordinates multiple agents to complete complex tasks.

**Our Implementation**:
```clojure
;; Orchestrator coordinates agents in sequence
(defn orchestrate-question-answering [user-question]
  (let [sql-agent (create-sql-agent)
        query-agent (create-query-agent)
        analysis-agent (create-analysis-agent)]
    ;; Execute agents in coordinated sequence
    ...))
```

#### 4. Context Management

**Microsoft's Approach**: Context flows between agents, maintaining state and history.

**Our Implementation**:
```clojure
(defn create-context
  "Create execution context with history, metadata, and shared state"
  []
  {:history []
   :metadata {}
   :shared-state {}
   :session-id (str (java.util.UUID/randomUUID))})
```

## Architecture Overview

### Component Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    User Interface                        │
│                     (chat.clj)                          │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                   Orchestrator                          │
│                 (orchestrator.clj)                      │
│  • Coordinates agent execution                          │
│  • Manages context flow                                 │
│  • Handles errors and results                           │
└────┬──────────────┬──────────────┬─────────────────────┘
     │              │              │
     ▼              ▼              ▼
┌─────────┐   ┌─────────┐   ┌─────────┐
│   SQL   │   │  Query  │   │Analysis │
│  Agent  │   │  Agent  │   │  Agent  │
└────┬────┘   └────┬────┘   └────┬────┘
     │             │              │
     │             │              │
     ▼             ▼              ▼
┌─────────────────────────────────────┐
│            Tool Registry             │
│           (tools.clj)                │
│  • Database Tools                    │
│  • LLM Tools                         │
└───┬──────────────┬──────────────────┘
    │              │
    ▼              ▼
┌────────┐    ┌────────┐
│Database│    │  LLM   │
│(db.clj)│    │(llm.clj│
└────────┘    └────────┘
```

### Agent Flow

```
User Question
     │
     ▼
┌─────────────────────────────────────┐
│  Orchestrator                        │
│  1. Creates execution context        │
│  2. Initializes agents               │
│  3. Coordinates execution            │
└─────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────┐
│  SQL Agent                           │
│  • Gets database schema (tool)       │
│  • Generates SQL via LLM (tool)      │
│  • Returns SQL query                 │
└─────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────┐
│  Query Agent                         │
│  • Executes SQL (tool)               │
│  • Returns query results             │
└─────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────┐
│  Analysis Agent                      │
│  • Analyzes results via LLM (tool)   │
│  • Returns human-readable analysis   │
└─────────────────────────────────────┘
     │
     ▼
Final Result to User
```

## Implementation Details

### File Structure

```
src/study_llm/
├── agent.clj                 # Core agent framework
├── tools.clj                 # Tool definitions and registry
├── orchestrator.clj          # Agent coordination
├── agents/
│   ├── sql_agent.clj        # SQL generation agent
│   ├── query_agent.clj      # Query execution agent
│   └── analysis_agent.clj   # Result analysis agent
├── chat.clj                 # Updated to use orchestrator
├── core.clj                 # Application entry point
├── db.clj                   # Database utilities (unchanged)
└── llm.clj                  # LLM utilities (unchanged)
```

### Key Components

#### 1. Core Agent Framework (`agent.clj`)

Defines the fundamental abstractions:
- `Agent` protocol: Interface for all agents
- `Tool` protocol: Interface for all tools
- Context management utilities
- Base implementations for easy agent creation

**Why This Design?**
- Protocols provide polymorphism in Clojure
- Records implement protocols efficiently
- Context as a map provides flexibility
- Inspired by Microsoft's agent abstraction

#### 2. Tool Registry (`tools.clj`)

Provides reusable tools for agents:
- Database tools: query execution, schema retrieval
- LLM tools: text generation, health checks
- Organized by category for easy discovery

**Why This Design?**
- Tools are stateless and composable
- Self-describing (name, description, parameters)
- Can be shared across multiple agents
- Easy to add new tools without modifying agents

#### 3. Specialized Agents

**SQL Agent** (`sql_agent.clj`):
- Responsibility: Convert natural language to SQL
- Tools used: get-schema, generate-text
- Input: User question
- Output: SQL query

**Query Agent** (`query_agent.clj`):
- Responsibility: Execute SQL queries
- Tools used: query-database
- Input: SQL query
- Output: Query results

**Analysis Agent** (`analysis_agent.clj`):
- Responsibility: Analyze and summarize results
- Tools used: generate-text
- Input: Question + results
- Output: Human-readable analysis

**Why Separate Agents?**
- Each has a single, clear responsibility
- Can be tested independently
- Can be reused in different workflows
- Easier to maintain and extend

#### 4. Orchestrator (`orchestrator.clj`)

Coordinates agent execution:
- Plans the execution sequence
- Manages context flow between agents
- Handles errors at each step
- Aggregates results

**Why an Orchestrator?**
- Separates coordination logic from agent logic
- Makes the workflow explicit and maintainable
- Enables different orchestration strategies
- Follows Microsoft's planning pattern

## Comparison: Before vs After

### Before: Monolithic Approach

```clojure
;; chat.clj - Everything in one place
(defn process-question [question schema-info]
  ;; Generate SQL
  (let [sql-result (llm/generate-sql-from-question question schema-info)]
    ;; Execute query
    (let [query-results (db/execute-query! [sql])]
      ;; Analyze results
      (let [analysis (llm/analyze-results question query-results)]
        ...))))
```

**Problems**:
- All logic coupled together
- Hard to test individual steps
- Cannot reuse components
- Difficult to extend

### After: Agent-Based Approach

```clojure
;; orchestrator.clj - Clear separation
(defn orchestrate-question-answering [user-question]
  (let [sql-agent (create-sql-agent)
        query-agent (create-query-agent)
        analysis-agent (create-analysis-agent)]
    ;; Execute agents with clear responsibilities
    (-> (agent/execute sql-agent {:question user-question} context)
        (handle-sql-result query-agent)
        (handle-query-result analysis-agent))))
```

**Benefits**:
- Each agent is independent
- Easy to test each agent
- Components are reusable
- Simple to add new agents

## Why Not Use Microsoft's Framework Directly?

### Considerations

1. **Language Mismatch**: Microsoft's frameworks are Python/.NET, this project is Clojure
2. **Complexity**: Would require FFI (Foreign Function Interface) or microservices
3. **Dependencies**: Adding Python/C# runtime increases complexity
4. **Learning**: Implementing patterns helps understand the concepts better

### Our Approach

Instead of forcing a foreign framework, we:
1. **Study** the core concepts from Microsoft's frameworks
2. **Adapt** them to Clojure's strengths (protocols, immutability, REPL)
3. **Implement** clean, idiomatic Clojure code
4. **Document** how our implementation maps to the original concepts

This gives us:
- ✅ The benefits of agent-based architecture
- ✅ Clean, idiomatic Clojure code
- ✅ No complex FFI or additional runtimes
- ✅ Full control and understanding
- ✅ Easy to maintain and extend

## Extensibility Examples

### Adding a New Agent

```clojure
;; New validation agent
(ns study-llm.agents.validation-agent
  (:require [study-llm.agent :as agent]))

(defn validation-agent-execute [agent input context]
  ;; Validate SQL before execution
  ...)

(defn create-validation-agent []
  (agent/create-agent
    "sql-validator"
    "Validates SQL queries for safety"
    [validation-tool]
    validation-agent-execute))

;; Use in orchestrator
(defn orchestrate-safe-question-answering [question]
  (let [sql-agent (create-sql-agent)
        validation-agent (create-validation-agent)  ; New!
        query-agent (create-query-agent)
        analysis-agent (create-analysis-agent)]
    ;; Add validation step
    ...))
```

### Adding a New Tool

```clojure
;; New caching tool
(def cache-tool
  (agent/create-tool
    "query-cache"
    "Cache query results for faster responses"
    {:key "Cache key"}
    (fn [params context]
      ;; Check cache, return cached result
      ...)))

;; Any agent can now use this tool
```

## Testing Strategy

### Unit Testing Agents

```clojure
(deftest sql-agent-test
  (let [agent (create-sql-agent)
        context (agent/create-context)
        result (agent/execute agent
                             {:question "How many customers?"}
                             context)]
    (is (= :success (:status result)))
    (is (string? (:result result)))))
```

### Unit Testing Tools

```clojure
(deftest database-query-tool-test
  (let [tool (get-tool :database :query-database)
        result (agent/invoke-tool tool
                                  {:sql "SELECT 1"}
                                  {})]
    (is (= :success (:status result)))))
```

### Integration Testing

```clojure
(deftest orchestrator-integration-test
  (let [result (orchestrator/answer-question
                 "How many customers?")]
    (is (= :success (:status result)))
    (is (contains? result :sql))
    (is (contains? result :results))
    (is (contains? result :analysis))))
```

## Performance Considerations

### No Performance Penalty

The agent-based architecture adds **minimal overhead**:
- Agent protocol calls compile to direct method calls
- Context is just a map passed by reference
- No additional network calls or serialization
- Tools are simple function wrappers

### Benefits for Performance

Actually **improves** performance potential:
- Easy to add caching at the tool level
- Can parallelize independent agents in the future
- Clear boundaries make profiling easier
- Can optimize individual tools without affecting agents

## Future Enhancements

### Possible Extensions

1. **Parallel Agent Execution**: Run independent agents concurrently
2. **Agent Learning**: Agents remember successful strategies
3. **Dynamic Orchestration**: Choose agents based on question type
4. **Multi-Modal Agents**: Agents that handle images, charts, etc.
5. **Agent Marketplace**: Plugin system for third-party agents
6. **Streaming Results**: Stream partial results as agents execute
7. **Human-in-the-Loop**: Ask user for confirmation at key steps

### Mapping to Microsoft's Advanced Features

| Microsoft Feature | Our Potential Implementation |
|-------------------|------------------------------|
| Planner | Dynamic orchestrator that chooses agents |
| Memory | Persistent context across sessions |
| Plugins | Tool registry with dynamic loading |
| Multi-Agent | Multiple orchestrators working together |
| Streaming | Async agent execution with callbacks |

## Conclusion

This refactoring demonstrates that:

1. **Agentic patterns are language-agnostic**: The core concepts work in any language
2. **Simpler can be better**: Direct implementation beats complex FFI
3. **Understanding beats tools**: Building it yourself teaches the patterns
4. **Clojure is excellent for this**: Protocols, immutability, and simplicity shine

The result is a **production-ready, extensible, maintainable** agent-based system that:
- ✅ Implements Microsoft Agentic Framework principles
- ✅ Uses idiomatic, clean Clojure code
- ✅ Provides better modularity and testability
- ✅ Enables future enhancements
- ✅ Serves as a learning reference

## References

### Microsoft Resources
- [Semantic Kernel Documentation](https://learn.microsoft.com/en-us/semantic-kernel/)
- [AutoGen Framework](https://github.com/microsoft/autogen)
- [AI Agent Design Patterns](https://learn.microsoft.com/en-us/ai/)

### Related Concepts
- [ReAct Pattern](https://arxiv.org/abs/2210.03629) - Reasoning and Acting agents
- [LangChain Agents](https://python.langchain.com/docs/modules/agents/) - Similar patterns in Python
- [Agent-Based Modeling](https://en.wikipedia.org/wiki/Agent-based_model)

### Clojure Resources
- [Protocols and Records](https://clojure.org/reference/protocols)
- [Functional Architecture](https://www.youtube.com/watch?v=3oQTSP4FngY)
- [Component Pattern](https://github.com/stuartsierra/component)
