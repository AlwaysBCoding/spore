(ns spore.system
  (:require [com.stuartsierra.component :as component]))

(defn generate-system-map [keyvals]
  (let [dependency-graph (->> (apply array-map keyvals)
                              (map (fn [[key value]]
                                     (assoc {} key (.dependencies value))))
                              (reduce merge {}))]

    (-> (apply component/system-map keyvals)
        (component/system-using
         dependency-graph))))
