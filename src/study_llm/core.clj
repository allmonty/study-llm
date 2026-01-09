(ns study-llm.core
  "Main entry point for the LLM-powered database chat application."
  (:require [study-llm.db :as db]
            [study-llm.llm :as llm]
            [study-llm.chat :as chat]
            [clojure.tools.logging :as log])
  (:gen-class))

(defn check-dependencies
  "Check if all required services are running and accessible."
  []
  (println "🔍 Checking system dependencies...")
  (println)
  
  ;; Check database
  (print "  PostgreSQL database... ")
  (flush)
  (let [db-status (db/test-connection)]
    (if (= :success (:status db-status))
      (println "✅ Connected")
      (do
        (println "❌ Failed")
        (println "     Error:" (:message db-status))
        (println "     Make sure PostgreSQL is running (docker-compose up -d postgres)"))))
  
  ;; Check Ollama
  (print "  Ollama LLM service... ")
  (flush)
  (let [ollama-status (llm/check-ollama-health)]
    (if (= :healthy (:status ollama-status))
      (do
        (println "✅ Running")
        (when-let [models (:models ollama-status)]
          (println "     Available models:" (count models))))
      (do
        (println "❌ Not accessible")
        (println "     Error:" (:message ollama-status))
        (println "     Make sure Ollama is running (docker-compose up -d ollama)")
        (println "     Then run: docker exec -it study-llm-ollama ollama pull llama2"))))
  
  (println))

(defn ensure-model-available
  "Ensure the required LLM model is available in Ollama."
  []
  (println "🤖 Checking for required model...")
  (let [health (llm/check-ollama-health)]
    (if (= :healthy (:status health))
      (let [models (:models health)
            model-names (set (map :name models))
            required-model "llama2"]
        (if (contains? model-names required-model)
          (do
            (println "✅ Model" required-model "is available")
            true)
          (do
            (println "⚠️  Model" required-model "not found")
            (println "📥 Pulling model (this may take a few minutes)...")
            (llm/pull-model required-model)
            (println "✅ Model ready")
            true)))
      (do
        (println "❌ Cannot check models - Ollama not accessible")
        false))))

(defn -main
  "Application entry point."
  [& args]
  (println)
  (println "╔════════════════════════════════════════════════════════════╗")
  (println "║   LLM-Powered Database Chat System - Study Project        ║")
  (println "║   Using: Clojure + PostgreSQL + Ollama (Local LLM)        ║")
  (println "╚════════════════════════════════════════════════════════════╝")
  (println)
  
  ;; Check if services are running
  (check-dependencies)
  
  ;; Ensure model is available (this might take time on first run)
  (when (ensure-model-available)
    (println)
    (println "✅ All systems ready!")
    (println)
    
    ;; Start the chat interface
    (try
      (chat/start-chat)
      (catch Exception e
        (log/error e "Error in chat system")
        (println)
        (println "❌ An error occurred:" (.getMessage e))
        (println)
        (println "Please check the logs and try again."))
      (finally
        ;; Cleanup
        (db/stop-db-pool!)
        (println)
        (println "🛑 System shutdown complete")))))
