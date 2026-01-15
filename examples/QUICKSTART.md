# Quick Start Guide: Multi-Tool Agent Selection

## What is This?

This guide shows you how to create agents that can intelligently choose between multiple tools.

## The Problem It Solves

**Before**: An agent could only use one tool per execution. If you wanted different behaviors, you needed different agents.

**Now**: A single agent can have multiple tools and intelligently select which one to use based on the user's input or context.

## Quick Example

```clojure
(require '[study-llm.agent :as agent])

;; Step 1: Create tools
(def add-tool
  (agent/create-tool
    :add
    "add sum plus"
    (fn [input context]
      {:status :success
       :result (reduce + (:numbers context))})))

(def multiply-tool
  (agent/create-tool
    :multiply  
    "multiply times product"
    (fn [input context]
      {:status :success
       :result (reduce * (:numbers context))})))

;; Step 2: Create agent with LLM-based selection
(def smart-calc
  (agent/create-llm-agent
    "calculator"
    "Does math operations"
    {:add add-tool
     :multiply multiply-tool}
    :config {:tool-selection-strategy :llm
             :primary-tool :add}))

;; Step 3: Use the agent - it picks the right tool!
(agent/execute smart-calc "add 5 and 3" {:numbers [5 3]})
;; => {:status :success, :result 8, :tool-used :add}

(agent/execute smart-calc "multiply 4 times 7" {:numbers [4 7]})
;; => {:status :success, :result 28, :tool-used :multiply}

(agent/execute smart-calc "what is 10 plus 20?" {:numbers [10 20]})
;; => {:status :success, :result 30, :tool-used :add}
```

## How It Works

The agent uses the LLM to understand your input and select the right tool:
1. Sends input and available tools to the LLM
2. LLM analyzes the intent (e.g., "add", "multiply")
3. LLM selects the most appropriate tool
4. Agent executes that tool

## Three Selection Strategies

### 1. LLM-Based Selection (Recommended for natural language)

Best for: Natural language inputs where users describe what they want to do

```clojure
:config {:tool-selection-strategy :llm
         :primary-tool :add}  ; fallback if LLM fails
```

The agent uses the LLM to:
- Understand the user's intent from natural language
- Analyze available tool descriptions
- Select the most appropriate tool

### 2. Custom Function (For complex logic)

Best for: When you need sophisticated selection based on context

```clojure
(defn my-selector [tools input context]
  (if (:urgent context)
    (:fast-tool tools)
    (:accurate-tool tools)))

:config {:tool-selection-strategy :function
         :tool-selector-fn my-selector}
```

Your function gets full control - examine input, context, anything!

### 3. Primary (Default - no selection)

Best for: When you want to always use the same tool (backward compatible)

```clojure
:config {:primary-tool :my-tool}
```

Just uses the specified tool every time.

## Tips for Good Tool Descriptions

Make your tool descriptions clear and descriptive for the LLM:

```clojure
;; ✅ Good - clear and descriptive
(agent/create-tool
  :add
  "Adds numbers together to get their sum"
  ...)

;; ✅ Also good - includes synonyms
(agent/create-tool
  :multiply
  "Multiplies numbers to get their product"
  ...)

;; ❌ Bad - too short
(agent/create-tool
  :add
  "adds numbers"
  ...)
```

## Real-World Use Cases

### 1. Natural Language Database Agent
```clojure
Tools:
- :select - "Retrieves and displays data from the database"
- :count - "Counts the number of records matching criteria"
- :summarize - "Provides aggregate statistics and summaries"

Input: "show me all customers" -> LLM selects :select
Input: "how many orders?" -> LLM selects :count
Input: "summarize sales data" -> LLM selects :summarize
```

### 2. File Processing Agent
```clojure
Tools:
- :read - "Reads and displays file contents"
- :write - "Writes or updates file contents"
- :delete - "Deletes or removes files"

Input: "read file.txt" -> LLM selects :read
Input: "save to output.txt" -> LLM selects :write
```

### 3. API Client Agent
```clojure
Tools:
- :get - "Retrieves data via GET request"
- :post - "Creates new resources via POST request"
- :put - "Updates existing resources via PUT request"
- :delete - "Deletes resources via DELETE request"

Input: "get user info" -> LLM selects :get
Input: "create new user" -> LLM selects :post
```

## Common Patterns

### Pattern 1: Natural Language Understanding
```clojure
Tools with clear descriptions
LLM understands user intent
Works with varied phrasings
```

### Pattern 2: Context-based Selection
```clojure
Custom selector that checks context:
- If context has :filter-fn -> use :filter tool
- If context has :transform-fn -> use :map tool
```

### Pattern 3: Hybrid Approach
```clojure
Use keyword matching for common cases
Add custom function as fallback for edge cases
```

## Testing Your Multi-Tool Agent

```clojure
;; Test each tool gets selected correctly
(defn test-selection []
  (let [agent (create-my-agent)]
    
    ;; Test 1: Should use add
    (let [r1 (agent/execute agent "add 1 and 2" {:numbers [1 2]})]
      (assert (= :add (:tool-used r1)))
      (assert (= 3 (:result r1))))
    
    ;; Test 2: Should use multiply
    (let [r2 (agent/execute agent "multiply 3 by 4" {:numbers [3 4]})]
      (assert (= :multiply (:tool-used r2)))
      (assert (= 12 (:result r2))))
    
    (println "All tests passed!")))
```

## Next Steps

1. **Try the examples**: Run `examples/multi_tool_agent.clj`
2. **Read the detailed docs**: See `examples/README.md`
3. **Create your own**: Start with 2-3 tools and keyword matching
4. **Experiment**: Try different selection strategies

## Need Help?

- See full examples in `examples/multi_tool_agent.clj`
- Read the framework docs in `AGENTIC_FRAMEWORK.md`
- Check the code in `src/study_llm/agent.clj`
