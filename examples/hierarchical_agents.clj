(ns hierarchical-agents
  "Example demonstrating hierarchical agent composition.
  
  This example shows how to:
  1. Create sub-agents with specialized reasoning
  2. Wrap sub-agents in tools
  3. Build a parent agent that coordinates sub-agents
  4. Toggle between Autonomous and Sequential execution modes
  
  Architecture:
  
  Parent Agent (Coordinator)
    ├─> Sub-Agent Tool 1 (Text Processor)
    │    └─> Sub-Agent with LLM reasoning
    │         ├─> Summarize Tool
    │         └─> Translate Tool
    ├─> Sub-Agent Tool 2 (Data Analyzer)
    │    └─> Sub-Agent with LLM reasoning
    │         ├─> Statistics Tool
    │         └─> Insights Tool
    └─> Direct Tool (Logger)
         └─> Passive execution (no sub-agent)
  "
  (:require [study-llm.agent :as agent]
            [clojure.string :as str]))

;; ============================================================================
;; Level 2: Leaf Tools (Passive Executors)
;; ============================================================================

(defn create-summarize-tool
  "Passive tool that creates summaries."
  []
  (agent/create-tool
    :summarize
    "Summarizes text to key points"
    (fn [text context]
      (cond
        (nil? text)
        {:status :error
         :result "Cannot summarize nil text"
         :updated-context {:summarized false}}
        
        (empty? text)
        {:status :error
         :result "Cannot summarize empty text"
         :updated-context {:summarized false}}
        
        :else
        {:status :success
         :result (str "Summary: " (subs text 0 (min 100 (count text))) "...")
         :updated-context {:summarized true}}))))

(defn create-translate-tool
  "Passive tool that translates text."
  []
  (agent/create-tool
    :translate
    "Translates text to another language"
    (fn [text context]
      (cond
        (nil? text)
        {:status :error
         :result "Cannot translate nil text"
         :updated-context {:translated false}}
        
        (empty? text)
        {:status :error
         :result "Cannot translate empty text"
         :updated-context {:translated false}}
        
        :else
        {:status :success
         :result (str "Translated: " text)
         :updated-context {:translated true}}))))

(defn create-statistics-tool
  "Passive tool that computes statistics."
  []
  (agent/create-tool
    :statistics
    "Computes basic statistics"
    (fn [data context]
      {:status :success
       :result {:count 42 :mean 7.5 :median 8.0}
       :updated-context {:stats-computed true}})))

(defn create-insights-tool
  "Passive tool that generates insights."
  []
  (agent/create-tool
    :insights
    "Generates data insights"
    (fn [data context]
      {:status :success
       :result "Key insight: Data shows upward trend"
       :updated-context {:insights-generated true}})))

(defn create-logger-tool
  "Passive tool for logging."
  []
  (agent/create-tool
    :logger
    "Logs information"
    (fn [message context]
      (println "LOG:" message)
      {:status :success
       :result "Logged successfully"
       :updated-context {:logged true}})))

;; ============================================================================
;; Level 1: Sub-Agents (Controllers with Reasoning)
;; ============================================================================

(defn create-text-processor-agent
  "Sub-agent specialized in text processing.
  
  Execution modes:
  - :autonomous - Uses LLM to decide between summarize/translate
  - :sequential - Always uses primary tool (summarize)"
  [& {:keys [execution-mode] :or {execution-mode :autonomous}}]
  (let [tools {:summarize (create-summarize-tool)
               :translate (create-translate-tool)}]
    (agent/create-llm-agent
      "text-processor"
      "Specialized sub-agent for text processing tasks"
      tools
      :config {:execution-mode execution-mode
               :primary-tool :summarize})))

(defn create-data-analyzer-agent
  "Sub-agent specialized in data analysis.
  
  Execution modes:
  - :autonomous - Uses LLM to decide between statistics/insights
  - :sequential - Always uses primary tool (statistics)"
  [& {:keys [execution-mode] :or {execution-mode :autonomous}}]
  (let [tools {:statistics (create-statistics-tool)
               :insights (create-insights-tool)}]
    (agent/create-llm-agent
      "data-analyzer"
      "Specialized sub-agent for data analysis tasks"
      tools
      :config {:execution-mode execution-mode
               :primary-tool :statistics})))

;; ============================================================================
;; Level 0: Parent Agent (Top-level Controller)
;; ============================================================================

