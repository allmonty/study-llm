# Examples Directory

This directory contains examples demonstrating various capabilities of the agentic framework.

## Available Examples

### 1. Multi-Tool Agent Selection (`multi_tool_agent.clj`)

**Demonstrates**: How an agent can intelligently choose between multiple tools based on input and context.

**Key Concepts**:
- Agents with multiple tools
- Tool selection strategies (keyword matching, custom functions)
- Intelligent agent behavior

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

### The Question: "Can an agent decide between multiple tools?"

**Answer: YES!** 

In the agentic framework, agents can have multiple tools and intelligently select which one to use based on:

1. **LLM Understanding**: The agent uses LLM to understand natural language input and choose the best tool
2. **Context Analysis**: The agent examines the context to determine which tool is appropriate
3. **Custom Logic**: You can provide a custom function that implements any selection logic you want

### Where Does the "Intelligence" Come From?

The intelligence is in the **agent's selection mechanism**, not just the tools:

- **Tools** = Individual capabilities (like "add numbers" or "filter data")
- **Agent** = The coordinator that decides which tool to use
- **Selection Strategy** = The logic that makes the decision (LLM, custom function, or primary)

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

1. **Use Descriptive Tool Names**: Tool names and descriptions help the LLM understand what each tool does
2. **Provide Good Descriptions**: The description field is crucial for LLM-based tool selection
3. **Set a Primary Tool**: Always configure a fallback primary tool for when LLM selection fails
4. **Custom Selection for Complex Logic**: Use `:function` strategy when you need sophisticated context-based selection logic

## Next Steps

1. Run the examples to see tool selection in action
2. Experiment with different inputs to see how tools are selected
3. Create your own multi-tool agents for your specific use cases
4. Consider combining tool selection with the orchestrator for even more powerful agent systems

## Related Documentation

- [AGENTIC_FRAMEWORK.md](../AGENTIC_FRAMEWORK.md) - Core framework concepts
- [README.md](../README.md) - Project overview
- [agent.clj](../src/study_llm/agent.clj) - Agent framework implementation
