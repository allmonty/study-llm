(ns study-llm.agents.query-assistant
  "Query Assistant Agent - A multi-tool agent that helps users interact with the database.
  
  This agent demonstrates the multi-tool selection capability by using LLM to choose
  between different tools based on the user's natural language input. It can:
  - Provide help and guidance
  - Show database schema information
  - Execute SQL queries
  
  The agent uses the :llm tool selection strategy to intelligently decide which
  tool to use based on the user's intent."
  (:require [study-llm.agent :as agent]
            [study-llm.db :as db]
            [study-llm.llm :as llm]
            [clojure.string :as str]
            [clojure.tools.logging :as log]))

(defn help-tool
  "Tool for providing help and guidance to users."
  []
  (agent/create-tool
    :help
    "Provides help, guidance, and information about how to use the database system"
    (fn [input context]
      (log/info "Providing help information")
      {:status :success
       :result (str "I can help you query the database!\n\n"
                    "Available tables:\n"
                    "- customers: Customer information and total spending\n"
                    "- products: Product catalog with categories and pricing\n"
                    "- orders: Order records with status\n"
                    "- order_items: Individual items in each order\n\n"
                    "Example questions you can ask:\n"
                    "- Show me the schema (to see all table details)\n"
                    "- What are the top 5 customers by total spent?\n"
                    "- How many products are in stock?\n"
                    "- List all pending orders\n\n"
                    "Just ask in natural language and I'll help!")
       :updated-context {:help-provided true}})))

(defn schema-tool
  "Tool for retrieving and displaying database schema information."
  []
  (agent/create-tool
    :schema
    "Retrieves and displays the database schema showing tables, columns, and data types"
    (fn [input context]
      (log/info "Retrieving database schema")
      (let [schema (db/get-schema-info)]
        {:status :success
         :result (str "Database Schema:\n\n"
                      (str/join "\n\n"
                                (map (fn [table]
                                       (str "Table: " (:table table) "\n"
                                            "Columns:\n"
                                            (str/join "\n"
                                                     (map (fn [col]
                                                           (str "  - " (:name col) " (" (:type col) ")"))
                                                          (:columns table)))))
                                     schema)))
         :updated-context {:schema schema
                          :schema-shown true}}))))

(defn execute-sql-tool
  "Tool for executing SQL queries against the database."
  []
  (agent/create-tool
    :execute-sql
    "Executes SQL queries against the PostgreSQL database and returns the results"
    (fn [input context]
      (log/info "Executing SQL query from user input")
      ;; Extract SQL from input - look for SQL-like patterns
      (let [sql-pattern #"(?i)(?:execute|run|query)?[\s:]*([SELECT|INSERT|UPDATE|DELETE].*)"
            sql-match (re-find sql-pattern input)
            sql (if sql-match
                  (str/trim (second sql-match))
                  ;; If no SQL found in input, check context
                  (or (:sql context) (:generated-sql context)))
            result (if sql
                     (db/execute-query! sql)
                     {:error "No SQL query found in input or context"})]
        (if (:error result)
          {:status :error
           :message (:error result)
           :result nil}
          {:status :success
           :result (str "Query executed successfully!\n"
                        "Found " (count result) " result(s):\n\n"
                        (str/join "\n" (take 10 (map pr-str result)))
                        (when (> (count result) 10)
                          (str "\n... and " (- (count result) 10) " more")))
           :updated-context {:query-results result
                            :result-count (count result)}})))))

(defn stats-tool
  "Tool for providing database statistics and summaries."
  []
  (agent/create-tool
    :stats
    "Provides statistics and summary information about the database contents"
    (fn [input context]
      (log/info "Retrieving database statistics")
      (try
        (let [customer-count (-> (db/execute-query! "SELECT COUNT(*) as count FROM customers")
                                 first
                                 :count)
              product-count (-> (db/execute-query! "SELECT COUNT(*) as count FROM products")
                               first
                               :count)
              order-count (-> (db/execute-query! "SELECT COUNT(*) as count FROM orders")
                             first
                             :count)]
          {:status :success
           :result (str "Database Statistics:\n\n"
                        "- Total Customers: " customer-count "\n"
                        "- Total Products: " product-count "\n"
                        "- Total Orders: " order-count "\n\n"
                        "You can ask me to show more details about any of these!")
           :updated-context {:stats {:customers customer-count
                                    :products product-count
                                    :orders order-count}}})
        (catch Exception e
          {:status :error
           :message (str "Error retrieving stats: " (.getMessage e))})))))

(defn create-query-assistant-agent
  "Create the Query Assistant agent with multi-tool selection.
  
  This agent demonstrates the :llm tool selection strategy. It can intelligently
  choose between different tools based on the user's natural language input:
  
  - :help - When user asks for help or doesn't know what to do
  - :schema - When user wants to see table structure
  - :execute-sql - When user wants to run a SQL query
  - :stats - When user wants database statistics
  
  The LLM analyzes the user's input and selects the most appropriate tool.
  
  Example usage:
    (def assistant (create-query-assistant-agent))
    (agent/execute assistant \"show me the schema\" {})
    (agent/execute assistant \"help me get started\" {})
    (agent/execute assistant \"what are the database statistics\" {})"
  []
  (let [tools {:help (help-tool)
               :schema (schema-tool)
               :execute-sql (execute-sql-tool)
               :stats (stats-tool)}
        memory (agent/create-memory :conversation)]
    (agent/create-llm-agent
      "query-assistant"
      "Multi-tool agent that helps users interact with the database by intelligently selecting the right tool"
      tools
      :memory memory
      :config {:tool-selection-strategy :llm
               :primary-tool :help})))