(defn create-coordinator-agent
  "Parent agent that coordinates sub-agents and direct tools.
  
  This demonstrates hierarchical composition where:
  - Sub-agents are wrapped in tools
  - Parent agent can be Autonomous or Sequential
  - Each level maintains its own execution mode
  
  Parameters:
  - execution-mode: :autonomous or :sequential for parent agent
  - sub-agent-mode: :autonomous or :sequential for sub-agents"
  [& {:keys [execution-mode sub-agent-mode]
      :or {execution-mode :autonomous
           sub-agent-mode :autonomous}}]
  (let [;; Create sub-agents
        text-processor (create-text-processor-agent :execution-mode sub-agent-mode)
        data-analyzer (create-data-analyzer-agent :execution-mode sub-agent-mode)
        
        ;; Wrap sub-agents in tools
        tools {:text-processing (agent/create-sub-agent-tool
                                  :text-processing
                                  "Delegates to text processing sub-agent"
                                  text-processor)
               :data-analysis (agent/create-sub-agent-tool
                               :data-analysis
                               "Delegates to data analysis sub-agent"
                               data-analyzer)
               :logging (create-logger-tool)}]
    
    (agent/create-llm-agent
      "coordinator"
      "Parent agent that coordinates sub-agents"
      tools
      :config {:execution-mode execution-mode
               :primary-tool :text-processing})))

;; ============================================================================
;; Example Usage
;; ============================================================================

(defn -main
  "Demonstrates hierarchical agent composition with different execution modes."
  [& args]
  (println "\n=== Hierarchical Agent Composition Demo ===\n")
  
  ;; Example 1: Fully Sequential (Deterministic)
  (println "Example 1: Fully Sequential Mode")
  (println "- Parent: Sequential")
  (println "- Sub-agents: Sequential")
  (let [agent (create-coordinator-agent
               :execution-mode :sequential
               :sub-agent-mode :sequential)
        result (agent/execute agent "Process this text" {})]
    (println "Result:" (:result result))
    (println "Context updates:" (:updated-context result))
    (println))
  
  ;; Example 2: Autonomous Parent, Sequential Sub-agents
  (println "Example 2: Autonomous Parent, Sequential Sub-agents")
  (println "- Parent: Autonomous (LLM selects which sub-agent)")
  (println "- Sub-agents: Sequential (deterministic tool selection)")
  (let [agent (create-coordinator-agent
               :execution-mode :autonomous
               :sub-agent-mode :sequential)
        result (agent/execute agent "Analyze this data" {})]
    (println "Result:" (:result result))
    (println "Tool used:" (:tool-used result))
    (println))
  
  ;; Example 3: Fully Autonomous (Maximum Intelligence)
  (println "Example 3: Fully Autonomous Mode")
  (println "- Parent: Autonomous (LLM selects which sub-agent)")
  (println "- Sub-agents: Autonomous (LLM selects which tool)")
  (let [agent (create-coordinator-agent
               :execution-mode :autonomous
               :sub-agent-mode :autonomous)
        result (agent/execute agent "Summarize this document" {})]
    (println "Result:" (:result result))
    (println "Tool used:" (:tool-used result))
    (println))
  
  ;; Example 4: Direct invocation of sub-agent
  (println "Example 4: Direct Sub-agent Usage")
  (println "- Using text processor sub-agent directly")
  (let [sub-agent (create-text-processor-agent :execution-mode :sequential)
        result (agent/execute sub-agent "Sample text to process" {})]
    (println "Result:" (:result result))
    (println))
  
  (println "=== Demo Complete ===\n")
  (println "Key Takeaways:")
  (println "1. Agents can wrap other agents in tools (hierarchical composition)")
  (println "2. Each agent level can have different execution modes")
  (println "3. Autonomous mode: LLM-driven intelligence")
  (println "4. Sequential mode: Deterministic, predictable")
  (println "5. Tools are passive unless wrapping a sub-agent"))

(comment
  ;; REPL experiments
  
  ;; Create a simple coordinator
  (def coordinator (create-coordinator-agent))
  
  ;; Execute with text input
  (agent/execute coordinator "Process this text" {})
  
  ;; Create autonomous coordinator
  (def smart-coordinator (create-coordinator-agent :execution-mode :autonomous))
  
  ;; Execute - agent will use LLM to select appropriate sub-agent
  (agent/execute smart-coordinator "Analyze the data trends" {})
  
  ;; Check agent memory
  (agent/get-memory (:memory coordinator) :limit 5)
  )
