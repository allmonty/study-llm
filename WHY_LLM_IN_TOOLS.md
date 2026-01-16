# Agent and Tool Architecture: Corrected Understanding

## Executive Summary

This document clarifies the **correct** architectural pattern for agents and tools in agentic frameworks:

**The Controller**: The **Agent** (via the LLM) is the only entity that reasons and issues tool calls.

**The Executor**: **Tools** are passive execution units. They do not contain their own LLM calls unless that tool is explicitly acting as a wrapper for a **Sub-Agent**.

This correction addresses a previous misunderstanding about how other frameworks (like Microsoft Semantic Kernel, LangChain, and LangGraph) structure their architectures.

## The Corrected Architecture Pattern

### Standard Agentic Pattern

```
Agent (Controller)
  ├─> Reasoning via LLM
  └─> Issues Tool Calls
       └─> Tool (Executor) - Passive execution
```

### Exception: Tools Wrapping Sub-Agents

```
Agent (Controller)
  └─> Issues Tool Call
       └─> Tool
            └─> Sub-Agent (Controller)
                 ├─> Reasoning via LLM
                 └─> Issues Tool Calls
                      └─> Tool (Executor)
```

### Agent Execution Modes

Our framework supports two agent execution modes:

1. **Autonomous Mode**: Agent uses LLM to reason about which tool to call based on context
   - Agent makes intelligent decisions
   - LLM-based tool selection (`:tool-selection-strategy :llm`)
   
2. **Sequential Mode**: Agent follows predefined tool invocation sequence
   - Deterministic, hard-coded tool selection
   - Direct tool invocation (`:tool-selection-strategy :primary`)

### Current Implementation Note

Our current implementation has a **transitional architecture** where some domain-specific LLM calls exist within tools (e.g., text-to-SQL generation, result analysis). This is a **pragmatic pattern** for domain-specific transformations but differs from the standard agentic pattern where:
- The agent's LLM handles reasoning and tool selection
- Tools are passive executors
- Domain-specific LLM calls should ideally be wrapped as Sub-Agents

## Why This Corrected Understanding Matters

### 1. **Clear Separation of Concerns**

**Agents** are responsible for:
- **Reasoning** via LLM
- **Decision-making** about which tools to invoke
- **Context management** across tool calls
- **Orchestration** of multi-step workflows
- Error handling and recovery

**Tools** are responsible for:
- **Passive execution** of specific tasks
- Database queries
- File operations
- API calls
- Data transformations
- OR wrapping a Sub-Agent (hierarchical composition)

This separation creates a clear architectural boundary.

### 2. **Hierarchical Agent Composition**

Agents can be composed hierarchically:

```clojure
;; Parent Agent
(def parent-agent 
  (create-agent 
    :tools {:sub-task (create-sub-agent-tool child-agent)}))

;; Child Agent wrapped in a tool
(defn create-sub-agent-tool [sub-agent]
  (create-tool 
    :sub-agent-wrapper
    "Delegates to a specialized sub-agent"
    (fn [input context]
      ;; Tool invokes another agent
      (execute sub-agent input context))))
```

This enables:
- Building complex agent hierarchies
- Delegating specialized tasks to sub-agents
- Composable agent architectures
- Separation of concerns at multiple levels

### 3. **Agent Execution Modes**

Agents can operate in different modes:

**Autonomous Mode** (LLM-driven):
```clojure
(create-llm-agent 
  "intelligent-agent"
  "Makes decisions about tool usage"
  tools
  :config {:tool-selection-strategy :llm})
```

**Sequential Mode** (Hard-coded):
```clojure
(create-llm-agent 
  "deterministic-agent"
  "Follows predefined tool sequence"
  tools
  :config {:tool-selection-strategy :primary
           :primary-tool :specific-tool})
```

This flexibility allows:
- Testing with deterministic tool selection
- Production use with intelligent tool selection
- Hybrid approaches for different use cases

### 4. **Testability**

```clojure
;; Test the tool independently
(deftest test-sql-generation-tool
  (let [tool (generate-sql-tool)
        result (invoke-tool tool "How many customers?" context)]
    (is (= :success (:status result)))))

;; Test the agent with mocked tools
(deftest test-agent-with-mock
  (let [mock-tool (create-tool :mock "Mock" (fn [_ _] {:status :success}))
        agent (create-llm-agent "test" "Test" {:mock mock-tool})]
    ...))
```

