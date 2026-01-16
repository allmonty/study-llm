# Architecture Design: Agent Execution Modes and Hierarchical Composition

## Overview

This document explains how to effectively model agent relationships in our agentic framework, with support for:

1. **Execution Modes**: Agents can toggle between 'Autonomous' (LLM-driven) and 'Sequential' (hard-coded)
2. **Hierarchical Composition**: Tools can encapsulate Sub-Agents, allowing for a tree of agents

## Core Architecture Principles

### The Controller-Executor Pattern

**The Controller**: The **Agent** (via the LLM) is the only entity that reasons and issues tool calls.

**The Executor**: **Tools** are passive execution units. They do NOT contain their own LLM calls unless that tool is explicitly acting as a wrapper for a **Sub-Agent**.

This corrects the previous understanding and aligns with standard agentic frameworks:
- Microsoft Semantic Kernel: Planner/Kernel reasons, Plugins execute
- LangChain: Agent reasons and selects, Tools execute
- LangGraph: Conditional edges use LLM for routing, Nodes typically execute

## Agent Execution Modes

### 1. Autonomous Mode

**Definition**: Agent uses LLM to reason about which tool to invoke based on the input and context.

**When to Use**:
- Complex, varied user inputs
- Multiple tools with different purposes
- Need for intelligent, adaptive decision-making
- User-facing applications with natural language input

**Configuration**:
```clojure
(create-llm-agent 
  "intelligent-agent"
  "Makes smart decisions about tool usage"
  {:tool1 (create-tool-1)
   :tool2 (create-tool-2)
   :tool3 (create-tool-3)}
  :config {:execution-mode :autonomous})
```

**How It Works**:
1. Agent receives input and context
2. Agent's LLM analyzes available tools and their descriptions
3. LLM selects the most appropriate tool
4. Agent invokes selected tool
5. Tool executes and returns result

**Example Flow**:
```
User: "Summarize this document"
  ↓
Agent LLM: Analyzes input → Selects :summarize tool
  ↓
Summarize Tool: Executes (passive) → Returns summary
  ↓
Agent: Returns result to user
```

### 2. Sequential Mode

**Definition**: Agent follows a predetermined tool invocation sequence, no LLM reasoning for tool selection.

**When to Use**:
- Well-defined, predictable workflows
- Single-purpose agents
- Testing and debugging
- Performance-critical paths requiring lower latency
- Deterministic behavior is required

**Configuration**:
```clojure
(create-llm-agent 
  "deterministic-agent"
  "Follows predefined tool sequence"
  {:primary (create-primary-tool)}
  :config {:execution-mode :sequential
           :primary-tool :primary})
```

**How It Works**:
1. Agent receives input and context
2. Agent directly invokes the configured primary tool
3. Tool executes and returns result
4. No LLM call for tool selection (faster, deterministic)

**Example Flow**:
```
User: "Process this data"
  ↓
Agent: Invokes primary tool (no LLM reasoning)
  ↓
Primary Tool: Executes → Returns result
  ↓
Agent: Returns result to user
```

### Comparison Table

| Aspect | Autonomous Mode | Sequential Mode |
|--------|----------------|----------------|
| Tool Selection | LLM-driven | Hard-coded |
| Latency | Higher (LLM call) | Lower (direct) |
| Flexibility | High (adaptive) | Low (fixed) |
| Determinism | Lower (LLM varies) | High (consistent) |
| Use Case | Varied inputs | Predictable workflows |
| Configuration | `:execution-mode :autonomous` | `:execution-mode :sequential` |
| Strategy | `:tool-selection-strategy :llm` | `:tool-selection-strategy :primary` |

## Hierarchical Agent Composition

### Concept

Tools can wrap Sub-Agents, creating a hierarchical tree structure where:
- Parent Agents coordinate high-level workflows
- Sub-Agents handle specialized domain reasoning
- Each level maintains its own execution mode
- Clear separation of concerns at each level

### Architecture Pattern

```
Parent Agent (Level 0)
  ├─> Tool A (wraps Sub-Agent)
  │    └─> Sub-Agent A (Level 1)
  │         ├─> Tool A1 (passive)
  │         └─> Tool A2 (passive)
  ├─> Tool B (wraps Sub-Agent)
  │    └─> Sub-Agent B (Level 1)
  │         ├─> Tool B1 (passive)
  │         ├─> Tool B2 (passive)
  │         └─> Tool B3 (wraps Sub-Sub-Agent)
  │              └─> Sub-Sub-Agent (Level 2)
  │                   └─> Tools...
  └─> Tool C (passive, no sub-agent)
```

