(ns study-llm.agent
  "Agentic framework for coordinating specialized AI agents.
  
  This framework is inspired by Microsoft Semantic Kernel and AutoGen principles,
  but implemented natively in Clojure to leverage functional programming patterns
  and JVM performance.
  
  Key Concepts:
  - Agent: An autonomous entity with specific capabilities and tools
  - Tool: A function that an agent can invoke to accomplish tasks
  - Orchestrator: Coordinates multiple agents to accomplish complex goals
  - Memory: Maintains context and conversation history across interactions"
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [study-llm.llm :as llm]))

;; ============================================================================
;; Agent Protocol
;; ============================================================================

(defprotocol Agent
  "Protocol defining the interface for all agents in the system."
  (execute [this input context]
    "Execute the agent's primary task with the given input and context.
    Returns a map with :status, :result, and optionally :updated-context"))

;; ============================================================================
;; Tool Registry
;; ============================================================================

(defn create-tool
  "Create a tool that agents can use.
  
  A tool is a named function with metadata describing its capabilities.
  
  Parameters:
  - name: Unique identifier for the tool
  - description: What the tool does (used for agent planning)
  - fn: The actual function to execute
  - schema: Optional parameter schema for validation"
  [name description f & {:keys [schema]}]
  {:name name
   :description description
   :fn f
   :schema schema})

(defn invoke-tool
  "Invoke a tool with the given arguments."
  [tool & args]
  (try
    (apply (:fn tool) args)
    (catch Exception e
      (log/error e "Error invoking tool:" (:name tool))
      {:status :error
       :message (.getMessage e)
       :tool (:name tool)})))

;; ============================================================================
;; Memory Management
;; ============================================================================

(defn create-memory
  "Create a new memory store for maintaining agent context.
  
  Memory types:
  - :conversation - Stores the full conversation history
  - :semantic - Stores embeddings for semantic search (future enhancement)
  - :working - Temporary working memory for current task"
  [type]
  {:type type
   :store (atom [])
   :created-at (java.time.Instant/now)})

(defn add-to-memory
  "Add an entry to the memory store."
  [memory entry]
  (swap! (:store memory) conj entry)
  memory)

(defn get-memory
  "Retrieve memory entries, optionally filtered."
  [memory & {:keys [limit filter-fn]
             :or {limit Integer/MAX_VALUE
                  filter-fn (constantly true)}}]
  (->> @(:store memory)
       (filter filter-fn)
       (take-last limit)))

(defn clear-memory
  "Clear the memory store."
  [memory]
  (reset! (:store memory) [])
  memory)

;; ============================================================================
;; Agent Implementations
;; ============================================================================

(defn create-tool-selection-prompt
  "Create a prompt for the LLM to select the appropriate tool.
  
  The prompt includes:
  - The user's input
  - Available tools with descriptions
  - Instructions to return only the tool name"
  [tools input context]
  (str "You are a tool selection assistant. Your job is to choose the most appropriate tool "
       "for the given user input.\n\n"
       "Available tools:\n"
       (str/join "\n"
                 (map (fn [[tool-key tool]]
                        (str "- " (name tool-key) ": " (:description tool)))
                      tools))
       "\n\n"
       "User input: " input "\n\n"
       "Instructions:\n"
       "1. Analyze the user input carefully\n"
       "2. Choose the SINGLE most appropriate tool from the list above\n"
       "3. Respond with ONLY the tool name (e.g., 'add' or 'multiply'), nothing else\n"
       "4. Do not include explanations, punctuation, or additional text\n\n"
       "Selected tool:"))

(defn select-tool-with-llm
  "Use the LLM to select the appropriate tool based on input and context."
  [tools input context config]
  (log/info "Using LLM to select tool for input:" input)
  (let [prompt (create-tool-selection-prompt tools input context)
        llm-result (llm/generate-completion prompt :temperature 0.1)
        primary-fallback (get tools (or (:primary-tool config) (first (keys tools))))]
    (if (= :success (:status llm-result))
      (let [selected-name (str/trim (str/lower-case (:response llm-result)))
            ;; Try to find the tool by matching the LLM's response to tool names
            selected-tool (or
                           ;; Exact match (as keyword)
                           (get tools (keyword selected-name))
                           ;; Try to find by name string match
                           (second (first (filter (fn [[k _]] 
                                                    (= selected-name (str/lower-case (name k))))
                                                  tools)))
                           ;; Fallback to primary
                           primary-fallback)]
        (log/info "LLM selected tool:" selected-name "-> resolved to:" (:name selected-tool))
        selected-tool)
      (do
        (log/warn "LLM tool selection failed, using primary tool. Error:" (:message llm-result))
        primary-fallback))))

(defn select-tool
  "Select the appropriate tool based on input and available tools.
  
  Selection strategies:
  - :primary - Use the configured primary tool (default)
  - :llm - Use LLM to intelligently select the best tool based on input
  - :function - Use a custom function to select the tool"
  [tools input context config]
  (let [strategy (or (:tool-selection-strategy config) :primary)]
    (case strategy
      :primary
      ;; Use the configured primary tool or first tool
      (let [primary-key (or (:primary-tool config) (first (keys tools)))]
        (get tools primary-key))
      
      :llm
      ;; Use LLM to select the appropriate tool
      (select-tool-with-llm tools input context config)
      
      :function
      ;; Use a custom selection function
      (if-let [select-fn (:tool-selector-fn config)]
        (select-fn tools input context)
        ;; Fallback to primary
        (get tools (or (:primary-tool config) (first (keys tools)))))
      
      ;; Default to primary strategy
      (get tools (or (:primary-tool config) (first (keys tools)))))))

(defrecord LLMAgent [name description tools memory config]
  Agent
  (execute [this input context]
    (log/info "LLMAgent executing:" name)
    ;; Select the appropriate tool based on configuration
    (let [tool-fn (select-tool tools input context config)
          result (if tool-fn
                   (let [tool-result (invoke-tool tool-fn input context)]
                     (assoc tool-result :tool-used (:name tool-fn)))
                   {:status :error
                    :message (str "No tool found for agent " name ". Available tools: " (keys tools))})]
      ;; Store interaction in memory
      (when memory
        (add-to-memory memory {:input input
                              :result result
                              :timestamp (java.time.Instant/now)
                              :agent name}))
      result)))

(defn create-llm-agent
  "Create an LLM-based agent with specific capabilities.
  
  Parameters:
  - name: Agent identifier
  - description: What this agent does
  - tools: Map of tool-name -> tool for this agent
  - config: Agent-specific configuration (e.g., temperature, prompts)"
  [name description tools & {:keys [memory config]
                             :or {memory (create-memory :conversation)
                                  config {}}}]
  (->LLMAgent name description tools memory config))

(defrecord DatabaseAgent [name description tools memory config]
  Agent
  (execute [this input context]
    (log/info "DatabaseAgent executing:" name)
    ;; Select the appropriate tool based on configuration
    (let [tool-fn (select-tool tools input context config)
          result (if tool-fn
                   (let [tool-result (invoke-tool tool-fn input context)]
                     (assoc tool-result :tool-used (:name tool-fn)))
                   {:status :error
                    :message (str "No tool found for agent " name ". Available tools: " (keys tools))})]
      ;; Store interaction in memory
      (when memory
        (add-to-memory memory {:input input
                              :result result
                              :timestamp (java.time.Instant/now)
                              :agent name}))
      result)))

(defn create-database-agent
  "Create a database agent for executing queries and managing data.
  
  Parameters:
  - name: Agent identifier
  - description: What this agent does
  - tools: Map of tool-name -> tool for database operations"
  [name description tools & {:keys [memory config]
                             :or {memory (create-memory :conversation)
                                  config {}}}]
  (->DatabaseAgent name description tools memory config))

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
