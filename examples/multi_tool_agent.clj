(ns examples.multi_tool_agent
  "Example demonstrating an agent with multiple tools and intelligent tool selection.
  
  This example shows how an agent can decide between multiple tools based on:
  1. LLM-based intelligent selection
  2. Custom selection functions
  3. Configured primary tool
  
  The agent has 'intelligence' in choosing the right tool for the task."
  (:require [study-llm.agent :as agent]
            [clojure.string :as str]))

;; ============================================================================
;; Example: Math Operations Agent
;; ============================================================================
;; This agent can perform different math operations and selects the appropriate
;; tool based on the user's input.

(defn create-add-tool
  "Tool for adding two numbers."
  []
  (agent/create-tool
    :add
    "add sum plus addition"
    (fn [input context]
      (let [numbers (or (:numbers context) [0 0])
            result (reduce + numbers)]
        {:status :success
         :result result
         :explanation (str "Added " (str/join " + " numbers) " = " result)}))))

(defn create-multiply-tool
  "Tool for multiplying two numbers."
  []
  (agent/create-tool
    :multiply
    "multiply product times multiplication"
    (fn [input context]
      (let [numbers (or (:numbers context) [1 1])
            result (reduce * numbers)]
        {:status :success
         :result result
         :explanation (str "Multiplied " (str/join " × " numbers) " = " result)}))))

(defn create-divide-tool
  "Tool for dividing two numbers."
  []
  (agent/create-tool
    :divide
    "divide division quotient"
    (fn [input context]
      (let [numbers (or (:numbers context) [1 1])
            [a b] numbers]
        (if (zero? b)
          {:status :error
           :message "Cannot divide by zero"}
          {:status :success
           :result (/ a b)
           :explanation (str "Divided " a " ÷ " b " = " (/ a b))})))))

(defn create-power-tool
  "Tool for raising a number to a power."
  []
  (agent/create-tool
    :power
    "power exponent raise"
    (fn [input context]
      (let [numbers (or (:numbers context) [2 2])
            [base exponent] numbers
            result (Math/pow base exponent)]
        {:status :success
         :result result
         :explanation (str base "^" exponent " = " result)}))))

(defn create-math-agent
  "Create a math agent with multiple tools that can be selected based on input.
  
  This agent demonstrates intelligent tool selection:
  - Uses LLM to analyze the input and select the right operation
  - The LLM understands natural language and chooses appropriately
  - Defaults to addition if LLM fails
  
  Example inputs:
  - 'add 5 and 3' -> LLM selects add tool
  - 'multiply 4 by 7' -> LLM selects multiply tool
  - 'divide 10 by 2' -> LLM selects divide tool
  - 'raise 2 to the power of 8' -> LLM selects power tool"
  []
  (agent/create-llm-agent
    "math-agent"
    "Performs mathematical operations intelligently selecting the right tool"
    {:add (create-add-tool)
     :multiply (create-multiply-tool)
     :divide (create-divide-tool)
     :power (create-power-tool)}
    :config {:tool-selection-strategy :llm
             :primary-tool :add}))

;; ============================================================================
;; Example: Data Processing Agent
;; ============================================================================
;; This agent processes data in different ways and uses a custom function
;; to select the appropriate tool.

(defn create-filter-tool
  "Tool for filtering data."
  []
  (agent/create-tool
    :filter
    "filter data to include only matching items"
    (fn [input context]
      (let [data (or (:data context) [])
            predicate (or (:predicate context) (constantly true))
            filtered (filter predicate data)]
        {:status :success
         :result (vec filtered)
         :explanation (str "Filtered data to " (count filtered) " items")}))))

(defn create-map-tool
  "Tool for transforming data."
  []
  (agent/create-tool
    :map
    "map transform data by applying a function"
    (fn [input context]
      (let [data (or (:data context) [])
            transform-fn (or (:transform context) identity)
            transformed (map transform-fn data)]
        {:status :success
         :result (vec transformed)
         :explanation (str "Transformed " (count data) " items")}))))

