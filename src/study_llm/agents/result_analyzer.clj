(ns study-llm.agents.result-analyzer
  "Result Analyzer Agent - Analyzes and summarizes query results using LLM.
  
  This agent is specialized for interpreting database query results and
  providing human-readable insights and analysis."
  (:require [study-llm.agent :as agent]
            [study-llm.llm :as llm]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]))

(defn create-analysis-prompt
  "Create a prompt for the LLM to analyze query results.
  This helps the LLM provide insights and summaries of the data."
  [user-question query-results]
  (str "You are a data analyst. A user asked: \"" user-question "\"\n\n"
       "The database returned the following results:\n"
       (json/generate-string query-results {:pretty true})
       "\n\n"
       "Please provide a clear, concise summary and any insights about this data. "
       "Be specific with numbers and facts. If asked to summarize, focus on key findings."))

(defn analyze-results-tool
  "Tool for analyzing query results using LLM."
  []
  (agent/create-tool
    :analyze-results
    "Analyzes database query results and provides insights"
    (fn [question context]
      (let [results (:query-results context)
            prompt (create-analysis-prompt question results)
            result (llm/generate-completion prompt :temperature 0.3)]
        (if (= :success (:status result))
          {:status :success
           :result (:response result)
           :updated-context {:analysis (:response result)}}
          result)))))

(defn create-result-analyzer-agent
  "Create the Result Analyzer agent for query result interpretation.
  
  This agent specializes in analyzing database query results and providing
  human-readable insights. It uses the LLM with a slightly higher temperature
  (0.3) for more creative and engaging analysis while remaining factual.
  
  Capabilities:
  - Summarize query results in natural language
  - Identify trends and patterns
  - Provide context and insights
  - Highlight key findings
  - Answer follow-up questions about results"
  []
  (let [tools {:analyze (analyze-results-tool)}
        memory (agent/create-memory :conversation)]
    (agent/create-llm-agent
      "result-analyzer"
      "Analyzes and summarizes database query results"
      tools
      :memory memory
      :config {:temperature 0.3
               :model "llama2"})))
