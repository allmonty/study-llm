(ns study-llm.agents.database-executor
  "Database Executor Agent - Executes SQL queries against PostgreSQL.
  
  This agent is specialized for database operations and is part of the
  agentic framework. It handles query execution, error handling, and
  result formatting."
  (:require [study-llm.agent :as agent]
            [study-llm.db :as db]
            [clojure.tools.logging :as log]))

(defn execute-query-tool
  "Tool for executing SQL queries against the database."
  []
  (agent/create-tool
    :execute-query
    "Executes SQL queries against PostgreSQL database"
    (fn [sql context]
      (log/info "Executing SQL query")
      (let [result (db/execute-query! [sql])]
        (if (:error result)
          {:status :error
           :message (:error result)
           :result nil}
          {:status :success
           :result result
           :updated-context {:query-results result
                           :result-count (count result)}})))))

(defn get-schema-tool
  "Tool for retrieving database schema information."
  []
  (agent/create-tool
    :get-schema
    "Retrieves database schema information"
    (fn [_ context]
      (log/info "Retrieving database schema")
      (let [schema (db/get-schema-info)]
        {:status :success
         :result schema
         :updated-context {:schema schema}}))))

(defn create-database-executor-agent
  "Create the Database Executor agent for query execution.
  
  This agent specializes in executing SQL queries and managing database operations.
  
  Capabilities:
  - Execute SQL SELECT queries
  - Retrieve schema information
  - Error handling for invalid SQL
  - Connection pooling management
  - Result formatting"
  []
  (let [tools {:query (execute-query-tool)
               :schema (get-schema-tool)}
        memory (agent/create-memory :conversation)]
    (agent/create-database-agent
      "database-executor"
      "Executes SQL queries against PostgreSQL database"
      tools
      :memory memory
      :config {:max-results 1000
               :timeout 30000})))
