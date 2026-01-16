(ns study-llm.agent
  "Agentic framework for coordinating specialized AI agents.
  
  This framework is inspired by Microsoft Semantic Kernel and AutoGen principles,
  but implemented natively in Clojure to leverage functional programming patterns
  and JVM performance.
  
  Key Concepts:
  - Agent: An autonomous entity with specific capabilities and tools
  - Tool: A function that an agent can invoke to accomplish tasks
  - Orchestrator: Coordinates multiple agents to accomplish complex goals
  - Memory: Maintains context and conversation history across interactions
  
  The implementation is split across multiple namespaces for better organization:
  - study-llm.agent.protocol - Core agent protocol
  - study-llm.agent.tools - Tool creation and invocation
  - study-llm.agent.memory - Memory management
  - study-llm.agent.selection - Tool selection strategies
  - study-llm.agent.implementations - Agent implementations
  - study-llm.agent.orchestrator - Multi-agent orchestration"
  (:require [study-llm.agent.protocol :as protocol]
            [study-llm.agent.tools :as tools]
            [study-llm.agent.memory :as memory]
            [study-llm.agent.selection :as selection]
            [study-llm.agent.implementations :as impl]
            [study-llm.agent.orchestrator :as orch]))

;; ============================================================================
;; Re-export Protocol
;; ============================================================================

(def Agent protocol/Agent)
(def execute protocol/execute)

;; ============================================================================
;; Re-export Tool Functions
;; ============================================================================

(def create-tool tools/create-tool)
(def create-sub-agent-tool tools/create-sub-agent-tool)
(def invoke-tool tools/invoke-tool)

;; ============================================================================
;; Re-export Memory Functions
;; ============================================================================

(def create-memory memory/create-memory)
(def add-to-memory memory/add-to-memory)
(def get-memory memory/get-memory)
(def clear-memory memory/clear-memory)

;; ============================================================================
;; Re-export Selection Functions (internal use)
;; ============================================================================

(def create-tool-selection-prompt selection/create-tool-selection-prompt)
(def select-tool-with-llm selection/select-tool-with-llm)
(def select-tool selection/select-tool)

;; ============================================================================
;; Re-export Agent Implementations
;; ============================================================================

(def LLMAgent impl/LLMAgent)
(def create-llm-agent impl/create-llm-agent)
(def DatabaseAgent impl/DatabaseAgent)
(def create-database-agent impl/create-database-agent)

;; ============================================================================
;; Re-export Orchestrator Functions
;; ============================================================================

(def create-orchestrator orch/create-orchestrator)
(def orchestrate-sequential orch/orchestrate-sequential)
(def orchestrate orch/orchestrate)
(def create-planner orch/create-planner)

;; ============================================================================
;; Example Usage (for documentation)
;; ============================================================================

(comment
  ;; Example usage of the agentic framework
  
  ;; Create tools
  (def sql-tool (create-tool
                  :generate-sql
                  "Converts natural language to SQL queries"
                  (fn [question schema] {:sql "SELECT * FROM customers"})))
  
  ;; Create agents
  (def sql-agent (create-llm-agent
                   "sql-generator"
                   "Converts natural language questions to SQL"
                   {:generate sql-tool}
                   :config {:temperature 0.1}))
  
  (def db-agent (create-database-agent
                  "database-executor"
                  "Executes SQL queries against PostgreSQL"
                  {:query (create-tool :execute-query "Execute SQL" identity)}))
  
  ;; Create orchestrator
  (def pipeline (create-orchestrator
                  [sql-agent db-agent]
                  :strategy :sequential))
  
  ;; Execute pipeline
  (orchestrate pipeline
              "How many customers do we have?"
              {:schema [...]}))
