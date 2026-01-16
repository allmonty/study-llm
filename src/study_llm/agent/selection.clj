(ns study-llm.agent.selection
  "Tool selection strategies for agents."
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [study-llm.llm :as llm]))

;; ============================================================================
;; Tool Selection
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
  (when (empty? tools)
    (throw (ex-info "Cannot select tool: no tools available" {:input input})))
  
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
