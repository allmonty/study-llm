(ns study-llm.agents.sql-agent
  "SQL Generation Agent - Converts natural language to SQL queries.
  
  This agent is specialized in understanding database schemas and generating
  appropriate SQL queries based on user questions. It uses the LLM tool to
  perform text-to-SQL conversion."
  (:require [study-llm.agent :as agent]
            [study-llm.tools :as tools]
            [clojure.string :as str]
            [clojure.tools.logging :as log]))

(defn create-sql-prompt
  "Create a prompt for SQL generation from natural language"
  [user-question schema-info]
  (str "You are a SQL expert. Given the following database schema and a user question, "
       "generate a SQL query that answers the question. "
       "Return ONLY the SQL query, nothing else. No explanations, no markdown formatting.\n\n"
       "Database Schema:\n"
       (str/join "\n"
                 (map (fn [table]
                        (str "Table: " (:table table) "\n"
                             "Columns:\n"
                             (str/join "\n"
                                      (map (fn [col]
                                            (str "  - " (:name col) " (" (:type col) ")"))
                                           (:columns table)))))
                      schema-info))
       "\n\n"
       "Table Relationships:\n"
       "- orders.customer_id → customers.id (each order belongs to a customer)\n"
       "- order_items.order_id → orders.id (each order item belongs to an order)\n"
       "- order_items.product_id → products.id (each order item references a product)\n"
       "Note: order_items does NOT have a customer_id column. To get customer info from order_items, "
       "you must JOIN through orders table: order_items → orders → customers\n\n"
       "User Question: " user-question "\n\n"
       "SQL Query:"))

(defn clean-sql
  "Clean up SQL output from LLM (remove markdown formatting, etc.)"
  [sql]
  (-> sql
      (str/replace #"```sql" "")
      (str/replace #"```" "")
      str/trim))

(defn sql-agent-execute
  "Execute function for the SQL agent"
  [agent input context]
  (try
    (let [question (:question input)
          ;; Get schema from context or fetch it
          schema-info (or (agent/get-shared-state context :schema-info)
                         (let [schema-tool (tools/get-tool :database :get-schema)
                               schema-result (agent/invoke-tool schema-tool {} context)]
                           (when (= :success (:status schema-result))
                             (:result schema-result))))
          
          ;; Update context with schema for reuse
          context (if schema-info
                   (agent/update-shared-state context :schema-info schema-info)
                   context)]
      
      (if-not schema-info
        (agent/format-agent-error "Failed to retrieve database schema" context)
        
        (let [;; Generate SQL using LLM tool
              prompt (create-sql-prompt question schema-info)
              llm-tool (tools/get-tool :llm :generate-text)
              llm-result (agent/invoke-tool llm-tool
                                           {:prompt prompt
                                            :temperature 0.1}
                                           context)]
          
          (if (= :success (:status llm-result))
            (let [sql (clean-sql (:result llm-result))
                  ;; Log this step in context
                  context (agent/log-agent-step context
                                                (agent/get-name agent)
                                                "generate-sql"
                                                {:question question
                                                 :sql sql})]
              (agent/format-agent-response sql context))
            (agent/format-agent-error
              (str "Failed to generate SQL: " (:message llm-result))
              context)))))
    (catch Exception e
      (log/error e "SQL Agent execution failed")
      (agent/format-agent-error (.getMessage e) context))))

(defn create-sql-agent
  "Create a new SQL generation agent"
  []
  (agent/create-agent
    "sql-generator"
    "Converts natural language questions to SQL queries using database schema"
    [(tools/get-tool :database :get-schema)
     (tools/get-tool :llm :generate-text)]
    sql-agent-execute))
