(ns study-llm.tools
  "Tool definitions for agents to interact with database and LLM.
  
  Tools are reusable components that agents can use to perform specific actions.
  This follows the Microsoft Agentic Framework pattern of separating capabilities
  from agents, allowing for better composition and reuse."
  (:require [study-llm.agent :as agent]
            [study-llm.db :as db]
            [study-llm.llm :as llm]
            [clojure.tools.logging :as log]))

;; ============================================================================
;; Database Tools
;; ============================================================================

(def query-database-tool
  "Tool for executing SQL queries against the database"
  (agent/create-tool
    "query-database"
    "Execute a SQL query against the PostgreSQL database and return results"
    {:sql "SQL query string to execute"}
    (fn [params _context]
      (let [sql (:sql params)]
        (log/info "Executing database query")
        (let [results (db/execute-query! [sql])]
          (if (:error results)
            {:status :error
             :message (:error results)}
            {:status :success
             :result results}))))))

(def get-schema-tool
  "Tool for retrieving database schema information"
  (agent/create-tool
    "get-schema"
    "Retrieve the database schema including tables, columns, and types"
    {}
    (fn [_params _context]
      (log/info "Retrieving database schema")
      {:status :success
       :result (db/get-schema-info)})))

(def get-sample-data-tool
  "Tool for retrieving sample data from tables"
  (agent/create-tool
    "get-sample-data"
    "Get sample rows from database tables to understand data format"
    {}
    (fn [_params _context]
      (log/info "Retrieving sample data")
      {:status :success
       :result (db/get-sample-data)})))

;; ============================================================================
;; LLM Tools
;; ============================================================================

(def generate-text-tool
  "Tool for generating text completions using the LLM"
  (agent/create-tool
    "generate-text"
    "Generate text completion using the LLM (Ollama)"
    {:prompt "The prompt to send to the LLM"
     :temperature "Temperature for generation (0.0-1.0, optional)"
     :model "Model to use (optional)"}
    (fn [params _context]
      (let [{:keys [prompt temperature model]} params
            opts (cond-> {}
                   temperature (assoc :temperature temperature)
                   model (assoc :model model))]
        (log/info "Generating LLM completion")
        (let [result (apply llm/generate-completion prompt (apply concat opts))]
          (if (= :success (:status result))
            {:status :success
             :result (:response result)}
            {:status :error
             :message (:message result)}))))))

(def check-llm-health-tool
  "Tool for checking LLM service health"
  (agent/create-tool
    "check-llm-health"
    "Check if the Ollama LLM service is healthy and accessible"
    {}
    (fn [_params _context]
      (log/info "Checking LLM health")
      (let [health (llm/check-ollama-health)]
        {:status :success
         :result health}))))

;; ============================================================================
;; Tool Registry
;; ============================================================================

(def all-tools
  "Registry of all available tools in the system"
  {:database {:query-database query-database-tool
              :get-schema get-schema-tool
              :get-sample-data get-sample-data-tool}
   :llm {:generate-text generate-text-tool
         :check-health check-llm-health-tool}})

(defn get-tool
  "Retrieve a tool by category and name"
  [category tool-name]
  (get-in all-tools [category tool-name]))

(defn get-tools-by-category
  "Get all tools in a category"
  [category]
  (vals (get all-tools category)))

(defn get-database-tools
  "Get all database-related tools"
  []
  (get-tools-by-category :database))

(defn get-llm-tools
  "Get all LLM-related tools"
  []
  (get-tools-by-category :llm))
