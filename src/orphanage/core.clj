(ns orphanage.core
  (:require [clojure.tools.reader :as r]
            [clojure.tools.namespace.parse :as parse]))

(defn ns-from-ref [ref]
  (cond (list? ref) (ns-from-ref (first ref))
        (vector? ref) (ns-from-ref (first ref))
        :else ref))

(defn find-nodes [root]
  (let [dir (clojure.java.io/file root)]
    (file-seq dir)))

(defn find-files [root]
  (filter #(.isFile %) (find-nodes root)))

(defn find-clj-files [root]
  (filter #(re-find #"\.clj$" (.getName %)) (find-files root)))

(defn get-namespace [rdr]
  (parse/read-ns-decl rdr))

(defn ns-loaded [namespace state init-fn]
  (init-fn state namespace))

(defn deps-loaded [ns deps state update-fn]
  (loop [deps deps state state]
    (if (empty? deps)
      state
      (recur (rest deps) (update-fn state ns (first deps))))))

(defn visit-files [files init-fn update-fn]
  (loop [file (first files) files (rest files) state {}] 
    (if file
      (let [rdr (-> file (java.io.FileReader.) (java.io.PushbackReader.))
            namespace (get-namespace rdr)
            state (ns-loaded (second namespace) state init-fn)
            deps (parse/deps-from-ns-decl namespace)
            state (deps-loaded (second namespace) deps state update-fn)]
        (recur (first files) (rest files) state))
      state)))

(defn find-orphans [root]
  (let [files (find-clj-files root)
        state (visit-files files 
                           (fn [state ns] (update-in state [ns] #(if % % 0)))
                           (fn [state ns deps] (update-in state [ns] #(if % (inc %) 1))))]
    (->> state
         (filter (fn [[k v]] (and k (= 0 v))))
         (into {}))))

(defn find-refs [root qns]
  (let [files (find-clj-files root)
        state (visit-files files 
                           (fn [state ns] (update-in state [ns] #(if % % [])))
                           (fn [state ns dep] (update-in state [ns] conj dep)))]
    (->> state
         (filter (fn [[k v]] k))
         (filter (fn [[ns refs]]
                   ((set refs) qns)))
         (map first))))
