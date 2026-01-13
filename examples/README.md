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
  - `:keyword` - Matches keywords in input to tool names/descriptions
  - `:function` - Uses a custom function to select the appropriate tool
  - `:primary` - Always uses a configured primary tool (default)

**Run the Example**:
```bash
# From the project root
clj -M -m examples.multi-tool-agent

# Or in REPL
clj
(require '[examples.multi-tool-agent :as demo])
(demo/-main)
```

**Example Agents Included**:

1. **Math Agent** - Performs different math operations (add, multiply, divide, power)
   - Uses keyword matching to select the right operation
   - Example: "multiply 5 by 6" automatically selects the multiply tool

2. **Data Processor Agent** - Processes data using different strategies (filter, map, reduce, sort)
   - Uses a custom selection function that looks at both input and context
   - Example: If context has `:predicate`, automatically uses filter tool

## Understanding Agent Tool Selection

### The Question: "Can an agent decide between multiple tools?"

**Answer: YES!** 

In the agentic framework, agents can have multiple tools and intelligently select which one to use based on:

1. **Keywords in Input**: The agent matches words in the user's input to tool names and descriptions
2. **Context Analysis**: The agent examines the context to determine which tool is appropriate
3. **Custom Logic**: You can provide a custom function that implements any selection logic you want

### Where Does the "Intelligence" Come From?

The intelligence is in the **agent's selection mechanism**, not just the tools:

- **Tools** = Individual capabilities (like "add numbers" or "filter data")
- **Agent** = The coordinator that decides which tool to use
- **Selection Strategy** = The logic that makes the decision

### Example: Math Agent

```clojure
;; Create a math agent with 4 tools
(def math-agent (create-math-agent))

;; The agent intelligently selects the right tool:
(execute math-agent "add 5 and 3" {:numbers [5 3]})
;; → Uses :add tool

(execute math-agent "multiply 4 by 7" {:numbers [4 7]})
;; → Uses :multiply tool

(execute math-agent "divide 100 by 5" {:numbers [100 5]})
;; → Uses :divide tool
```

The agent parses the input, finds keywords like "add", "multiply", "divide", and selects the corresponding tool automatically.

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
;; Using keyword matching
(agent/create-llm-agent
  "my-agent"
  "Agent description"
  {:tool1 tool1
   :tool2 tool2}
  :config {:tool-selection-strategy :keyword
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

1. **Use Descriptive Tool Names**: Tool names and descriptions should contain keywords that might appear in user input
2. **Provide Good Descriptions**: The description field is used for keyword matching
3. **Set a Primary Tool**: Always configure a fallback primary tool for when no match is found
4. **Custom Selection for Complex Logic**: Use `:function` strategy when you need sophisticated selection logic

## Next Steps

1. Run the examples to see tool selection in action
2. Experiment with different inputs to see how tools are selected
3. Create your own multi-tool agents for your specific use cases
4. Consider combining tool selection with the orchestrator for even more powerful agent systems

## Related Documentation

- [AGENTIC_FRAMEWORK.md](../AGENTIC_FRAMEWORK.md) - Core framework concepts
- [README.md](../README.md) - Project overview
- [agent.clj](../src/study_llm/agent.clj) - Agent framework implementation
