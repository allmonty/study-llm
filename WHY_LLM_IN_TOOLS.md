# Why LLM Calls Are Made from Tools Instead of Agents

## Executive Summary

In this agentic framework, **LLM calls are made from within tools, not directly from agents**. This is an intentional architectural decision that follows the **Tool Pattern** used by modern agentic frameworks like Microsoft Semantic Kernel, LangChain, and LangGraph.

## The Architecture Pattern

### Current Implementation

```
Agent (LLMAgent)
  └─> Tool (generate-sql-tool)
       └─> LLM Call (llm/generate-completion)
```

### Where LLM Calls Happen

In our codebase, LLM calls are located as follows:

1. **SQL Generator Agent** (`src/study_llm/agents/sql_generator.clj`)
   - Agent: `LLMAgent`
   - Tool: `generate-sql-tool` 
   - **LLM call location**: Inside the tool function at line 53
   ```clojure
   result (llm/generate-completion prompt :temperature 0.1)
   ```

2. **Result Analyzer Agent** (`src/study_llm/agents/result_analyzer.clj`)
   - Agent: `LLMAgent`
   - Tool: `analyze-results-tool`
   - **LLM call location**: Inside the tool function at line 31
   ```clojure
   result (llm/generate-completion prompt :temperature 0.3)
   ```

3. **Agent Framework** (`src/study_llm/agent.clj`)
   - Tool selection: `select-tool-with-llm`
   - **LLM call location**: Inside the tool selection function at line 131
   ```clojure
   llm-result (llm/generate-completion prompt :temperature 0.1)
   ```

## Why This Design?

### 1. **Separation of Concerns**

**Agents** are responsible for:
- Orchestration logic
- Tool selection
- Memory management
- Context passing
- Error handling

**Tools** are responsible for:
- Actual task execution
- LLM interaction
- Database queries
- External API calls
- Business logic

This separation makes the code modular and maintainable.

### 2. **Reusability**

Tools can be:
- Used by different agents
- Combined in different ways
- Tested independently
- Shared across workflows

Example: The same SQL generation tool could be used by:
- An interactive query agent
- A batch reporting agent
- A data export agent

### 3. **Flexibility in Agent Implementation**

Agents can choose between different tools based on:
- Input type
- Context
- Configuration
- Custom selection logic

The agent doesn't need to know *how* a tool works, only *what* it does.

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

### 5. **Alignment with Industry Standards**

This pattern is used by all major agentic frameworks:

#### Microsoft Semantic Kernel
```csharp
// Agent has plugins (tools)
var agent = new Agent();
agent.Plugins.Add(sqlPlugin);  // Plugin calls LLM
await agent.RunAsync(input);
```

#### LangChain
```python
# Agent has tools
from langchain.agents import Tool

sql_tool = Tool(
    name="SQL Generator",
    func=generate_sql,  # This function calls LLM
    description="Converts text to SQL"
)

agent = initialize_agent(tools=[sql_tool])
```

#### LangGraph
```python
# Nodes in graph contain the logic
def sql_node(state):
    # This node calls LLM
    return {"sql": call_llm(state["query"])}

graph = StateGraph()
graph.add_node("sql_generator", sql_node)
```

### 6. **Tool Composition**

Tools can be composed or chained:

```clojure
;; Simple tool
(defn basic-tool []
  (create-tool :basic "Basic" 
    (fn [input _] 
      (llm/generate-completion input))))

;; Composite tool
(defn enhanced-tool []
  (create-tool :enhanced "Enhanced"
    (fn [input context]
      (let [preprocessed (preprocess input)
            result1 (llm/generate-completion preprocessed)
            result2 (llm/generate-completion (refine result1))]
        (merge result1 result2)))))
```

### 7. **Configuration Flexibility**

Each tool can have its own LLM configuration:

