(ns study-llm.llm
  "LLM integration using Ollama for local inference.
  Ollama is chosen because it runs locally, supports many models, and is production-ready."
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.tools.logging :as log]))

;; Ollama API configuration
;; NOTE: For a learning/study project, configuration is hard-coded for simplicity.
;; In production, use environment variables or configuration files:
;;   {:base-url (or (System/getenv "OLLAMA_URL") "http://localhost:11434")
;;    :model (or (System/getenv "OLLAMA_MODEL") "llama2")
;;    :timeout (Integer/parseInt (or (System/getenv "OLLAMA_TIMEOUT") "60000"))}
(def ollama-config
  {:base-url "http://localhost:11434"
   :model "llama2"  ; Default model, can be changed to others like mistral, codellama
   :timeout 60000}) ; 60 second timeout for LLM responses

(defn check-ollama-health
  "Check if Ollama is running and accessible."
  []
  (try
    (let [response (http/get (str (:base-url ollama-config) "/api/tags")
                             {:as :json
                              :throw-exceptions false})]
      (if (= 200 (:status response))
        {:status :healthy :models (get-in response [:body :models])}
        {:status :unhealthy :error "Ollama not responding"}))
    (catch Exception e
      {:status :error :message (.getMessage e)})))

(defn pull-model
  "Pull a model from Ollama if not already available.
  This is necessary the first time you use a model."
  [model-name]
  (log/info "Pulling model:" model-name)
  (try
    (let [response (http/post (str (:base-url ollama-config) "/api/pull")
                              {:body (json/generate-string {:name model-name})
                               :headers {"Content-Type" "application/json"}
                               :timeout 300000 ; 5 minutes for model download
                               :as :json})]
      (log/info "Model pulled successfully:" model-name)
      {:status :success})
    (catch Exception e
      (log/error e "Error pulling model:" model-name)
      {:status :error :message (.getMessage e)})))

(defn generate-completion
  "Generate a completion from the LLM using Ollama.
  Options:
  - :model - which model to use (default: llama2)
  - :temperature - creativity level 0-1 (default: 0.1 for more factual responses)
  - :stream - whether to stream the response (default: false)"
  [prompt & {:keys [model temperature stream]
             :or {model (:model ollama-config)
                  temperature 0.1
                  stream false}}]
  (try
    (let [request-body {:model model
                       :prompt prompt
                       :stream stream
                       :options {:temperature temperature}}
          response (http/post (str (:base-url ollama-config) "/api/generate")
                             {:body (json/generate-string request-body)
                              :headers {"Content-Type" "application/json"}
                              :timeout (:timeout ollama-config)
                              :as :json})]
      (if (= 200 (:status response))
        {:status :success
         :response (get-in response [:body :response])
         :model model}
        {:status :error
         :message "Unexpected response from Ollama"}))
    (catch Exception e
      (log/error e "Error generating completion")
      {:status :error
       :message (.getMessage e)})))

(defn create-sql-prompt
  "Create a prompt for the LLM to generate SQL from natural language.
  This is the key to making text-to-SQL work effectively."
  [user-question schema-info]
  (str "You are a SQL expert. Given the following database schema and a user question, "
       "generate a SQL query that answers the question. "
       "Return ONLY the SQL query, nothing else. No explanations, no markdown formatting.\n\n"
       "IMPORTANT RULES:\n"
       "1. If the question is not related to the database (e.g., greetings like 'Hello'), return: SELECT 'I can help you query the database. Please ask a question about customers, products, or orders.' AS message\n"
       "2. All columns in SELECT must either be in GROUP BY or use an aggregate function (COUNT, SUM, AVG, etc.)\n"
       "3. Use proper PostgreSQL syntax\n"
       "4. Always use table aliases for clarity\n\n"
       "Database Schema:\n"
       (str/join "\n"
                 (map (fn [table]
                        (str "Table: " (:table table) "\n"
                             "Columns:\n"
                             (str/join "\n"
                                      (map (fn [col]
                                            (str "  - " (:name col) " (" (:type col) ")"))
                                           (:columns table)))))
                      schema-info))
       "\n\n"
       "Table Relationships:\n"
       "- orders.customer_id → customers.id (each order belongs to a customer)\n"
       "- order_items.order_id → orders.id (each order item belongs to an order)\n"
       "- order_items.product_id → products.id (each order item references a product)\n"
       "Note: order_items does NOT have a customer_id column. To get customer info from order_items, "
       "you must JOIN through orders table: order_items → orders → customers\n\n"
       "User Question: " user-question "\n\n"
       "SQL Query:"))

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

(defn generate-sql-from-question
  "Use the LLM to generate SQL from a natural language question."
  [user-question schema-info]
  (let [prompt (create-sql-prompt user-question schema-info)
        result (generate-completion prompt :temperature 0.1)]
    (if (= :success (:status result))
      {:status :success
       :sql (str/trim (:response result))}
      result)))

(defn analyze-results
  "Use the LLM to analyze and summarize query results."
  [user-question results]
  (let [prompt (create-analysis-prompt user-question results)
        result (generate-completion prompt :temperature 0.3)]
    (if (= :success (:status result))
      {:status :success
       :analysis (:response result)}
      result)))