(defn create-reduce-tool
  "Tool for aggregating data."
  []
  (agent/create-tool
    :reduce
    "reduce aggregate summarize data"
    (fn [input context]
      (let [data (or (:data context) [])
            reduce-fn (or (:reducer context) +)
            initial (or (:initial context) 0)
            result (reduce reduce-fn initial data)]
        {:status :success
         :result result
         :explanation (str "Reduced data to single value: " result)}))))

(defn create-sort-tool
  "Tool for sorting data."
  []
  (agent/create-tool
    :sort
    "sort order arrange data"
    (fn [input context]
      (let [data (or (:data context) [])
            comparator (or (:comparator context) compare)
            sorted (sort comparator data)]
        {:status :success
         :result (vec sorted)
         :explanation (str "Sorted " (count data) " items")}))))

(defn data-tool-selector
  "Custom function to select tool based on context and input.
  
  This demonstrates a more sophisticated selection strategy where
  the agent looks at the context to make decisions."
  [tools input context]
  (cond
    ;; If context has a predicate, use filter
    (:predicate context)
    (:filter tools)
    
    ;; If context has a transform function, use map
    (:transform context)
    (:map tools)
    
    ;; If context has a reducer function, use reduce
    (:reducer context)
    (:reduce tools)
    
    ;; If context has a comparator, use sort
    (:comparator context)
    (:sort tools)
    
    ;; Default: use keyword matching
    :else
    (let [input-lower (str/lower-case (str input))]
      (cond
        (re-find #"filter|where|select" input-lower) (:filter tools)
        (re-find #"map|transform|convert" input-lower) (:map tools)
        (re-find #"reduce|sum|aggregate" input-lower) (:reduce tools)
        (re-find #"sort|order|arrange" input-lower) (:sort tools)
        :else (:filter tools))))) ;; default

(defn create-data-processor-agent
  "Create a data processing agent with custom tool selection.
  
  This agent uses a custom selection function that considers both
  the input text and the context to choose the right tool.
  
  Example uses:
  - Filter: {:predicate even?} in context -> uses filter tool
  - Map: {:transform inc} in context -> uses map tool
  - Reduce: {:reducer +} in context -> uses reduce tool
  - Sort: {:comparator >} in context -> uses sort tool"
  []
  (agent/create-llm-agent
    "data-processor"
    "Processes data using the appropriate tool based on input and context"
    {:filter (create-filter-tool)
     :map (create-map-tool)
     :reduce (create-reduce-tool)
     :sort (create-sort-tool)}
    :config {:tool-selection-strategy :function
             :tool-selector-fn data-tool-selector
             :primary-tool :filter}))

;; ============================================================================
;; Usage Examples
;; ============================================================================

(defn demo-math-agent
  "Demonstrates the math agent selecting different tools based on input."
  []
  (println "\n========================================")
  (println "Math Agent - Tool Selection Demo")
  (println "========================================\n")
  
  (let [agent (create-math-agent)]
    ;; Example 1: Addition (LLM understands "add")
    (println "Example 1: 'add 10 and 20'")
    (let [result (agent/execute agent "add 10 and 20" {:numbers [10 20]})]
      (println "  Tool used:" (:tool-used result))
      (println "  Result:" (:result result))
      (println "  Explanation:" (:explanation result))
      (println))
    
    ;; Example 2: Multiplication (LLM understands "multiply")
    (println "Example 2: 'multiply 5 by 6'")
    (let [result (agent/execute agent "multiply 5 by 6" {:numbers [5 6]})]
      (println "  Tool used:" (:tool-used result))
      (println "  Result:" (:result result))
      (println "  Explanation:" (:explanation result))
      (println))
    
    ;; Example 3: Division (LLM understands "divide")
    (println "Example 3: 'divide 100 by 4'")
    (let [result (agent/execute agent "divide 100 by 4" {:numbers [100 4]})]
      (println "  Tool used:" (:tool-used result))
      (println "  Result:" (:result result))
      (println "  Explanation:" (:explanation result))
      (println))
    
    ;; Example 4: Power (LLM understands "power" or "raise")
    (println "Example 4: 'raise 2 to the power of 8'")
    (let [result (agent/execute agent "raise 2 to the power of 8" {:numbers [2 8]})]
      (println "  Tool used:" (:tool-used result))
      (println "  Result:" (:result result))
      (println "  Explanation:" (:explanation result))
      (println))
    
    ;; Example 5: Natural language - LLM understands intent
    (println "Example 5: 'what is 3 plus 7?' (natural language)")
    (let [result (agent/execute agent "what is 3 plus 7?" {:numbers [3 7]})]
      (println "  Tool used:" (:tool-used result))
      (println "  Result:" (:result result))
      (println "  Explanation:" (:explanation result))
      (println))))

(defn demo-data-processor-agent
  "Demonstrates the data processor agent selecting different tools."
  []
  (println "\n========================================")
  (println "Data Processor Agent - Tool Selection Demo")
  (println "========================================\n")
  
  (let [agent (create-data-processor-agent)
        sample-data [1 2 3 4 5 6 7 8 9 10]]
    
    ;; Example 1: Filter (predicate in context)
    (println "Example 1: Filter even numbers")
    (let [result (agent/execute agent 
                                "filter the data"
                                {:data sample-data
                                 :predicate even?})]
      (println "  Tool used:" (:tool-used result))
      (println "  Result:" (:result result))
      (println "  Explanation:" (:explanation result))
      (println))
    
    ;; Example 2: Map (transform in context)
    (println "Example 2: Transform by doubling")
    (let [result (agent/execute agent
                                "transform the data"
                                {:data [1 2 3 4 5]
                                 :transform #(* 2 %)})]
      (println "  Tool used:" (:tool-used result))
      (println "  Result:" (:result result))
      (println "  Explanation:" (:explanation result))
      (println))
    
    ;; Example 3: Reduce (reducer in context)
    (println "Example 3: Sum all numbers")
    (let [result (agent/execute agent
                                "reduce to sum"
                                {:data sample-data
                                 :reducer +
                                 :initial 0})]
      (println "  Tool used:" (:tool-used result))
      (println "  Result:" (:result result))
      (println "  Explanation:" (:explanation result))
      (println))
    
    ;; Example 4: Sort (comparator in context)
    (println "Example 4: Sort descending")
    (let [result (agent/execute agent
                                "sort the data"
                                {:data [5 2 8 1 9 3]
                                 :comparator >})]
      (println "  Tool used:" (:tool-used result))
      (println "  Result:" (:result result))
      (println "  Explanation:" (:explanation result))
      (println))
    
    ;; Example 5: Keyword-based selection (no context hints)
    (println "Example 5: 'filter' in input text")
    (let [result (agent/execute agent
                                "filter data where value is odd"
                                {:data sample-data
                                 :predicate odd?})]
      (println "  Tool used:" (:tool-used result))
      (println "  Result:" (:result result))
      (println "  Explanation:" (:explanation result))
      (println))))

(defn -main
  "Run all demonstrations."
  [& args]
  (println "\n╔════════════════════════════════════════════════════════════╗")
  (println "║  Multi-Tool Agent Selection Demo                          ║")
  (println "║  Demonstrating agent intelligence in choosing tools       ║")
  (println "╚════════════════════════════════════════════════════════════╝")
  
  (demo-math-agent)
  (demo-data-processor-agent)
  
  (println "\n========================================")
  (println "Key Takeaways:")
  (println "========================================")
  (println "1. Agents can have multiple tools and select the right one")
  (println "2. Selection strategies:")
  (println "   - :llm - Use LLM to intelligently choose based on input")
  (println "   - :function - Use custom logic to select tools")
  (println "   - :primary - Always use a specific tool (default)")
  (println "3. The agent has 'intelligence' through:")
  (println "   - LLM understanding of natural language")
  (println "   - Context analysis")
  (println "   - Custom selection functions")
  (println "4. This makes agents more versatile and autonomous")
  (println))

(comment
  ;; Try running the demo:
  (-main)
  
  ;; Or run individual demos:
  (demo-math-agent)
  (demo-data-processor-agent)
  
  ;; Create and test your own multi-tool agent:
  (let [agent (create-math-agent)]
    (agent/execute agent "multiply 7 times 8" {:numbers [7 8]}))
  )
