(ns study-llm.agent.memory
  "Memory management for maintaining agent context and conversation history.")

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
