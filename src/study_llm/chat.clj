(ns study-llm.chat
  "Terminal-based chat interface for interacting with the LLM-powered database system."
  (:require [study-llm.db :as db]
            [study-llm.llm :as llm]
            [clojure.string :as str]
            [clojure.tools.logging :as log]))

(defn print-separator []
  (println (str (apply str (repeat 80 "-")))))

(defn print-welcome []
  (println)
  (print-separator)
  (println "  🤖 LLM-Powered Database Chat System")
  (print-separator)
  (println)
  (println "  Welcome! You can ask questions about the database in natural language.")
  (println "  The system will:")
  (println "    1. Convert your question to SQL using the LLM")
  (println "    2. Execute the query against PostgreSQL")
  (println "    3. Have the LLM analyze and summarize the results")
  (println)
  (println "  Example questions:")
  (println "    - What are the top 5 customers by total spent?")
  (println "    - How many orders were completed vs processing?")
  (println "    - Show me products in the Electronics category")
  (println "    - Which country has the most customers?")
  (println)
  (println "  Type 'exit' or 'quit' to leave")
  (println "  Type 'schema' to see the database structure")
  (println "  Type 'help' for more information")
  (println)
  (print-separator)
  (println))

(defn print-help []
  (println)
  (println "📖 Help Information:")
  (println)
  (println "How it works:")
  (println "  1. You ask a question in plain English")
  (println "  2. The LLM (Llama2 via Ollama) converts it to SQL")
  (println "  3. The SQL runs against PostgreSQL database")
  (println "  4. The LLM analyzes the results and explains them")
  (println)
  (println "Commands:")
  (println "  exit/quit - Exit the chat")
  (println "  schema    - View database schema")
  (println "  help      - Show this help message")
  (println)
  (println "Tips:")
  (println "  - Be specific in your questions")
  (println "  - You can ask for summaries, counts, top N items, etc.")
  (println "  - The system works best with analytical questions")
  (println))

(defn print-schema [schema-info]
  (println)
  (println "📊 Database Schema:")
  (println)
  (doseq [table schema-info]
    (println "Table:" (:table table))
    (doseq [col (:columns table)]
      (println (str "  - " (:name col) " (" (:type col) ")")))
    (println)))

(defn process-question
  "Process a user question by generating SQL, executing it, and analyzing results."
  [question schema-info]
  (println)
  (println "🤔 Thinking...")
  (println)
  
  ;; Step 1: Generate SQL from the question
  ;; NOTE: In production, consider adding a debug mode to control SQL logging
  (println "Step 1: Converting your question to SQL...")
  (let [sql-result (llm/generate-sql-from-question question schema-info)]
    (if (= :success (:status sql-result))
      (let [sql (:sql sql-result)
            ;; Clean up SQL - remove markdown formatting if present
            clean-sql (-> sql
                         (str/replace #"```sql" "")
                         (str/replace #"```" "")
                         str/trim)]
        (println "Generated SQL:")
        (println "  " clean-sql)
        (println)
        
        ;; Step 2: Execute the SQL query
        (println "Step 2: Executing query against database...")
        (let [query-results (db/execute-query! [clean-sql])]
          (if (:error query-results)
            (do
              (println "❌ Error executing query:" (:error query-results))
              (println)
              (println "The SQL might be incorrect. Try rephrasing your question."))
            (do
              (println "✅ Query executed successfully!")
              (println "Found" (count query-results) "result(s)")
              (println)
              
              ;; Step 3: Have LLM analyze the results
              (println "Step 3: Analyzing results...")
              (let [analysis-result (llm/analyze-results question query-results)]
                (if (= :success (:status analysis-result))
                  (do
                    (println)
                    (print-separator)
                    (println "📊 Analysis:")
                    (println)
                    (println (:analysis analysis-result))
                    (println)
                    (print-separator))
                  (do
                    (println "❌ Error analyzing results:" (:message analysis-result))
                    (println)
                    (println "Raw results:")
                    (doseq [row (take 10 query-results)]
                      (println row)))))))))
      (do
        (println "❌ Error generating SQL:" (:message sql-result))
        (println)
        (println "Please try rephrasing your question or check if Ollama is running.")))))

(defn handle-input
  "Handle user input and route to appropriate handler."
  [input schema-info]
  (let [trimmed (str/trim input)
        lower (str/lower-case trimmed)]
    (cond
      (or (= lower "exit") (= lower "quit"))
      :exit
      
      (= lower "help")
      (do
        (print-help)
        :continue)
      
      (= lower "schema")
      (do
        (print-schema schema-info)
        :continue)
      
      (str/blank? trimmed)
      :continue
      
      :else
      (do
        (process-question trimmed schema-info)
        :continue))))

(defn start-chat
  "Start the interactive chat session."
  []
  (print-welcome)
  
  ;; Get schema information once at startup
  (println "Loading database schema...")
  (let [schema-info (db/get-schema-info)]
    (println "✅ Schema loaded!")
    (println)
    
    ;; Main chat loop
    (loop []
      (print "You: ")
      (flush)
      (when-let [input (read-line)]
        (let [result (handle-input input schema-info)]
          (when (not= result :exit)
            (println)
            (recur)))))
    
    (println)
    (println "👋 Goodbye! Thanks for using the LLM-powered database chat system.")
    (println)))
