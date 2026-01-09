(ns study-llm.chat
  "Terminal-based chat interface for interacting with the LLM-powered database system.
  
  Updated to use the agent-based orchestrator pattern for better modularity and extensibility."
  (:require [study-llm.db :as db]
            [study-llm.orchestrator :as orchestrator]
            [clojure.string :as str]
            [clojure.tools.logging :as log]))

(defn print-separator []
  (println (str (apply str (repeat 80 "-")))))

(defn print-welcome []
  (println)
  (print-separator)
  (println "  🤖 LLM-Powered Database Chat System (Agent-Based)")
  (print-separator)
  (println)
  (println "  Welcome! You can ask questions about the database in natural language.")
  (println "  The system uses an agent-based architecture where:")
  (println "    1. SQL Agent converts your question to SQL using the LLM")
  (println "    2. Query Agent executes the query against PostgreSQL")
  (println "    3. Analysis Agent analyzes and summarizes the results")
  (println "    4. Orchestrator coordinates all agents seamlessly")
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
  (println "How it works (using Agent-based Architecture):")
  (println "  1. SQL Agent: Converts your question to SQL using the LLM")
  (println "  2. Query Agent: Executes the SQL against PostgreSQL database")
  (println "  3. Analysis Agent: Analyzes the results and explains them")
  (println "  4. Orchestrator: Coordinates all agents to work together")
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
  "Process a user question using the agent-based orchestrator.
  
  The orchestrator coordinates multiple specialized agents:
  - SQL Agent: Generates SQL from natural language
  - Query Agent: Executes the SQL query
  - Analysis Agent: Analyzes and summarizes results"
  [question _schema-info]
  (println)
  (println "🤔 Thinking...")
  (println)
  
  (println "Using agent-based orchestrator to answer your question...")
  (println)
  
  ;; Use the orchestrator to coordinate agents
  (let [result (orchestrator/answer-question question)]
    (if (= :success (:status result))
      (do
        ;; Success - display the results
        (println "Step 1: SQL Agent - Generated SQL query")
        (println "  " (:sql result))
        (println)
        
        (println "Step 2: Query Agent - Executed query successfully")
        (println "  Found" (count (:results result)) "result(s)")
        (println)
        
        (println "Step 3: Analysis Agent - Analyzed results")
        (println)
        (print-separator)
        (println "📊 Analysis:")
        (println)
        (println (:analysis result))
        (println)
        (print-separator))
      
      ;; Error - display error message
      (do
        (println "❌ Error during" (name (:step result)) ":")
        (println "   " (:message result))
        (println)
        (when (:sql result)
          (println "Generated SQL (before error):")
          (println "  " (:sql result))
          (println))
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
