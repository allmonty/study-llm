(ns study-llm.agent.orchestrator
  "Multi-agent orchestration for coordinating complex workflows."
  (:require [clojure.tools.logging :as log]
            [study-llm.agent.protocol :refer [execute]]
            [study-llm.agent.memory :refer [create-memory add-to-memory]]))

;; ============================================================================
;; Multi-Agent Orchestration
;; ============================================================================

(defn create-orchestrator
  "Create an orchestrator to coordinate multiple agents.
  
  The orchestrator manages the workflow between agents, maintaining context
  and ensuring proper sequencing of operations."
  [agents & {:keys [memory strategy]
             :or {memory (create-memory :conversation)
                  strategy :sequential}}]
  {:agents agents
   :memory memory
   :strategy strategy})

(defn orchestrate-sequential
  "Execute agents in sequential order, passing results between them.
  
  This is the default orchestration strategy for the text-to-SQL pipeline:
  1. SQL Generator Agent creates SQL
  2. Database Agent executes the query
  3. Analyzer Agent interprets results"
  [orchestrator input initial-context]
  (log/info "Starting sequential orchestration")
  (loop [agents (:agents orchestrator)
         context initial-context
         results []]
    (if (empty? agents)
      {:status :success
       :results results
       :final-context context}
      (let [agent (first agents)
            agent-result (execute agent input context)
            updated-context (merge context
                                  (:updated-context agent-result)
                                  {:previous-result (:result agent-result)})]
        ;; Store in orchestrator memory
        (add-to-memory (:memory orchestrator)
                      {:agent-name (:name agent)
                       :result agent-result
                       :timestamp (java.time.Instant/now)})
        
        (if (= :error (:status agent-result))
          ;; Error occurred, stop execution
          {:status :error
           :error agent-result
           :results results
           :context context}
          ;; Continue with next agent
          (recur (rest agents)
                updated-context
                (conj results agent-result)))))))

(defn orchestrate
  "Orchestrate multiple agents to accomplish a task.
  
  Supports different orchestration strategies:
  - :sequential - Execute agents one after another (default)
  - :parallel - Execute agents concurrently (future enhancement)
  - :dynamic - Let LLM decide which agents to use (future enhancement)"
  [orchestrator input context]
  (case (:strategy orchestrator)
    :sequential (orchestrate-sequential orchestrator input context)
    ;; Future: Add :parallel and :dynamic strategies
    (orchestrate-sequential orchestrator input context)))

;; ============================================================================
;; Planning (Future Enhancement)
;; ============================================================================

(defn create-planner
  "Create a planner that breaks down complex tasks into agent steps.
  
  This is a placeholder for future enhancement. A planner could use an LLM
  to dynamically determine which agents to invoke and in what order."
  []
  {:type :static-sequential
   :description "Simple sequential planner for text-to-SQL pipeline"})