### Implementation

#### Step 1: Create Leaf Tools (Passive Executors)

```clojure
(defn create-format-tool []
  (create-tool :format "Formats data"
    (fn [data context]
      {:status :success
       :result (format-data data)
       :updated-context {:formatted true}})))

(defn create-validate-tool []
  (create-tool :validate "Validates data"
    (fn [data context]
      {:status :success
       :result (validate-data data)
       :updated-context {:validated true}})))
```

#### Step 2: Create Sub-Agents

```clojure
(defn create-data-processor-agent []
  (create-llm-agent
    "data-processor"
    "Processes data with formatting and validation"
    {:format (create-format-tool)
     :validate (create-validate-tool)}
    :config {:execution-mode :sequential
             :primary-tool :format}))
```

#### Step 3: Wrap Sub-Agents in Tools

```clojure
(defn create-processor-tool [processor-agent]
  (create-sub-agent-tool
    :data-processing
    "Delegates to data processing sub-agent"
    processor-agent))
```

#### Step 4: Create Parent Agent

```clojure
(defn create-coordinator-agent []
  (let [processor (create-data-processor-agent)
        tools {:processing (create-processor-tool processor)
               :logging (create-logging-tool)}]
    (create-llm-agent
      "coordinator"
      "Coordinates data processing workflow"
      tools
      :config {:execution-mode :autonomous})))
```

### Execution Flow Example

```
User Input: "Process and validate this data"
  ↓
Parent Agent (Autonomous):
  - LLM analyzes input
  - Selects :processing tool
  ↓
Processing Tool (Sub-Agent Wrapper):
  - Delegates to Data Processor Sub-Agent
  ↓
Data Processor Sub-Agent (Sequential):
  - Uses primary tool (:format)
  - No LLM reasoning needed
  ↓
Format Tool (Passive):
  - Executes formatting logic
  - Returns formatted data
  ↓
Results bubble back up to user
```

## Mixing Execution Modes

### Pattern 1: Autonomous Parent, Sequential Sub-Agents

**Use Case**: Intelligent routing to specialized, deterministic sub-agents

```clojure
(def coordinator 
  (create-llm-agent "coordinator" "Smart router"
    {:text-processing (create-sub-agent-tool 
                        :text-processing 
                        "Text processing" 
                        (create-text-agent :execution-mode :sequential))
     :data-analysis (create-sub-agent-tool 
                      :data-analysis 
                      "Data analysis" 
                      (create-data-agent :execution-mode :sequential))}
    :config {:execution-mode :autonomous}))
```

**Benefits**:
- Parent intelligently routes to the right domain
- Sub-agents execute deterministically (faster)
- Best of both worlds: smart routing + fast execution

### Pattern 2: Sequential Parent, Autonomous Sub-Agents

**Use Case**: Fixed workflow with intelligent sub-tasks

```clojure
(def pipeline
  (create-llm-agent "pipeline" "Fixed pipeline"
    {:processor (create-sub-agent-tool
                  :processor
                  "Main processor"
                  (create-processor-agent :execution-mode :autonomous))}
    :config {:execution-mode :sequential
             :primary-tool :processor}))
```

**Benefits**:
- Predictable top-level flow
- Sub-agents can make intelligent decisions within their domain
- Good for testing/debugging while keeping flexibility

### Pattern 3: Fully Autonomous

**Use Case**: Maximum flexibility and intelligence

```clojure
(def smart-system
  (create-llm-agent "smart-system" "Fully intelligent"
    {:sub-agent-1 (create-sub-agent-tool 
                    :sa1 "Sub 1" 
                    (create-agent-1 :execution-mode :autonomous))
     :sub-agent-2 (create-sub-agent-tool 
                    :sa2 "Sub 2" 
                    (create-agent-2 :execution-mode :autonomous))}
    :config {:execution-mode :autonomous}))
```

**Benefits**:
- Maximum adaptability at all levels
- Best for complex, unpredictable scenarios
- Higher latency due to multiple LLM calls

### Pattern 4: Fully Sequential

**Use Case**: Maximum performance and determinism

```clojure
(def fast-pipeline
  (create-llm-agent "fast-pipeline" "Deterministic"
    {:processor (create-sub-agent-tool
                  :proc "Processor"
                  (create-processor :execution-mode :sequential))}
    :config {:execution-mode :sequential
             :primary-tool :processor}))
```

**Benefits**:
- Lowest latency (no LLM calls for routing)
- Completely predictable behavior
- Best for production pipelines with known workflows