### 4. **Corrected Framework Understanding**

#### Microsoft Semantic Kernel - Corrected Understanding
In Semantic Kernel:
- **Plugins** (tools) are passive functions
- The **Kernel/Planner** (agent) uses LLM to reason and select plugins
- Plugins do NOT contain LLM calls (unless wrapping a sub-agent)

```csharp
// Agent reasons and selects plugin
var planner = new SequentialPlanner(kernel);
var plan = await planner.CreatePlanAsync(goal); // LLM reasoning here
await plan.InvokeAsync(); // Plugins execute passively
```

#### LangChain - Corrected Understanding
In LangChain:
- **Tools** are passive functions
- The **Agent** uses LLM to reason and select tools
- Tools do NOT contain LLM calls (unless they are chains/sub-agents)

```python
# Agent with LLM reasoning
agent = initialize_agent(
    tools=[calculator_tool],  # Passive tools
    llm=llm,  # Agent's LLM for reasoning
    agent=AgentType.ZERO_SHOT_REACT_DESCRIPTION
)
agent.run("What is 25 * 4?")  # Agent reasons, selects tools
```

#### LangGraph - Corrected Understanding
In LangGraph:
- **Nodes** can be passive functions OR agents
- The **Graph** defines workflow, not necessarily LLM reasoning at each node
- Conditional edges use LLM to route between nodes

```python
# Node is typically passive
def calculator_node(state):
    return {"result": state["x"] + state["y"]}

# Agent/LLM reasoning happens in routing
graph.add_conditional_edges(
    "agent",
    lambda x: llm.predict(x)  # LLM reasoning for routing
)
```

## Recommended Patterns for This Framework

### ✅ Pattern 1: Agent Reasons, Tools Execute (Standard)

```clojure
;; GOOD: Agent uses LLM for reasoning and tool selection
(defn create-intelligent-agent []
  (create-llm-agent
    "reasoning-agent"
    "Uses LLM to reason and select appropriate tools"
    {:database (create-db-tool)
     :calculator (create-calc-tool)
     :search (create-search-tool)}
    :config {:tool-selection-strategy :llm}))

;; Tools are passive executors
(defn create-db-tool []
  (create-tool :database "Execute database query"
    (fn [query context]
      {:status :success
       :result (execute-db-query query)})))
```

**Benefits**:
- Clear separation: Agent reasons, Tools execute
- Agent's LLM handles intelligence
- Tools are simple, testable, passive functions

### ✅ Pattern 2: Hierarchical Agents (Sub-Agent in Tool)

```clojure
;; GOOD: Tool wraps a sub-agent for specialized reasoning
(defn create-sql-expert-tool [sql-expert-agent]
  (create-tool :sql-expert "Specialized SQL generation"
    (fn [question context]
      ;; Delegate to sub-agent which has its own LLM reasoning
      (execute sql-expert-agent question context))))

(defn create-parent-agent []
  (let [sql-expert (create-sql-expert-agent)  ; Sub-agent
        tools {:sql-expert (create-sql-expert-tool sql-expert)
               :database (create-db-tool)}]
    (create-llm-agent "parent" "Coordinates sub-agents" tools
      :config {:tool-selection-strategy :llm})))
```

**Benefits**:
- Hierarchical composition of agents
- Specialized sub-agents for complex tasks
- Clear responsibility boundaries
- Scalable architecture

### ✅ Pattern 3: Sequential Mode (Deterministic)

```clojure
;; GOOD: Agent follows predefined sequence, no LLM for tool selection
(defn create-sequential-agent []
  (create-llm-agent
    "sequential-agent"
    "Follows deterministic tool sequence"
    {:primary-tool (create-primary-tool)}
    :config {:tool-selection-strategy :primary
             :primary-tool :primary-tool}))
```

**Benefits**:
- Predictable, testable behavior
- Lower latency (no LLM call for tool selection)
- Suitable for well-defined workflows

## Transitional Pattern in Current Implementation

### ⚠️ Current Pragmatic Approach

Our current implementation uses a **transitional pattern** where domain-specific LLM calls exist within tools:

