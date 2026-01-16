# Examples Directory

This directory contains examples demonstrating various capabilities of the agentic framework.

## Available Examples

### 1. Multi-Tool Agent Selection (`multi_tool_agent.clj`)

**Demonstrates**: How an agent can intelligently choose between multiple tools based on input and context.

**Key Concepts**:
- Agents with multiple tools
- Tool selection strategies (LLM-based, custom functions, primary)
- Intelligent agent behavior
- Autonomous vs Sequential execution modes

### 2. Hierarchical Agent Composition (`hierarchical_agents.clj`)

**Demonstrates**: How to build hierarchical agent architectures where tools can wrap sub-agents.

**Key Concepts**:
- Sub-agents wrapped in tools
- Multi-level agent hierarchies
- Parent agents coordinating sub-agents
- Execution mode flexibility at each level
- Mixing Autonomous and Sequential modes

**What You'll Learn**:
- Tools CAN contain agents (sub-agents) for complex reasoning
- Parent agents delegate to specialized sub-agents
- Each level can have its own execution mode (Autonomous/Sequential)
- Clear separation: Agents reason, Tools execute (or delegate to sub-agents)

**Run the Example**:
```bash
# From the project root
clj -M -m hierarchical-agents

# Or in REPL
clj
(require '[hierarchical-agents :as demo])
(demo/-main)
```

**Example Architecture**:
```
Parent Agent (Coordinator)
  ├─> Text Processing Tool
  │    └─> Text Processor Sub-Agent (Autonomous or Sequential)
  │         ├─> Summarize Tool (passive)
  │         └─> Translate Tool (passive)
  ├─> Data Analysis Tool
  │    └─> Data Analyzer Sub-Agent (Autonomous or Sequential)
  │         ├─> Statistics Tool (passive)
  │         └─> Insights Tool (passive)
  └─> Logger Tool (passive, no sub-agent)
```

**What You'll Learn**:
- Yes, agents CAN decide between multiple tools!
- The "intelligence" comes from selection strategies, not just the tools themselves
- Three selection strategies are available:
  - `:llm` - Uses LLM to intelligently choose based on natural language understanding
  - `:function` - Uses a custom function to select the appropriate tool
  - `:primary` - Always uses a configured primary tool (default)

**Run the Example**:
```bash
# From the project root
clj -M -m multi-tool-agent

# Or in REPL
clj
(require '[multi-tool-agent :as demo])
(demo/-main)
```

**Example Agents Included**:

1. **Math Agent** - Performs different math operations (add, multiply, divide, power)
   - Uses LLM to understand natural language and select the right operation
   - Example: "multiply 5 by 6" → LLM selects the multiply tool
   - Example: "what is 3 plus 7?" → LLM understands and selects the add tool

2. **Data Processor Agent** - Processes data using different strategies (filter, map, reduce, sort)
   - Uses a custom selection function that looks at both input and context
   - Example: If context has `:predicate`, automatically uses filter tool

## Understanding Agent Tool Selection

### Corrected Architecture Understanding

**The Controller**: The **Agent** (via the LLM) is the only entity that reasons and issues tool calls.

**The Executor**: **Tools** are passive execution units. They do NOT contain their own LLM calls unless that tool is explicitly acting as a wrapper for a **Sub-Agent**.

This corrected understanding aligns with standard agentic frameworks like Microsoft Semantic Kernel, LangChain, and LangGraph.

### The Question: "Can an agent decide between multiple tools?"

**Answer: YES!** 

In the agentic framework, agents can have multiple tools and intelligently select which one to use based on:

1. **LLM Understanding**: The agent uses LLM to understand natural language input and choose the best tool
2. **Context Analysis**: The agent examines the context to determine which tool is appropriate
3. **Custom Logic**: You can provide a custom function that implements any selection logic you want

### Where Does the "Intelligence" Come From?

The intelligence is in the **agent's reasoning mechanism**, not in the tools:

- **Agent** = Uses LLM to reason about which tool to invoke
- **Tools** = Passive executors OR wrappers for Sub-Agents
- **Sub-Agents** = Specialized agents with their own LLM reasoning
- **Selection Strategy** = How the agent decides (LLM, custom function, or primary)