```clojure
;; SQL Generator: Low temperature for precision
(defn generate-sql-tool []
  (create-tool :generate-sql "Generate SQL"
    (fn [question context]
      (llm/generate-completion prompt :temperature 0.1))))

;; Analyzer: Higher temperature for creativity  
(defn analyze-results-tool []
  (create-tool :analyze "Analyze results"
    (fn [question context]
      (llm/generate-completion prompt :temperature 0.3))))
```

## Alternative Approaches and Why They're Problematic

### ❌ Approach 1: LLM Calls Directly in Agent

```clojure
;; BAD: Agent calls LLM directly
(defrecord LLMAgent [name]
  Agent
  (execute [this input context]
    (let [prompt (create-prompt input context)
          result (llm/generate-completion prompt)]  ; ❌ Agent knows about LLM
      result)))
```

**Problems**:
- Agent is tightly coupled to LLM implementation
- Can't reuse tools across agents
- Hard to test (must mock LLM for every agent test)
- Agent has too many responsibilities
- Can't swap LLM providers easily

### ❌ Approach 2: Tools Return Prompts, Agent Calls LLM

```clojure
;; BAD: Tools return prompts, agent calls LLM
(defn generate-sql-tool []
  (create-tool :generate-sql "Generate SQL"
    (fn [question context]
      {:prompt (create-sql-prompt question context)})))  ; ❌ Returns prompt, not result

(defrecord LLMAgent [name]
  Agent
  (execute [this input context]
    (let [tool-result (invoke-tool tool input context)
          prompt (:prompt tool-result)
          llm-result (llm/generate-completion prompt)]  ; ❌ Agent calls LLM
      llm-result)))
```

**Problems**:
- Split responsibility makes code harder to follow
- Agent needs to know about prompts
- Tools can't encapsulate their complete behavior
- More complex error handling
- Harder to compose tools

## The Right Pattern: Tools Encapsulate Complete Behavior

```clojure
;; ✅ GOOD: Tool handles everything
(defn generate-sql-tool []
  (create-tool :generate-sql "Generate SQL"
    (fn [question context]
      ;; Tool owns the complete workflow:
      ;; 1. Create prompt
      (let [prompt (create-sql-prompt question context)
            ;; 2. Call LLM
            result (llm/generate-completion prompt :temperature 0.1)]
        ;; 3. Process result
        (if (= :success (:status result))
          {:status :success
           :result (str/trim (:response result))}
          result)))))

;; Agent just orchestrates
(defrecord LLMAgent [name tools]
  Agent
  (execute [this input context]
    (let [tool (select-tool tools input context)]
      (invoke-tool tool input context))))  ; ✅ Simple delegation
```

**Benefits**:
- Clear separation of concerns
- Tool is self-contained and testable
- Agent is simple and reusable
- Easy to understand and maintain

## Real-World Example from the Codebase

### SQL Generator Agent

**File**: `src/study_llm/agents/sql_generator.clj`

```clojure
;; The TOOL contains the LLM call
(defn generate-sql-tool []
  (agent/create-tool
    :generate-sql
    "Converts natural language questions to SQL queries using LLM"
    (fn [question context]
      (let [schema-info (:schema context)
            prompt (create-sql-prompt question schema-info)
            ;; LLM CALL HAPPENS HERE IN THE TOOL
            result (llm/generate-completion prompt :temperature 0.1)]
        (if (= :success (:status result))
          (let [sql (-> (:response result)
                       (str/replace #"```sql" "")
                       (str/replace #"```" "")
                       str/trim)]
            {:status :success
             :result sql
             :updated-context {:generated-sql sql}})
          result)))))

;; The AGENT just orchestrates the tool
(defn create-sql-generator-agent []
  (let [tools {:generate (generate-sql-tool)}
        memory (agent/create-memory :conversation)]
    (agent/create-llm-agent
      "sql-generator"
      "Converts natural language questions to SQL queries"
      tools
      :memory memory
      :config {:temperature 0.1 :model "llama2"})))
