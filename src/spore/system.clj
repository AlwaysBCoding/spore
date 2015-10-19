(ns spore.system
  (:require [com.stuartsierra.component :as component]))

(defn generate-system-map [keyvals]
  (let [dependency-graph (->> (apply array-map keyvals)
                              (map (fn [[key value]]
                                     (assoc {} key (.-dependencies value))))
                              (reduce merge {}))]

    (-> (apply component/system-map keyvals)
        (component/system-using
         dependency-graph))))

(defn generate-spore-system-map []
  (let [model-namespaces (filter (fn [ns] (.startsWith (str (.getName ns)) "spore.model.")) (all-ns))
        keyvals (->> model-namespaces
                     (map (fn [model-ns]
                            (let [exports (var-get (get (ns-publics (.getName model-ns)) 'exports))
                                  spore-class (:class exports)]
                              (vector (.ident spore-class) spore-class))))
                     (reduce into []))]

    (generate-system-map keyvals)))
