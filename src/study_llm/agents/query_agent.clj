(ns study-llm.agents.query-agent
  "Query Execution Agent - Executes SQL queries against the database.
  
  This agent is responsible for executing SQL queries and returning results.
  It provides a safe interface for database query execution."
  (:require [study-llm.agent :as agent]
            [study-llm.tools :as tools]
            [clojure.tools.logging :as log]))

(defn query-agent-execute
  "Execute function for the query agent"
  [agent input context]
  (try
    (let [sql (:sql input)]
      
      (if-not sql
        (agent/format-agent-error "No SQL query provided" context)
        
        (let [;; Execute query using database tool
              db-tool (tools/get-tool :database :query-database)
              db-result (agent/invoke-tool db-tool {:sql sql} context)]
          
          (if (= :success (:status db-result))
            (let [results (:result db-result)
                  ;; Log this step in context
                  context (agent/log-agent-step context
                                                (agent/get-name agent)
                                                "execute-query"
                                                {:sql sql
                                                 :row-count (count results)})]
              (agent/format-agent-response results context))
            (agent/format-agent-error
              (str "Failed to execute query: " (:message db-result))
              context)))))
    (catch Exception e
      (log/error e "Query Agent execution failed")
      (agent/format-agent-error (.getMessage e) context))))

(defn create-query-agent
  "Create a new query execution agent"
  []
  (agent/create-agent
    "query-executor"
    "Executes SQL queries against the PostgreSQL database"
    [(tools/get-tool :database :query-database)]
    query-agent-execute))