**Two Levels of LLM Usage:**
1. **Agent-level LLM**: For reasoning and tool selection (controller)
2. **Sub-Agent LLM**: For domain-specific reasoning when wrapped in tools (specialized controller)

### Example: Math Agent

```clojure
;; Create a math agent with 4 tools
(def math-agent (create-math-agent))

;; The agent intelligently selects the right tool using LLM:
(execute math-agent "add 5 and 3" {:numbers [5 3]})
;; → LLM analyzes input, selects :add tool

(execute math-agent "multiply 4 by 7" {:numbers [4 7]})
;; → LLM understands "multiply", selects :multiply tool

(execute math-agent "what is 100 divided by 5?" {:numbers [100 5]})
;; → LLM understands natural language, selects :divide tool
```

The agent uses the LLM to understand the user's intent and selects the appropriate tool.

## How to Create Your Own Multi-Tool Agent

### Step 1: Create Multiple Tools

```clojure
(def tool1 (agent/create-tool 
             :tool1 
             "description with keywords"
             (fn [input context] {...})))

(def tool2 (agent/create-tool 
             :tool2 
             "another description"
             (fn [input context] {...})))
```

### Step 2: Create Agent with Tool Selection Strategy

```clojure
;; Using LLM-based selection
(agent/create-llm-agent
  "my-agent"
  "Agent description"
  {:tool1 tool1
   :tool2 tool2}
  :config {:tool-selection-strategy :llm
           :primary-tool :tool1})

;; Using custom function
(agent/create-llm-agent
  "my-agent"
  "Agent description"
  {:tool1 tool1
   :tool2 tool2}
  :config {:tool-selection-strategy :function
           :tool-selector-fn my-custom-selector})
```

### Step 3: Execute and Let the Agent Choose

```clojure
(agent/execute my-agent "some input" {:context :data})
;; The agent will automatically select the right tool!
```

## Best Practices

### Choosing Execution Modes

**Autonomous Mode** (`:execution-mode :autonomous`):
- Use when: Input is varied and unpredictable
- Benefits: Intelligent, adaptive decision-making
- Trade-off: Higher latency (LLM call for tool selection)
- Example: User-facing chatbot with multiple capabilities

**Sequential Mode** (`:execution-mode :sequential`):
- Use when: Workflow is well-defined and predictable
- Benefits: Lower latency, deterministic behavior
- Trade-off: Less flexible, cannot adapt to unexpected input
- Example: Data processing pipeline with fixed steps

### Tool Design Guidelines

1. **Passive Tools**: Should NOT contain LLM calls (database, file operations, calculations)
2. **Sub-Agent Tools**: CAN contain LLM reasoning by wrapping a sub-agent
3. **Use Descriptive Tool Names**: Tool names and descriptions help the LLM understand what each tool does
4. **Provide Good Descriptions**: The description field is crucial for LLM-based tool selection
5. **Set a Primary Tool**: Always configure a fallback primary tool for when LLM selection fails
6. **Custom Selection for Complex Logic**: Use `:function` strategy when you need sophisticated context-based selection logic

### Hierarchical Agent Design

1. **Parent Agent**: Coordinates sub-agents, uses LLM for high-level routing
2. **Sub-Agents**: Specialized for specific domains, have their own tool sets
3. **Tool Wrappers**: Use `create-sub-agent-tool` to cleanly wrap sub-agents
4. **Mixed Modes**: Parent can be Autonomous while sub-agents are Sequential (or vice versa)
5. **Clear Boundaries**: Each level has well-defined responsibilities

## Next Steps

1. Run the examples to see tool selection in action
2. Experiment with different inputs to see how tools are selected
3. Create your own multi-tool agents for your specific use cases
4. Consider combining tool selection with the orchestrator for even more powerful agent systems

## Related Documentation

- [WHY_LLM_IN_TOOLS.md](../WHY_LLM_IN_TOOLS.md) - **Corrected architecture understanding**
- [AGENTIC_FRAMEWORK.md](../AGENTIC_FRAMEWORK.md) - Core framework concepts
- [README.md](../README.md) - Project overview
- [agent.clj](../src/study_llm/agent.clj) - Agent framework implementation
