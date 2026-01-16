(ns study-llm.agent.protocol
  "Core agent protocol definition.")

;; ============================================================================
;; Agent Protocol
;; ============================================================================

(defprotocol Agent
  "Protocol defining the interface for all agents in the system."
  (execute [this input context]
    "Execute the agent's primary task with the given input and context.
    Returns a map with :status, :result, and optionally :updated-context"))