```

### What the Agent Does

From `src/study_llm/agent.clj`:

```clojure
(defrecord LLMAgent [name description tools memory config]
  Agent
  (execute [this input context]
    (log/info "LLMAgent executing:" name)
    ;; 1. Select appropriate tool
    (let [tool-fn (select-tool tools input context config)
          ;; 2. Invoke the tool (tool makes LLM call)
          result (if tool-fn
                   (let [tool-result (invoke-tool tool-fn input context)]
                     (assoc tool-result :tool-used (:name tool-fn)))
                   {:status :error
                    :message (str "No tool found for agent " name)})]
      ;; 3. Store in memory
      (when memory
        (add-to-memory memory {:input input
                              :result result
                              :timestamp (java.time.Instant/now)
                              :agent name}))
      result)))
```

**Agent's responsibilities**:
1. Select the right tool
2. Invoke the tool
3. Store interaction in memory
4. Return result

**Tool's responsibilities**:
1. Create appropriate prompt
2. Call LLM with correct parameters
3. Process LLM response
4. Update context
5. Handle errors

## Benefits Demonstrated in This Codebase

### 1. **Framework Support for Multiple Tools per Agent**

The framework is designed to support multiple tools per agent. While the current SQL Generator and Result Analyzer agents use one primary tool each, the architecture supports multiple tools as demonstrated in the `examples/multi_tool_agent.clj` file:

```clojure
;; Example from examples/multi_tool_agent.clj
;; Agent with multiple tools (add, subtract, multiply, divide)
(defn create-math-agent []
  (let [tools {:add (create-add-tool)
               :subtract (create-subtract-tool)
               :multiply (create-multiply-tool)
               :divide (create-divide-tool)}]
    (agent/create-llm-agent
      "math-agent"
      "Performs mathematical operations"
      tools
      :config {:tool-selection-strategy :llm})))
```

The agent can even use the LLM to intelligently select which tool to use based on the input! For example, if the user says "add 5 and 3", the agent uses the LLM to select the `:add` tool.

### 2. **Tool Reusability** (Currently Demonstrated)

The same tool could be used in different agents:

```clojure
;; Reuse SQL tool in different agents
(def sql-tool (generate-sql-tool))

(def interactive-agent 
  (create-llm-agent "interactive" "Interactive" {:sql sql-tool}))

(def batch-agent
  (create-llm-agent "batch" "Batch processing" {:sql sql-tool}))
```

### 3. **Easy Testing**

```clojure
;; Test tool independently
(deftest test-sql-tool
  (let [tool (generate-sql-tool)
        context {:schema test-schema}]
    (testing "generates valid SQL"
      (let [result (invoke-tool tool "How many customers?" context)]
        (is (= :success (:status result)))
        (is (str/starts-with? (:result result) "SELECT"))))))

;; Test agent with mock tool
(deftest test-agent
  (let [mock-tool (create-tool :mock "Mock" 
                    (fn [_ _] {:status :success :result "mocked"}))
        agent (create-llm-agent "test" "Test" {:mock mock-tool})]
    (testing "agent orchestrates tool correctly"
      (let [result (execute agent "input" {})]
        (is (= :success (:status result)))))))
```

### 4. **Different LLM Configurations per Tool**

Each tool can use different LLM settings:

```clojure
;; SQL generation: precise (low temperature)
(defn generate-sql-tool []
  (create-tool :sql "SQL"
    (fn [q c] (llm/generate-completion prompt :temperature 0.1))))

;; Creative analysis: more varied (higher temperature)
(defn analyze-results-tool []
  (create-tool :analyze "Analyze"
    (fn [q c] (llm/generate-completion prompt :temperature 0.3))))
