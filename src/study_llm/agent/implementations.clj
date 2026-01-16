(ns study-llm.agent.implementations
  "Agent implementations (LLMAgent, DatabaseAgent, etc.)."
  (:require [clojure.tools.logging :as log]
            [study-llm.agent.protocol :refer [Agent]]
            [study-llm.agent.memory :refer [create-memory add-to-memory]]
            [study-llm.agent.tools :refer [invoke-tool]]
            [study-llm.agent.selection :refer [select-tool]]))

;; ============================================================================
;; Agent Implementations
;; ============================================================================

(defrecord LLMAgent [name description tools memory config]
  Agent
  (execute [this input context]
    (log/info "LLMAgent executing:" name)
    ;; Select the appropriate tool based on configuration
    (let [tool-fn (select-tool tools input context config)
          result (if tool-fn
                   (let [tool-result (invoke-tool tool-fn input context)]
                     (assoc tool-result :tool-used (:name tool-fn)))
                   {:status :error
                    :message (str "No tool found for agent " name ". Available tools: " (keys tools))})]
      ;; Store interaction in memory
      (when memory
        (add-to-memory memory {:input input
                              :result result
                              :timestamp (java.time.Instant/now)
                              :agent name}))
      result)))

(defn create-llm-agent
  "Create an LLM-based agent with specific capabilities.
  
  Parameters:
  - name: Agent identifier
  - description: What this agent does
  - tools: Map of tool-name -> tool for this agent
  - config: Agent-specific configuration
  
  Config options:
  - :execution-mode - :autonomous (LLM-driven, default) or :sequential (deterministic)
  - :tool-selection-strategy - :llm (intelligent), :primary (default), or :function (custom)
  - :primary-tool - The default tool to use in :primary strategy
  - :tool-selector-fn - Custom function for :function strategy
  - :temperature - LLM temperature for tool selection
  
  Execution modes:
  - :autonomous - Agent uses LLM to reason and select appropriate tool (:tool-selection-strategy :llm)
  - :sequential - Agent follows predefined sequence (:tool-selection-strategy :primary)
  
  Example (Autonomous):
    (create-llm-agent \"smart-agent\" \"Intelligent agent\" tools
      :config {:execution-mode :autonomous})
  
  Example (Sequential):
    (create-llm-agent \"simple-agent\" \"Deterministic agent\" tools
      :config {:execution-mode :sequential :primary-tool :specific-tool})
  "
  [name description tools & {:keys [memory config]
                             :or {memory (create-memory :conversation)
                                  config {}}}]
  ;; Set tool-selection-strategy based on execution-mode if not explicitly set
  (let [execution-mode (:execution-mode config)
        updated-config (if (and execution-mode (not (:tool-selection-strategy config)))
                        (case execution-mode
                          :autonomous (assoc config :tool-selection-strategy :llm)
                          :sequential (assoc config :tool-selection-strategy :primary)
                          ;; Default: treat unknown modes as sequential
                          (do
                            (log/warn "Unknown execution-mode:" execution-mode 
                                     "- defaulting to :sequential")
                            (assoc config :tool-selection-strategy :primary)))
                        config)]
    (->LLMAgent name description tools memory updated-config)))

(defrecord DatabaseAgent [name description tools memory config]
  Agent
  (execute [this input context]
    (log/info "DatabaseAgent executing:" name)
    ;; Select the appropriate tool based on configuration
    (let [tool-fn (select-tool tools input context config)
          result (if tool-fn
                   (let [tool-result (invoke-tool tool-fn input context)]
                     (assoc tool-result :tool-used (:name tool-fn)))
                   {:status :error
                    :message (str "No tool found for agent " name ". Available tools: " (keys tools))})]
      ;; Store interaction in memory
      (when memory
        (add-to-memory memory {:input input
                              :result result
                              :timestamp (java.time.Instant/now)
                              :agent name}))
      result)))

(defn create-database-agent
  "Create a database agent for executing queries and managing data.
  
  Parameters:
  - name: Agent identifier
  - description: What this agent does
  - tools: Map of tool-name -> tool for database operations"
  [name description tools & {:keys [memory config]
                             :or {memory (create-memory :conversation)
                                  config {}}}]
  (->DatabaseAgent name description tools memory config))
