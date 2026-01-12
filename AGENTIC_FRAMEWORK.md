# Agentic Framework Architecture

## Overview

This project has been refactored to use an **Agentic Framework** architecture inspired by **Microsoft Semantic Kernel** and **Microsoft AutoGen**, but implemented natively in Clojure to leverage functional programming patterns and JVM performance.

## What is an Agentic Framework?

An agentic framework is a software architecture pattern that uses autonomous AI agents to accomplish complex tasks. Key characteristics:

1. **Agent Abstraction** - Encapsulating specific capabilities and responsibilities
2. **Tool/Function Calling** - Agents can use tools to accomplish tasks
3. **Multi-Agent Orchestration** - Coordination between multiple specialized agents
4. **Memory/State Management** - Agents maintain context across interactions
5. **Planning & Reasoning** - Breaking down complex tasks into steps

## Why Use an Agentic Framework?

### Benefits

- **Modularity**: Each agent has a clear, focused responsibility
- **Reusability**: Agents and tools can be reused across different workflows
- **Maintainability**: Easier to update, test, and debug individual agents
- **Scalability**: Can add new agents without modifying existing ones
- **Composability**: Agents can be combined in different ways for different tasks
- **Testability**: Individual agents can be tested in isolation

### Comparison to Traditional Approach

**Before (Monolithic)**:
```
User Question → Single LLM Function → SQL → Database → Results → Analysis
```

**After (Agentic)**:
```
User Question → Orchestrator → [SQL Agent → DB Agent → Analyzer Agent] → Results
                     ↓
               Context & Memory Management
```

## Framework Choice Rationale

### Microsoft Frameworks Evaluated

#### 1. Microsoft Semantic Kernel
- **Languages**: C#, Python, Java
- **Pros**: 
  - Official Microsoft support
  - Rich plugin ecosystem
  - Good documentation
- **Cons**: 
  - No official Clojure support
  - Would require Java interop (adds complexity)
  - Designed primarily for .NET ecosystem

#### 2. Microsoft AutoGen
- **Languages**: Python only
- **Pros**:
  - Excellent multi-agent conversation framework
  - Research-backed approach
- **Cons**:
  - Python-only (would require complete rewrite)
  - Not suitable for JVM/Clojure ecosystem

### Decision: Clojure-Native Agentic Framework

**Chosen Approach**: Implement agentic principles natively in Clojure

**Reasoning**:

1. **JVM Performance**: No overhead from Python bridge or Java interop
2. **Functional Paradigm**: Clojure's functional nature aligns perfectly with agent composition
3. **Simplicity**: No external framework dependencies to manage
4. **Educational Value**: Shows how to build agentic systems from first principles
5. **Customization**: Full control over agent behavior and orchestration
6. **Production-Ready**: Pure Clojure is easier to deploy and maintain

**Inspired By**:
- **Microsoft Semantic Kernel**: Plugin and planner architecture, memory management
- **Microsoft AutoGen**: Multi-agent conversation patterns, agent coordination
- **LangChain**: Tool/function abstraction, sequential chains (simplified)
- **LangGraph**: State-based workflows, multi-agent orchestration concepts

**Pattern Convergence**: Despite different implementations, all successful agentic frameworks converge on similar core patterns:
- Composable units (agents/chains/nodes)
- Tool/function abstraction
- State/context management
- Sequential and parallel execution
- Memory for conversation history

Our framework demonstrates these universal patterns in a Clojure-native implementation.

## Architecture Components

### 1. Core Framework (`src/study_llm/agent.clj`)

#### Agent Protocol
```clojure
(defprotocol Agent
  (execute [this input context]))
```

All agents implement this protocol, ensuring consistent interface.

#### Tool System
```clojure
(defn create-tool [name description fn & {:keys [schema]}])
```

Tools are named functions with metadata. Agents use tools to accomplish tasks.

#### Memory Management
```clojure
(defn create-memory [type])
(defn add-to-memory [memory entry])
(defn get-memory [memory & opts])
```

Maintains conversation context and history across agent interactions.

#### Orchestrator
```clojure
(defn create-orchestrator [agents & {:keys [strategy]}])
(defn orchestrate [orchestrator input context])
```

Coordinates multiple agents to accomplish complex tasks. Supports different strategies:
- `:sequential` - Execute agents one after another (current)
- `:parallel` - Execute agents concurrently (future)
- `:dynamic` - LLM decides which agents to use (future)

### 2. Specialized Agents

#### SQL Generator Agent (`src/study_llm/agents/sql_generator.clj`)
- **Purpose**: Convert natural language to SQL
- **Tools**: 
  - `generate-sql` - LLM-based text-to-SQL conversion
- **Configuration**: 
  - Temperature: 0.1 (low for factual, deterministic output)
  - Model: llama2
- **Memory**: Conversation history of SQL generations

