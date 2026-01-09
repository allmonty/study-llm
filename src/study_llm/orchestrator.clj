(ns study-llm.orchestrator
  "Orchestrator for coordinating multiple agents to answer user questions.
  
  The orchestrator implements the planning and execution pattern from Microsoft's
  Agentic Framework. It coordinates multiple specialized agents to complete
  complex tasks by:
  
  1. Planning - Determining which agents to use and in what order
  2. Execution - Running agents in sequence, passing context between them
  3. Monitoring - Tracking execution and handling errors
  4. Result aggregation - Combining results from multiple agents
  
  This pattern enables:
  - Separation of concerns (each agent has a specific role)
  - Composability (agents can be combined in different ways)
  - Extensibility (new agents can be added easily)
  - Maintainability (each agent can be tested independently)"
  (:require [study-llm.agent :as agent]
            [study-llm.agents.sql-agent :as sql-agent]
            [study-llm.agents.query-agent :as query-agent]
            [study-llm.agents.analysis-agent :as analysis-agent]
            [clojure.tools.logging :as log]))

;; ============================================================================
;; Orchestrator Agent
;; ============================================================================

(defn execute-agent-chain
  "Execute a chain of agents, passing context and results between them.
  
  This is the core orchestration logic that:
  1. Executes agents in sequence
  2. Passes context between agents
  3. Handles errors at each step
  4. Collects results from all agents"
  [agents input context]
  (loop [remaining-agents agents
         current-context context
         results []]
    (if (empty? remaining-agents)
      ;; All agents executed successfully
      {:status :success
       :results results
       :context current-context}
      
      ;; Execute next agent
      (let [current-agent (first remaining-agents)
            agent-name (agent/get-name current-agent)
            agent-input (or (get input agent-name) input)]
        
        (log/info "Orchestrator executing agent:" agent-name)
        
        (let [agent-result (agent/execute current-agent agent-input current-context)]
          (if (= :success (:status agent-result))
            ;; Agent succeeded, continue with next agent
            (recur (rest remaining-agents)
                   (:context agent-result)
                   (conj results {:agent agent-name
                                  :result (:result agent-result)}))
            ;; Agent failed, stop execution
            (do
              (log/error "Agent" agent-name "failed:" (:message agent-result))
              {:status :error
               :failed-agent agent-name
               :message (:message agent-result)
               :results results
               :context current-context})))))))

(defn orchestrate-question-answering
  "Orchestrate the complete question-answering flow.
  
  Flow:
  1. SQL Agent: Convert question to SQL
  2. Query Agent: Execute SQL against database
  3. Analysis Agent: Analyze and summarize results"
  [user-question]
  (let [;; Create agents
        sql-agent (sql-agent/create-sql-agent)
        query-agent (query-agent/create-query-agent)
        analysis-agent (analysis-agent/create-analysis-agent)
        
        ;; Create initial context
        context (agent/create-context {:user-question user-question})
        
        ;; Execute SQL agent
        _ (log/info "Step 1: Generating SQL from question")
        sql-result (agent/execute sql-agent {:question user-question} context)]
    
    (if (not= :success (:status sql-result))
      ;; SQL generation failed
      {:status :error
       :step :sql-generation
       :message (:message sql-result)}
      
      ;; SQL generated successfully, execute query
      (let [sql (:result sql-result)
            context (:context sql-result)
            
            _ (log/info "Step 2: Executing SQL query")
            query-result (agent/execute query-agent {:sql sql} context)]
        
        (if (not= :success (:status query-result))
          ;; Query execution failed
          {:status :error
           :step :query-execution
           :sql sql
           :message (:message query-result)}
          
          ;; Query executed successfully, analyze results
          (let [results (:result query-result)
                context (:context query-result)
                
                _ (log/info "Step 3: Analyzing results")
                analysis-result (agent/execute analysis-agent
                                              {:question user-question
                                               :results results}
                                              context)]
            
            (if (not= :success (:status analysis-result))
              ;; Analysis failed
              {:status :error
               :step :analysis
               :sql sql
               :results results
               :message (:message analysis-result)}
              
              ;; Complete success
              {:status :success
               :sql sql
               :results results
               :analysis (:result analysis-result)
               :context (:context analysis-result)})))))))

(defn create-orchestrator-agent
  "Create an orchestrator agent that coordinates the question-answering flow.
  
  This agent is a meta-agent that manages other agents to complete
  the full task of answering database questions."
  []
  (agent/create-agent
    "orchestrator"
    "Coordinates SQL generation, query execution, and result analysis agents"
    []  ; Orchestrator doesn't use tools directly, it uses other agents
    (fn [agent input context]
      (let [question (:question input)
            result (orchestrate-question-answering question)]
        (if (= :success (:status result))
          (agent/format-agent-response result context)
          (agent/format-agent-error (:message result) context))))))

;; ============================================================================
;; Public API
;; ============================================================================

(defn answer-question
  "Main entry point for answering user questions using the agent system.
  
  This function orchestrates multiple agents to:
  1. Convert the question to SQL
  2. Execute the query
  3. Analyze and summarize the results
  
  Returns a map with:
  - :status - :success or :error
  - :sql - Generated SQL query
  - :results - Query results
  - :analysis - LLM analysis of results
  - :message - Error message (if status is :error)"
  [user-question]
  (orchestrate-question-answering user-question))