## Best Practices

### 1. Start Sequential, Add Autonomy Where Needed

```clojure
;; Start with sequential for clarity
(def agent-v1 (create-agent :execution-mode :sequential))

;; Test and validate behavior

;; Add autonomy for complex cases
(def agent-v2 (create-agent :execution-mode :autonomous))
```

### 2. Use Hierarchical Composition for Domain Separation

```clojure
;; Good: Each domain has its own sub-agent
(def system
  {:nlp-tasks (create-nlp-agent)
   :data-tasks (create-data-agent)
   :db-tasks (create-db-agent)})

;; Bad: One agent tries to do everything
(def monolith (create-mega-agent-with-50-tools))
```

### 3. Match Execution Mode to Use Case

| Use Case | Recommended Mode | Reason |
|----------|-----------------|---------|
| User chatbot | Autonomous | Varied, unpredictable input |
| Data pipeline | Sequential | Fixed, predictable workflow |
| Development/Testing | Sequential | Deterministic for debugging |
| Production critical path | Sequential | Lower latency |
| Experimental features | Autonomous | Need flexibility |

### 4. Document Your Agent Hierarchy

```clojure
(comment
  "Agent Hierarchy:
   
   Coordinator (Autonomous)
     ├─> Text Processor (Sequential)
     │    ├─> Summarize Tool
     │    └─> Translate Tool
     ├─> Data Analyzer (Autonomous)
     │    ├─> Statistics Tool
     │    └─> Insights Tool
     └─> Logger Tool (passive)
   
   Coordinator intelligently routes to sub-agents.
   Text Processor always uses summarize first (sequential).
   Data Analyzer chooses between stats/insights based on data (autonomous).")
```

### 5. Test Each Level Independently

```clojure
;; Test sub-agent in isolation
(def text-processor (create-text-processor-agent :execution-mode :sequential))
(execute text-processor "test input" {})

;; Test parent agent
(def coordinator (create-coordinator-agent))
(execute coordinator "test input" {})
```

## Common Patterns

### Pattern: Domain Expert Sub-Agents

```clojure
;; Each domain expert is a sub-agent
(defn create-expert-system []
  (let [sql-expert (create-sql-expert-agent :execution-mode :autonomous)
        data-expert (create-data-expert-agent :execution-mode :autonomous)
        viz-expert (create-viz-expert-agent :execution-mode :sequential)]
    (create-llm-agent
      "expert-system"
      "Routes to domain experts"
      {:sql (create-sub-agent-tool :sql "SQL expert" sql-expert)
       :data (create-sub-agent-tool :data "Data expert" data-expert)
       :viz (create-sub-agent-tool :viz "Viz expert" viz-expert)}
      :config {:execution-mode :autonomous})))
```

### Pattern: Pipeline with Checkpoints

```clojure
;; Each pipeline stage is a sub-agent
(defn create-pipeline []
  (let [ingestion (create-ingestion-agent :execution-mode :sequential)
        validation (create-validation-agent :execution-mode :autonomous)
        processing (create-processing-agent :execution-mode :sequential)]
    (create-orchestrator
      [ingestion validation processing]
      :strategy :sequential)))
```

### Pattern: Fallback Chain

```clojure
;; Try specialized agent first, fall back to general
(defn create-smart-agent []
  (create-llm-agent
    "smart-agent"
    "Tries specialized agents with fallback"
    {:specialized (create-sub-agent-tool 
                    :spec "Specialized" 
                    (create-specialized-agent))
     :general (create-general-tool)}
    :config {:execution-mode :autonomous}))
```

## Implementation Examples

See the complete working examples in:
- `examples/hierarchical_agents.clj` - Full demonstration of hierarchical composition
- `examples/multi_tool_agent.clj` - Multi-tool selection examples

## Summary

The framework effectively models agent relationships through:

1. **Clear Role Separation**:
   - Agents: Controllers that reason (via LLM)
   - Tools: Executors that perform tasks (or wrap sub-agents)

2. **Execution Mode Flexibility**:
   - Autonomous: LLM-driven intelligence
   - Sequential: Deterministic performance

3. **Hierarchical Composition**:
   - Parent agents coordinate workflows
   - Sub-agents handle domain-specific reasoning
   - Tools provide passive execution or sub-agent delegation

4. **Practical Patterns**:
   - Mix modes based on requirements
   - Separate concerns by domain
   - Test each level independently
   - Document the hierarchy

This architecture provides a clean, scalable way to build complex multi-agent systems while maintaining clarity and performance.
