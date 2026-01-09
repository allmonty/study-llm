(ns study-llm.agent
  "Core agent framework inspired by Microsoft Agentic Framework principles.
  
  This namespace implements the fundamental agent abstraction that enables:
  1. Agent-based architecture with clear responsibilities
  2. Tool/function calling capabilities
  3. Context and memory management
  4. Extensible agent system
  
  Design inspired by:
  - Microsoft Semantic Kernel: Agent abstraction and planning
  - Microsoft AutoGen: Multi-agent coordination
  - LangChain: Tool abstraction patterns"
  (:require [clojure.tools.logging :as log]))

;; ============================================================================
;; Agent Protocol
;; ============================================================================

(defprotocol Agent
  "Core agent protocol defining the interface for all agents in the system.
  
  Agents are autonomous components that:
  - Have a specific role and capabilities
  - Can use tools to perform actions
  - Maintain context across interactions
  - Can be composed and orchestrated"
  
  (get-name [this]
    "Return the agent's name/identifier")
  
  (get-description [this]
    "Return a description of the agent's capabilities")
  
  (get-tools [this]
    "Return a collection of tools this agent can use")
  
  (execute [this input context]
    "Execute the agent's primary function with the given input and context.
    Returns a map with :status, :result, and updated :context"))

;; ============================================================================
;; Tool Protocol
;; ============================================================================

(defprotocol Tool
  "Protocol for tools that agents can use.
  
  Tools are discrete functions that agents can invoke to perform actions
  such as database queries, LLM calls, calculations, etc."
  
  (get-tool-name [this]
    "Return the tool's name")
  
  (get-tool-description [this]
    "Return a description of what this tool does")
  
  (get-tool-parameters [this]
    "Return the expected parameters for this tool")
  
  (invoke-tool [this params context]
    "Invoke the tool with the given parameters and context.
    Returns a map with :status and :result"))

;; ============================================================================
;; Base Agent Implementation
;; ============================================================================

(defrecord BaseAgent [name description tools execute-fn]
  Agent
  (get-name [_] name)
  (get-description [_] description)
  (get-tools [_] tools)
  (execute [this input context]
    (try
      (log/info "Agent" name "executing with input:" (pr-str (take 100 (str input))))
      (let [result (execute-fn this input context)]
        (log/info "Agent" name "completed successfully")
        result)
      (catch Exception e
        (log/error e "Agent" name "failed during execution")
        {:status :error
         :message (.getMessage e)
         :context context}))))

(defn create-agent
  "Create a new agent with the given name, description, tools, and execution function.
  
  The execute-fn should be a function of [agent input context] that returns:
  {:status :success/:error, :result any, :context updated-context-map}"
  [name description tools execute-fn]
  (->BaseAgent name description tools execute-fn))

;; ============================================================================
;; Base Tool Implementation
;; ============================================================================

(defrecord BaseTool [name description parameters invoke-fn]
  Tool
  (get-tool-name [_] name)
  (get-tool-description [_] description)
  (get-tool-parameters [_] parameters)
  (invoke-tool [_ params context]
    (try
      (log/debug "Tool" name "invoked with params:" params)
      (let [result (invoke-fn params context)]
        (log/debug "Tool" name "completed")
        result)
      (catch Exception e
        (log/error e "Tool" name "failed")
        {:status :error
         :message (.getMessage e)}))))

(defn create-tool
  "Create a new tool with the given name, description, parameters, and invoke function.
  
  The invoke-fn should be a function of [params context] that returns:
  {:status :success/:error, :result any}"
  [name description parameters invoke-fn]
  (->BaseTool name description parameters invoke-fn))

;; ============================================================================
;; Context Management
;; ============================================================================

(defn create-context
  "Create a new agent execution context.
  
  Context is a map that flows through the agent execution chain and can contain:
  - :history - conversation history
  - :metadata - arbitrary metadata
  - :shared-state - state shared between agents
  - :session-id - unique session identifier"
  ([] (create-context {}))
  ([initial-data]
   (merge {:history []
           :metadata {}
           :shared-state {}
           :session-id (str (java.util.UUID/randomUUID))
           :created-at (java.time.Instant/now)}
          initial-data)))

(defn add-to-history
  "Add an entry to the context history.
  Entry should be a map with :role, :content, and optional :metadata"
  [context entry]
  (update context :history conj
          (assoc entry :timestamp (java.time.Instant/now))))

(defn update-shared-state
  "Update the shared state in the context"
  [context key value]
  (assoc-in context [:shared-state key] value))

(defn get-shared-state
  "Get a value from the shared state in the context"
  [context key]
  (get-in context [:shared-state key]))

;; ============================================================================
;; Agent Utilities
;; ============================================================================

(defn log-agent-step
  "Log an agent execution step to the context history"
  [context agent-name action result]
  (add-to-history context
                  {:role :agent
                   :agent agent-name
                   :action action
                   :result (if (string? result)
                            result
                            (pr-str result))}))

(defn format-agent-response
  "Format a successful agent response"
  [result context]
  {:status :success
   :result result
   :context context})

(defn format-agent-error
  "Format an agent error response"
  [message context]
  {:status :error
   :message message
   :context context})