```clojure
;; TRANSITIONAL: Domain-specific LLM in tool
(defn generate-sql-tool []
  (create-tool :generate-sql "Generate SQL"
    (fn [question context]
      ;; Domain-specific LLM call for text-to-SQL
      (llm/generate-completion prompt :temperature 0.1))))
```

**Why this exists:**
- Pragmatic solution for domain-specific transformations
- Simpler than creating sub-agents for every LLM call
- Works well for single-purpose transformations

**Better approach (recommended for evolution):**
```clojure
;; RECOMMENDED: Wrap as sub-agent
(defn create-sql-generator-sub-agent []
  (create-llm-agent "sql-generator-sub" "Generates SQL"
    {:generate (create-prompt-tool)}
    :config {:temperature 0.1}))

(defn create-sql-expert-tool [sql-agent]
  (create-tool :sql-expert "SQL generation sub-agent"
    (fn [question context]
      (execute sql-agent question context))))
```

This refactoring provides:
- Cleaner separation of concerns
- Better testability
- More consistent architecture
- Easier to extend with multiple tools per sub-agent

## Architecture Summary

### The Standard Pattern (Recommended)

```
┌─────────────────────────────────────────┐
│           Agent (Controller)            │
│  • LLM Reasoning                        │
│  • Tool Selection via LLM               │
│  • Context Management                   │
└─────────────┬───────────────────────────┘
              │
              ▼
    ┌─────────────────────┐
    │   Tools (Executors) │
    │   • Passive         │
    │   • No LLM calls    │
    └─────────────────────┘
```

### Hierarchical Pattern (For Complex Domains)

```
┌─────────────────────────────────────────┐
│      Parent Agent (Controller)          │
│  • LLM Reasoning                        │
│  • Tool Selection via LLM               │
└─────────────┬───────────────────────────┘
              │
              ▼
    ┌─────────────────────┐
    │   Tool (Wrapper)    │
    │   • Wraps Sub-Agent │
    └─────────┬───────────┘
              │
              ▼
    ┌─────────────────────┐
    │ Sub-Agent           │
    │ • LLM Reasoning     │
    │ • Tool Selection    │
    └─────────┬───────────┘
              │
              ▼
    ┌─────────────────────┐
    │ Sub-Tools           │
    │ • Passive Executors │
    └─────────────────────┘
```

### Execution Mode Flexibility

**Autonomous Mode:**
- Agent uses LLM to reason about tool selection
- Dynamic, intelligent decision-making
- Best for complex, varied inputs

**Sequential Mode:**
- Agent follows predefined tool sequence
- Deterministic, predictable
- Best for well-defined workflows

## Key Architectural Principles

### 1. Agent as Controller
- **Reasoning**: Agent uses LLM to understand input and context
- **Decision-Making**: Agent selects which tool to invoke
- **Orchestration**: Agent manages multi-step workflows
- **Context Management**: Agent maintains state across operations

### 2. Tools as Executors
- **Passive Execution**: Tools perform specific tasks without reasoning
- **No LLM Calls**: Tools should not make LLM calls (unless wrapping Sub-Agent)
- **Single Responsibility**: Each tool does one thing well
- **Stateless**: Tools don't maintain state

### 3. Hierarchical Composition
- **Sub-Agents in Tools**: Complex domains can wrap agents in tools
- **Recursive Structure**: Sub-agents follow same pattern
- **Clear Boundaries**: Each level has well-defined responsibilities
- **Scalability**: Easy to add new levels of abstraction

### 4. Execution Mode Flexibility
- **Autonomous**: LLM-driven tool selection for intelligence
- **Sequential**: Hard-coded tool selection for determinism
- **Hybrid**: Mix both approaches based on use case

## Framework Comparisons - Corrected

### How Other Frameworks Actually Work

#### Microsoft Semantic Kernel
**Corrected Pattern**: Agent/Planner uses LLM for reasoning, Plugins are passive

```csharp
// The Planner (Agent) uses LLM to reason
var planner = new SequentialPlanner(kernel);
var plan = await planner.CreatePlanAsync("Send an email");

// Plugins (Tools) are passive - no LLM calls
public class EmailPlugin
{
    [SKFunction("Send an email")]
    public async Task<string> SendEmail(string recipient, string message)
    {
        // Passive execution - no LLM call
        return await emailService.Send(recipient, message);
    }
}
```

#### LangChain
**Corrected Pattern**: Agent uses LLM for reasoning and tool selection