#### Database Executor Agent (`src/study_llm/agents/database_executor.clj`)
- **Purpose**: Execute SQL queries safely
- **Tools**:
  - `execute-query` - Run SQL against PostgreSQL
  - `get-schema` - Retrieve database schema
- **Configuration**:
  - Max results: 1000
  - Timeout: 30 seconds
- **Memory**: Query execution history

#### Result Analyzer Agent (`src/study_llm/agents/result_analyzer.clj`)
- **Purpose**: Interpret and explain query results
- **Tools**:
  - `analyze-results` - LLM-based result analysis
- **Configuration**:
  - Temperature: 0.3 (higher for creative insights)
  - Model: llama2
- **Memory**: Analysis history

### 3. Orchestration Flow

```
User Question
      ↓
[Initial Context: schema, question]
      ↓
SQL Generator Agent
  - Input: Question + Schema
  - Tool: generate-sql
  - Output: SQL Query
      ↓
[Updated Context: + generated SQL]
      ↓
Database Executor Agent
  - Input: SQL Query
  - Tool: execute-query
  - Output: Query Results
      ↓
[Updated Context: + query results]
      ↓
Result Analyzer Agent
  - Input: Question + Results
  - Tool: analyze-results
  - Output: Analysis & Insights
      ↓
Final Output to User
```

## Code Examples

### Creating a Custom Agent

```clojure
(ns my-app.agents.custom-agent
  (:require [study-llm.agent :as agent]))

(defn my-tool []
  (agent/create-tool
    :my-tool
    "Description of what this tool does"
    (fn [input context]
      {:status :success
       :result "tool output"
       :updated-context {:new-key "value"}})))

(defn create-my-agent []
  (let [tools {:tool1 (my-tool)}
        memory (agent/create-memory :conversation)]
    (agent/create-llm-agent
      "my-agent"
      "Agent description"
      tools
      :memory memory
      :config {:temperature 0.5})))
```

### Using the Orchestrator

```clojure
;; Create agents
(def agent1 (create-agent-1))
(def agent2 (create-agent-2))

;; Create orchestrator
(def pipeline (agent/create-orchestrator
                [agent1 agent2]
                :strategy :sequential))

;; Execute pipeline
(def result (agent/orchestrate
              pipeline
              "user input"
              {:initial :context}))
```

### Accessing Agent Memory

```clojure
;; Get recent interactions
(agent/get-memory (:memory sql-agent) :limit 5)

;; Get filtered results
(agent/get-memory (:memory sql-agent)
                  :filter-fn #(= (:status %) :success))

;; Clear memory
(agent/clear-memory (:memory sql-agent))
```

## Comparison with Other Frameworks

| Feature | Our Framework | Semantic Kernel | AutoGen | LangChain | LangGraph |
|---------|--------------|-----------------|---------|-----------|-----------|
| Language | Clojure | C#/Python/Java | Python | Python/JS | Python |
| JVM Native | ✅ | ⚠️ (Java only) | ❌ | ❌ | ❌ |
| Multi-Agent | ✅ | ✅ | ✅ | ✅ | ✅ |
| Memory | ✅ | ✅ | ✅ | ✅ | ✅ |
| Planning | 🚧 Future | ✅ | ✅ | ✅ | ✅ |
| Tool Registry | ✅ | ✅ (Plugins) | ✅ | ✅ | ✅ |
| Functional | ✅ | ⚠️ | ❌ | ⚠️ | ⚠️ |
| Dependencies | Low | High | High | Very High | High |
| Learning Curve | Low | Medium | Medium | High | Medium |
| Graph Workflows | 🚧 Future | ⚠️ | ❌ | ❌ | ✅ |
| Persistence | 🚧 Future | ✅ | ⚠️ | ⚠️ | ✅ |
| Cycles/Loops | 🚧 Future | ⚠️ | ⚠️ | ❌ | ✅ |

Legend:
- ✅ Fully supported
- ⚠️ Partial support
- ❌ Not supported
- 🚧 Planned

### Detailed Framework Comparisons

For in-depth comparisons with LangChain and LangGraph, including:
- Architecture pattern mappings
- Code examples side-by-side
- Tool/agent system comparisons
- Orchestration pattern differences
- State management approaches

See **[LANGCHAIN_LANGGRAPH_COMPARISON.md](LANGCHAIN_LANGGRAPH_COMPARISON.md)**

## Performance Considerations

### Benefits of Native Clojure Implementation

1. **No Bridge Overhead**: Direct JVM execution, no Python/C# interop
2. **Immutable Data**: Thread-safe by default, easier parallelization
3. **REPL Development**: Interactive development and debugging
4. **JVM Optimization**: Benefit from JIT compilation
5. **Connection Pooling**: Efficient resource management with HikariCP

### Scalability

The agentic architecture supports:
- **Horizontal Scaling**: Stateless agents can run on multiple instances
- **Parallel Execution**: Future enhancement for concurrent agent execution
- **Caching**: Agent memory can cache common queries/results
- **Load Balancing**: Multiple LLM backends can be used

