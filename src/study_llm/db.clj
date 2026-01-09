(ns study-llm.db
  "Database connection and query utilities for PostgreSQL."
  (:require [next.jdbc :as jdbc]
            [next.jdbc.connection :as connection]
            [clojure.tools.logging :as log])
  (:import [com.zaxxer.hikari HikariDataSource]))

;; Database configuration
;; NOTE: For a learning/study project, credentials are intentionally hard-coded for simplicity.
;; Using explicit JDBC URL with embedded credentials to completely bypass environment variables.
;; This ensures the connection always uses the configured credentials regardless of any
;; PGUSER, PGPASSWORD, PGHOST, or other PostgreSQL environment variables.
;;
;; The credentials are embedded in the URL to prevent HikariCP from checking environment
;; variables during connection initialization.
;;
;; In production, use environment variables:
;;   {:jdbcUrl (str "jdbc:postgresql://" (System/getenv "DB_HOST") ":" 
;;                  (System/getenv "DB_PORT") "/" (System/getenv "DB_NAME")
;;                  "?user=" (System/getenv "DB_USER")
;;                  "&password=" (System/getenv "DB_PASSWORD"))}
(def db-config
  {:jdbcUrl "jdbc:postgresql://localhost:5432/studydb?user=studyuser&password=studypass"})

;; Connection pool (HikariCP for production-grade connection pooling)
(defonce datasource (atom nil))

(defn start-db-pool!
  "Initialize the database connection pool.
  HikariCP is used because it's the fastest, most production-ready connection pool."
  []
  (when-not @datasource
    (log/info "Starting database connection pool...")
    (reset! datasource (connection/->pool HikariDataSource db-config))
    (log/info "Database connection pool started successfully")))

(defn stop-db-pool!
  "Close the database connection pool."
  []
  (when @datasource
    (log/info "Stopping database connection pool...")
    (.close @datasource)
    (reset! datasource nil)
    (log/info "Database connection pool stopped")))

(defn get-datasource
  "Get the current datasource, starting the pool if necessary."
  []
  (when-not @datasource
    (start-db-pool!))
  @datasource)

(defn execute-query!
  "Execute a SQL query and return results.
  This is a wrapper around next.jdbc/execute! for convenience."
  [sql-vec]
  (try
    (let [ds (get-datasource)]
      (jdbc/execute! ds sql-vec))
    (catch Exception e
      (log/error e "Error executing query:" sql-vec)
      {:error (.getMessage e)})))

(defn get-schema-info
  "Get database schema information for the LLM to understand the structure.
  This returns table names, columns, and their types."
  []
  (let [tables-query ["SELECT table_name 
                       FROM information_schema.tables 
                       WHERE table_schema = 'public' 
                       ORDER BY table_name"]
        tables (execute-query! tables-query)]
    (mapv
      (fn [table]
        (let [table-name (:tables/table_name table)
              columns-query ["SELECT column_name, data_type, is_nullable
                             FROM information_schema.columns
                             WHERE table_schema = 'public' 
                             AND table_name = ?
                             ORDER BY ordinal_position" table-name]
              columns (execute-query! columns-query)]
          {:table table-name
           :columns (mapv (fn [col]
                           {:name (:columns/column_name col)
                            :type (:columns/data_type col)
                            :nullable (:columns/is_nullable col)})
                         columns)}))
      tables)))

(defn get-sample-data
  "Get a few sample rows from each table to help the LLM understand the data."
  []
  (let [tables ["customers" "products" "orders" "order_items"]]
    (into {}
          (map (fn [table]
                 [table (execute-query! [(str "SELECT * FROM " table " LIMIT 3")])])
               tables))))

(defn test-connection
  "Test the database connection."
  []
  (try
    (let [result (execute-query! ["SELECT version()"])]
      (if (:error result)
        {:status :error :message (:error result)}
        {:status :success :version (-> result first :version)}))
    (catch Exception e
      {:status :error :message (.getMessage e)})))
