(ns study-llm.agents.sql-generator
  "SQL Generator Agent - Converts natural language to SQL using LLM.
  
  This agent is specialized for text-to-SQL conversion and is part of the
  agentic framework. It encapsulates the LLM interaction and prompt engineering
  needed for accurate SQL generation."
  (:require [study-llm.agent :as agent]
            [study-llm.llm :as llm]
            [clojure.string :as str]
            [clojure.tools.logging :as log]))

(defn create-sql-prompt
  "Create a prompt for the LLM to generate SQL from natural language.
  This is the key to making text-to-SQL work effectively."
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

(defn generate-sql-tool
  "Tool for generating SQL from natural language."
  []
  (agent/create-tool
    :generate-sql
    "Converts natural language questions to SQL queries using LLM"
    (fn [question context]
      (let [schema-info (:schema context)
            prompt (create-sql-prompt question schema-info)
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

(defn create-sql-generator-agent
  "Create the SQL Generator agent for text-to-SQL conversion.
  
  This agent specializes in converting natural language questions to SQL queries.
  It uses the LLM with a low temperature (0.1) for more deterministic, factual output.
  
  Capabilities:
  - Understands database schema
  - Generates valid SQL queries
  - Handles table relationships and joins
  - Optimized for PostgreSQL syntax"
  []
  (let [tools {:generate (generate-sql-tool)}
        memory (agent/create-memory :conversation)]
    (agent/create-llm-agent
      "sql-generator"
      "Converts natural language questions to SQL queries"
      tools
      :memory memory
      :config {:temperature 0.1
               :model "llama2"})))
