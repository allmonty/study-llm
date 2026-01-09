(ns study-llm.agents.analysis-agent
  "Analysis Agent - Analyzes and summarizes query results.
  
  This agent uses the LLM to provide human-readable analysis and insights
  about database query results."
  (:require [study-llm.agent :as agent]
            [study-llm.tools :as tools]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]))

(defn create-analysis-prompt
  "Create a prompt for analyzing query results"
  [user-question query-results]
  (str "You are a data analyst. A user asked: \"" user-question "\"\n\n"
       "The database returned the following results:\n"
       (json/generate-string query-results {:pretty true})
       "\n\n"
       "Please provide a clear, concise summary and any insights about this data. "
       "Be specific with numbers and facts. If asked to summarize, focus on key findings."))

(defn analysis-agent-execute
  "Execute function for the analysis agent"
  [agent input context]
  (try
    (let [question (:question input)
          results (:results input)]
      
      (if-not (and question results)
        (agent/format-agent-error "Missing question or results" context)
        
        (let [;; Generate analysis using LLM tool
              prompt (create-analysis-prompt question results)
              llm-tool (tools/get-tool :llm :generate-text)
              llm-result (agent/invoke-tool llm-tool
                                           {:prompt prompt
                                            :temperature 0.3}
                                           context)]
          
          (if (= :success (:status llm-result))
            (let [analysis (:result llm-result)
                  ;; Log this step in context
                  context (agent/log-agent-step context
                                                (agent/get-name agent)
                                                "analyze-results"
                                                {:question question
                                                 :result-count (count results)})]
              (agent/format-agent-response analysis context))
            (agent/format-agent-error
              (str "Failed to analyze results: " (:message llm-result))
              context)))))
    (catch Exception e
      (log/error e "Analysis Agent execution failed")
      (agent/format-agent-error (.getMessage e) context))))

(defn create-analysis-agent
  "Create a new analysis agent"
  []
  (agent/create-agent
    "result-analyzer"
    "Analyzes and summarizes database query results using LLM"
    [(tools/get-tool :llm :generate-text)]
    analysis-agent-execute))
