(ns study-llm.chat
  "Terminal-based chat interface for interacting with the LLM-powered database system.
  
  This module now uses the agentic framework to orchestrate specialized agents:
  - SQL Generator Agent: Converts questions to SQL
  - Database Executor Agent: Executes queries
  - Result Analyzer Agent: Interprets and explains results"
  (:require [study-llm.agent :as agent]
            [study-llm.agents.sql-generator :as sql-gen]
            [study-llm.agents.database-executor :as db-exec]
            [study-llm.agents.result-analyzer :as analyzer]
            [study-llm.db :as db]
            [clojure.string :as str]
            [clojure.tools.logging :as log]))

(defn print-separator []
  (println (str (apply str (repeat 80 "-")))))

(defn print-welcome []
  (println)
  (print-separator)
  (println "  🤖 LLM-Powered Database Chat System (Multi-Agent Architecture)")
  (print-separator)
  (println)
  (println "  Welcome! This system uses specialized AI agents working together:")
  (println "    • SQL Generator Agent - Converts questions to SQL")
  (println "    • Database Executor Agent - Runs queries safely")
  (println "    • Result Analyzer Agent - Interprets and explains data")
  (println)
  (println "  The agents coordinate through an orchestrator for complex tasks.")
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
  (println "How it works - Multi-Agent Architecture:")
  (println "  This system uses specialized AI agents coordinated by an orchestrator:")
  (println)
  (println "  1. SQL Generator Agent:")
  (println "     - Converts your natural language question to SQL")
  (println "     - Uses LLM (Llama2) with low temperature for accuracy")
  (println "     - Understands database schema and relationships")
  (println)
  (println "  2. Database Executor Agent:")
  (println "     - Executes SQL queries against PostgreSQL")
  (println "     - Manages connection pooling")
  (println "     - Handles errors and validates results")
  (println)
  (println "  3. Result Analyzer Agent:")
  (println "     - Interprets query results")
  (println "     - Provides insights and summaries")
  (println "     - Uses LLM with higher temperature for creativity")
  (println)
  (println "  The orchestrator coordinates these agents in sequence,")
  (println "  passing context between them for optimal results.")
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

;; ============================================================================
;; Agent Instance Management
;; ============================================================================
;; Agents are created once and reused to avoid overhead of recreation
;; (memory stores, configurations, etc.)

(defonce agent-instances
  "Atom holding singleton agent instances for reuse."
  (atom {}))

(defn get-or-create-agent
  "Get an agent from cache or create it if it doesn't exist.
  This ensures agents are created once and reused."
  [agent-key create-fn]
  (if-let [existing-agent (get @agent-instances agent-key)]
    existing-agent
    (let [new-agent (create-fn)]
      (swap! agent-instances assoc agent-key new-agent)
      new-agent)))

(defn process-question
  "Process a user question using the multi-agent orchestration framework.
  
  This function demonstrates the agentic architecture where specialized agents
  work together to accomplish a complex task (text-to-SQL-to-analysis pipeline).
  
  NOTE: This implementation manually executes each agent step-by-step to provide
  clear visibility into the agent workflow for educational purposes. For a more
  concise implementation, use the orchestrator directly (see commented alternative below).
  
  NOTE: Agents are created once and cached to avoid overhead on each question.
  
  Agent Pipeline:
  1. SQL Generator Agent - Converts natural language to SQL
  2. Database Executor Agent - Executes the SQL query
  3. Result Analyzer Agent - Interprets and explains results"
  [question schema-info]
  (println)
  (println "🤖 Multi-Agent System Processing...")
  (println)
  
  ;; Get or create specialized agents (cached for reuse)
  (let [sql-agent (get-or-create-agent :sql-generator sql-gen/create-sql-generator-agent)
        db-agent (get-or-create-agent :database-executor db-exec/create-database-executor-agent)
        analyzer-agent (get-or-create-agent :result-analyzer analyzer/create-result-analyzer-agent)
        
        ;; Create orchestrator (note: not used in this implementation, see alternative below)
        orchestrator (agent/create-orchestrator
                      [sql-agent db-agent analyzer-agent]
                      :strategy :sequential)
        
        ;; Initial context with schema information
        initial-context {:schema schema-info
                        :question question}]
    
    ;; Step 1: SQL Generator Agent
    (println "Step 1: SQL Generator Agent - Converting question to SQL...")
    (let [sql-result (agent/execute sql-agent question initial-context)]
      (if (= :success (:status sql-result))
        (let [sql (:result sql-result)]
          (println "Generated SQL:")
          (println "  " sql)
          (println)
          
          ;; Step 2: Database Executor Agent
          (println "Step 2: Database Executor Agent - Executing query...")
          (let [db-context (merge initial-context (:updated-context sql-result))
                db-result (agent/execute db-agent sql db-context)]
            (if (= :success (:status db-result))
              (let [results (:result db-result)]
                (println "✅ Query executed successfully!")
                (println "Found" (count results) "result(s)")
                (println)
                
                ;; Step 3: Result Analyzer Agent
                (println "Step 3: Result Analyzer Agent - Analyzing results...")
                (let [analysis-context (merge db-context (:updated-context db-result))
                      analysis-result (agent/execute analyzer-agent question analysis-context)]
                  (if (= :success (:status analysis-result))
                    (do
                      (println)
                      (print-separator)
                      (println "📊 Analysis:")
                      (println)
                      (println (:result analysis-result))
                      (println)
                      (print-separator))
                    (do
                      (println "❌ Error analyzing results:" (:message analysis-result))
                      (println)
                      (println "Raw results:")
                      (doseq [row (take 10 results)]
                        (println row))))))
              (do
                (println "❌ Error executing query:" (:message db-result))
                (println)
                (println "The SQL might be incorrect. Try rephrasing your question.")))))
        (do
          (println "❌ Error generating SQL:" (:message sql-result))
          (println)
          (println "Please try rephrasing your question or check if Ollama is running."))))))

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

;; ============================================================================
;; Alternative Implementation Using Orchestrator Directly
;; ============================================================================
;; This demonstrates using the orchestrator's built-in sequential execution,
;; which is more concise but provides less visibility into each step.
;;
;; To use this instead of the manual step-by-step approach above, replace
;; the process-question function with this implementation:
;;
;; (defn process-question-with-orchestrator
;;   "Alternative implementation using orchestrator directly."
;;   [question schema-info]
;;   (println)
;;   (println "🤖 Multi-Agent System Processing...")
;;   (println)
;;   
;;   (let [sql-agent (sql-gen/create-sql-generator-agent)
;;         db-agent (db-exec/create-database-executor-agent)
;;         analyzer-agent (analyzer/create-result-analyzer-agent)
;;         orchestrator (agent/create-orchestrator
;;                       [sql-agent db-agent analyzer-agent]
;;                       :strategy :sequential)
;;         initial-context {:schema schema-info}]
;;     
;;     ;; Use orchestrator to execute all agents
;;     (let [result (agent/orchestrate orchestrator question initial-context)]
;;       (if (= :success (:status result))
;;         (let [final-result (last (:results result))]
;;           (println)
;;           (print-separator)
;;           (println "📊 Analysis:")
;;           (println)
;;           (println (:result final-result))
;;           (println)
;;           (print-separator))
;;         (do
;;           (println "❌ Error in agent pipeline:" (:error result))
;;           (println "Please try rephrasing your question."))))))