```python
from langchain.agents import initialize_agent, Tool
from langchain.llms import OpenAI

# Tools are passive functions
def calculator(query: str) -> str:
    # No LLM call - just execute
    return str(eval(query))

calculator_tool = Tool(
    name="Calculator",
    func=calculator,  # Passive function
    description="Useful for math calculations"
)

# Agent uses LLM for reasoning
llm = OpenAI()
agent = initialize_agent(
    tools=[calculator_tool],
    llm=llm,  # Agent's LLM
    agent=AgentType.ZERO_SHOT_REACT_DESCRIPTION
)

# Agent reasons, selects tools, executes them
result = agent.run("What is 25 * 4 + 10?")
```

#### LangGraph
**Corrected Pattern**: Nodes can be passive OR agents, edges use LLM for routing

```python
from langgraph.graph import StateGraph

# Passive node - no LLM
def tool_node(state):
    return {"result": perform_calculation(state["input"])}

# Agent node - has LLM
def agent_node(state):
    # Agent uses LLM to reason
    action = llm.predict(state["input"])
    return {"action": action}

# Graph with conditional routing via LLM
graph = StateGraph()
graph.add_node("agent", agent_node)
graph.add_node("tool", tool_node)
graph.add_conditional_edges(
    "agent",
    lambda x: llm.route(x)  # LLM decides routing
)
```

## Conclusion

The corrected architectural understanding is:

1. **Agent as Controller** - Agent uses LLM to reason and make decisions
2. **Tools as Executors** - Tools are passive, executing specific tasks
3. **Exception for Sub-Agents** - Tools CAN wrap sub-agents for hierarchical composition
4. **Execution Mode Flexibility** - Agents can be Autonomous (LLM-driven) or Sequential (deterministic)

### The Corrected Key Principle

> **Agents reason with LLM, Tools execute passively (unless wrapping Sub-Agents).**

- **Agents** use LLM to decide *what* to do and *which tool* to use
- **Tools** execute the *how* - passive operations without reasoning
- **Sub-Agents** can be wrapped in tools for hierarchical composition

This separation is fundamental to building scalable, maintainable agentic systems.

### Current Implementation Status

Our framework currently uses a **transitional pattern** where some domain-specific LLM calls exist in tools (e.g., text-to-SQL). This is pragmatic but differs from the ideal:

**Current (Transitional)**:
```clojure
;; Tool contains domain-specific LLM call
(defn generate-sql-tool []
  (create-tool :generate-sql "Generate SQL"
    (fn [question context]
      (llm/generate-completion prompt :temperature 0.1))))
```

**Recommended (Standard Pattern)**:
```clojure
;; Sub-agent for domain-specific reasoning
(def sql-sub-agent 
  (create-llm-agent "sql-expert" "SQL reasoning"
    {:format-prompt (create-prompt-tool)}))

;; Tool wraps sub-agent
(defn sql-expert-tool [sql-agent]
  (create-tool :sql-expert "SQL generation"
    (fn [question context]
      (execute sql-agent question context))))
```

### Migration Path

To fully align with standard agentic architecture:

1. ✅ **Already Implemented**: Agent-level LLM for tool selection (`:tool-selection-strategy :llm`)
2. ✅ **Already Implemented**: Support for Autonomous vs Sequential modes
3. 🔄 **Transitional**: Domain-specific LLM calls in tools
4. 🎯 **Future**: Migrate domain-specific LLM calls to Sub-Agents wrapped in tools

This provides a clear path from our pragmatic current state to the ideal standard architecture.

## References

- [Microsoft Semantic Kernel Documentation](https://learn.microsoft.com/en-us/semantic-kernel/)
- [LangChain Agent Documentation](https://python.langchain.com/docs/modules/agents/)
- [LangGraph Documentation](https://langchain-ai.github.io/langgraph/)
- [AGENTIC_FRAMEWORK.md](./AGENTIC_FRAMEWORK.md) - Our framework documentation
- [REFACTORING_DECISIONS.md](./REFACTORING_DECISIONS.md) - Architecture decisions

---

**Summary**: Agents use LLM to reason and select tools. Tools are passive executors unless wrapping Sub-Agents. This corrected understanding aligns with standard agentic frameworks like Microsoft Semantic Kernel, LangChain, and LangGraph.
