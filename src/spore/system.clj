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

;; Testing
(defn generate-dependency-graph []
  (let [model-namespaces (filter (fn [ns] (.startsWith (str (.getName ns)) "spore.model.")) (all-ns))
        keyvals (->> model-namespaces
                     (map (fn [model-ns]
                            (let [exports (var-get (get (ns-publics (.getName model-ns)) 'exports))
                                  spore-class (:class exports)]
                              (vector (.ident spore-class) spore-class))))
                     (reduce into []))]

    (->> (apply array-map keyvals)
         (map (fn [[key value]]
                (assoc {} key (.-dependencies value))))
         (reduce merge {}))))

(defn generate-keyvals []
  (let [model-namespaces (filter (fn [ns] (.startsWith (str (.getName ns)) "spore.model.")) (all-ns))
        keyvals (->> model-namespaces
                     (map (fn [model-ns]
                            (let [exports (var-get (get (ns-publics (.getName model-ns)) 'exports))
                                  spore-class (:class exports)]
                              (vector (.ident spore-class) spore-class))))
                     (reduce into []))]

    keyvals))

(defn generate-system [component-graph dependency-graph]
  (let [spore-component-keyvals (generate-keyvals)
        spore-dependency-graph (generate-dependency-graph)
        composite-keyvals (reduce into spore-component-keyvals (vec component-graph))
        composite-dependency-graph (merge dependency-graph spore-dependency-graph)]
    
    (-> (apply component/system-map composite-keyvals)
        (component/system-using composite-dependency-graph))))
