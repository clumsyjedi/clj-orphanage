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

(defn ns-loaded [namespace state]
  (update-in state [namespace] #(if % % 0)))

(defn deps-loaded [deps state]
  (if (empty? deps)
    state
    (recur (rest deps) (update-in state [(first deps)] #(if % (inc %) 1)))))

(defn visit-files [file files state]
  (if file
   (let [rdr (-> file (java.io.FileReader.) (java.io.PushbackReader.))
         namespace (get-namespace rdr)
         state (ns-loaded (second namespace) state)
         deps (parse/deps-from-ns-decl namespace)
         state (deps-loaded deps state)]
     (recur (first files) (rest files) state))
   state))

(defn find-orphans [root]
  (let [files (find-clj-files root)
        state (visit-files (first files) (rest files) {})]
    (->> state
         (filter (fn [[k v]] (= 0 v)))
         (remove nil?)
         (into {}))))
