(ns study-llm.agent.tools
  "Tool creation and invocation for agents."
  (:require [clojure.tools.logging :as log]
            [study-llm.agent.protocol :refer [execute]]))

;; ============================================================================
;; Tool Registry
;; ============================================================================

(defn create-tool
  "Create a tool that agents can use.
  
  A tool is a named function with metadata describing its capabilities.
  
  Parameters:
  - name: Unique identifier for the tool
  - description: What the tool does (used for agent planning)
  - fn: The actual function to execute
  - schema: Optional parameter schema for validation"
  [name description f & {:keys [schema]}]
  {:name name
   :description description
   :fn f
   :schema schema})

(defn create-sub-agent-tool
  "Create a tool that wraps a sub-agent, enabling hierarchical agent composition.
  
  This allows tools to delegate to specialized sub-agents for complex reasoning tasks.
  The sub-agent has its own LLM reasoning, tools, and memory.
  
  Parameters:
  - name: Unique identifier for the tool
  - description: What the sub-agent does
  - sub-agent: An agent instance to delegate to
  
  Example:
    (def sql-expert (create-llm-agent ...))
    (def sql-tool (create-sub-agent-tool :sql-expert \"SQL reasoning\" sql-expert))
  "
  [name description sub-agent]
  (when (nil? sub-agent)
    (throw (ex-info "sub-agent cannot be nil" 
                   {:name name :description description})))
  (create-tool name description
    (fn [input context]
      (execute sub-agent input context))
    :schema {:type :sub-agent
             :agent-name (or (:name sub-agent) "unknown")}))

(defn invoke-tool
  "Invoke a tool with the given arguments."
  [tool & args]
  (try
    (apply (:fn tool) args)
    (catch Exception e
      (log/error e "Error invoking tool:" (:name tool))
      {:status :error
       :message (.getMessage e)
       :tool (:name tool)})))