## Future Enhancements

### Planned Features

1. **Dynamic Planning**
   - LLM-based task decomposition
   - Automatic agent selection based on task

2. **Parallel Orchestration**
   - Execute independent agents concurrently
   - Reduce latency for multi-step tasks

3. **Advanced Memory**
   - Semantic search in conversation history
   - Long-term memory persistence
   - Context summarization

4. **Tool Discovery**
   - Automatic tool registration
   - LLM-driven tool selection

5. **Agent Communication**
   - Direct agent-to-agent messaging
   - Collaborative problem solving

6. **Monitoring & Observability**
   - Agent performance metrics
   - Execution traces
   - Error tracking

## Migration from Previous Architecture

### What Changed

**Before**:
- Direct function calls in `llm.clj`
- Monolithic `process-question` in `chat.clj`
- No explicit agent abstraction

**After**:
- Agent protocol and implementations
- Specialized agent modules
- Orchestrator for coordination
- Memory management
- Tool registry

### Backward Compatibility

The `llm.clj` module is retained for backward compatibility:
- Original functions still work
- Can be used directly without agents
- Gradual migration path for existing code

### Breaking Changes

None. The new agentic framework is additive:
- Old code continues to work
- New code uses agent framework
- Can mix both approaches during transition

## Best Practices

### Agent Design

1. **Single Responsibility**: Each agent should have one clear purpose
2. **Idempotency**: Agents should be side-effect free where possible
3. **Error Handling**: Always return structured error responses
4. **Context Management**: Update context appropriately for next agent
5. **Logging**: Log agent actions for debugging and monitoring

### Tool Creation

1. **Descriptive Names**: Use clear, descriptive tool names
2. **Documentation**: Provide detailed descriptions for LLM planning
3. **Schema Validation**: Define parameter schemas when possible
4. **Error Messages**: Return helpful error messages
5. **Atomicity**: Tools should be atomic operations

### Orchestration

1. **Context Enrichment**: Each agent should add valuable context
2. **Error Propagation**: Handle errors gracefully in pipeline
3. **Memory Management**: Clean up memory when appropriate
4. **Performance**: Monitor orchestration latency
5. **Testing**: Test orchestration pipelines end-to-end

## Testing

### Unit Testing Agents

```clojure
(deftest test-sql-generator-agent
  (let [agent (sql-gen/create-sql-generator-agent)
        context {:schema [...]}
        result (agent/execute agent "How many customers?" context)]
    (is (= :success (:status result)))
    (is (string? (:result result)))))
```

### Integration Testing

```clojure
(deftest test-full-pipeline
  (let [orchestrator (create-orchestrator [...])
        result (agent/orchestrate orchestrator "test query" {...})]
    (is (= :success (:status result)))
    (is (= 3 (count (:results result))))))
```

### Memory Testing

```clojure
(deftest test-agent-memory
  (let [memory (agent/create-memory :conversation)
        _ (agent/add-to-memory memory {:test "data"})
        history (agent/get-memory memory)]
    (is (= 1 (count history)))))
```

## Conclusion

This Clojure-native agentic framework provides:
- ✅ **Clear Architecture**: Well-defined agent responsibilities
- ✅ **Flexibility**: Easy to add/modify agents
- ✅ **Performance**: Native JVM execution
- ✅ **Maintainability**: Modular, testable code
- ✅ **Educational Value**: Learn agentic principles
- ✅ **Production-Ready**: Suitable for real applications

The framework demonstrates that you don't need heavyweight external frameworks to build sophisticated multi-agent AI systems. By leveraging Clojure's strengths (functional programming, immutability, JVM performance), we've created a clean, efficient, and maintainable agentic architecture.

## References

### Primary Frameworks
- [Microsoft Semantic Kernel](https://github.com/microsoft/semantic-kernel) - Plugin and planner architecture
- [Microsoft AutoGen](https://github.com/microsoft/autogen) - Multi-agent conversations
- [LangChain](https://github.com/langchain-ai/langchain) - Tool abstraction and chains
- [LangGraph](https://github.com/langchain-ai/langgraph) - Graph-based multi-agent workflows

### Documentation
- [Semantic Kernel Overview](https://learn.microsoft.com/en-us/semantic-kernel/overview/)
- [AutoGen Research](https://www.microsoft.com/en-us/research/project/autogen/)
- [LangChain Documentation](https://python.langchain.com/docs/)
- [LangGraph Documentation](https://langchain-ai.github.io/langgraph/)

### Related Documents
- [LANGCHAIN_LANGGRAPH_COMPARISON.md](LANGCHAIN_LANGGRAPH_COMPARISON.md) - Detailed comparison with LangChain and LangGraph
- [REFACTORING_DECISIONS.md](REFACTORING_DECISIONS.md) - Framework selection rationale
- [README.md](README.md) - Project overview and usage