```

## Comparison with Other Patterns

### Pattern Comparison Table

| Aspect | LLM in Tools (✅ Current) | LLM in Agents (❌) | LLM as Separate Service (⚠️) |
|--------|-------------------------|-------------------|----------------------------|
| Separation of Concerns | ✅ Clear | ❌ Coupled | ✅ Clear |
| Reusability | ✅ High | ❌ Low | ✅ High |
| Testability | ✅ Easy | ⚠️ Moderate | ✅ Easy |
| Flexibility | ✅ High | ❌ Low | ⚠️ Moderate |
| Simplicity | ✅ Simple | ⚠️ Moderate | ❌ Complex |
| Industry Standard | ✅ Yes | ❌ No | ⚠️ Sometimes |

## Framework Comparison

### How Other Frameworks Handle This

#### Microsoft Semantic Kernel
**Pattern**: Functions/Plugins contain LLM calls
```csharp
// Plugin (tool) makes LLM call
[SKFunction("Convert text to SQL")]
public async Task<string> GenerateSQL(string question)
{
    var prompt = CreatePrompt(question);
    return await kernel.InvokeAsync(prompt);  // LLM call in plugin
}
```

#### LangChain
**Pattern**: Tools contain the logic
```python
from langchain.tools import Tool

def generate_sql(question: str) -> str:
    prompt = create_prompt(question)
    return llm.invoke(prompt)  # LLM call in tool

sql_tool = Tool(
    name="SQL Generator",
    func=generate_sql,  # Tool function contains LLM call
    description="Converts questions to SQL"
)
```

#### LangGraph  
**Pattern**: Nodes contain the logic
```python
def sql_node(state):
    prompt = create_prompt(state["question"])
    sql = llm.invoke(prompt)  # LLM call in node
    return {"sql": sql}

# Node is like a tool
graph.add_node("generate_sql", sql_node)
```

#### AutoGen
**Pattern**: Agents contain the logic but delegate to functions
```python
class SQLAgent(ConversableAgent):
    @register_function
    def generate_sql(self, question):
        """Function callable by the agent or other agents."""
        prompt = self.create_prompt(question)
        return self.llm.generate(prompt)  # Function contains LLM call
```

**Note**: AutoGen uses a hybrid approach where agents can call LLM directly OR use registered functions. Our framework follows the pure tool/function pattern similar to Semantic Kernel and LangChain.

### Our Implementation Aligns with Semantic Kernel and LangChain

```clojure
;; Semantic Kernel style: Tool (plugin) contains LLM call
(defn generate-sql-tool []
  (create-tool :generate-sql "Generate SQL"
    (fn [question context]
      (let [prompt (create-sql-prompt question context)]
        (llm/generate-completion prompt :temperature 0.1)))))

;; Agent orchestrates tools
(defn create-sql-generator-agent []
  (create-llm-agent "sql-generator" "SQL Generator" 
    {:generate (generate-sql-tool)}))
```

## Conclusion

The decision to call LLMs from tools rather than agents is:

1. **Intentional** - Following industry best practices
2. **Standard** - Used by Semantic Kernel, LangChain, LangGraph
3. **Beneficial** - Provides modularity, reusability, testability
4. **Flexible** - Allows multiple tools, configurations, and compositions
5. **Maintainable** - Clear separation of concerns

### The Key Principle

> **Agents orchestrate, Tools execute.**

- **Agents** decide *what* to do (which tool to use)
- **Tools** decide *how* to do it (including making LLM calls)

This separation is fundamental to building scalable, maintainable agentic systems.

## References

- [Microsoft Semantic Kernel Documentation](https://learn.microsoft.com/en-us/semantic-kernel/)
- [LangChain Tool Documentation](https://python.langchain.com/docs/modules/tools/)
- [LangGraph State Management](https://langchain-ai.github.io/langgraph/)
- [AGENTIC_FRAMEWORK.md](./AGENTIC_FRAMEWORK.md) - Our framework documentation
- [REFACTORING_DECISIONS.md](./REFACTORING_DECISIONS.md) - Architecture decisions

---

**Summary**: LLM calls are in tools because tools encapsulate complete task execution, while agents orchestrate tool selection and context management. This is the standard pattern used across all major agentic frameworks.
