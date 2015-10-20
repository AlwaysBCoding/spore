(ns spore.implementation.instance
  (:require [spore.helpers.resource :as resource-helpers]
            [spore.helpers.util :as util]
            [datomic.api :as d]))

(defn ident
  ([self]
   (first (keys (.-manifest self)))))

(defn id
  ([self {:keys [source] :or {source :datomic} :as options}]
   (condp = source
     :datomic (:db/id (.-entity self))
     :sporeID ((resource-helpers/resource-attribute (.ident self) :sporeID) (.-entity self))
     (throw (ex-info
             "Called id on a resource with an unknown source parameter"
             {:resource (.ident self)
              :source source})))))

(defn display
  ([self {:keys [] :or {} :as options}]
   (into [] (.-entity self))))

(defn attr
  ([self attribute {:keys [] :or {} :as options}]
   (let [attribute-manifest (-> self
                                (.-manifest)
                                (get (ident self))
                                (get attribute))
         default-value (-> self
                           (.-entity)
                           (get (resource-helpers/resource-attribute (ident self) attribute)))]

     (cond

       (and (= :ref (:type attribute-manifest))
            (:ref-type attribute-manifest false)
            (not (= :many (:cardinality attribute-manifest)))
            (not (empty? default-value)))
       ((resolve (symbol (str "spore.model." (resource-helpers/ident->namespace (:ref-type attribute-manifest)))
                         (str "->" (resource-helpers/ident->namespace (:ref-type attribute-manifest)))))
        (var-get (resolve (symbol (str "spore.model." (resource-helpers/ident->namespace (:ref-type attribute-manifest)))
                                  "manifest")))
        default-value)

       (and (= (:ref (:type attribute-manifest)))
            (:ref-type attribute-manifest false)
            (= (:many (:cardinality attribute-manifest)))
            (not (empty? default-value)))
       (map
        (fn [entity]
          ((resolve (symbol (str "spore.model." (resource-helpers/ident->namespace (:ref-type attribute-manifest)))
                            (str "->" (resource-helpers/ident->namespace (:ref-type attribute-manifest)))))
           (var-get (resolve (symbol (str "spore.model." (resource-helpers/ident->namespace (:ref-type attribute-manifest)))
                                     "manifest")))
           entity))
        default-value)
       
       :else
       default-value))))

(defn serialize
  ([self serializer options]
   (let [invokable-serializer (resolve (symbol (str "spore.serializer." (resource-helpers/ident->namespace (.ident self))) (name serializer)))]
     (invokable-serializer options))))

(defn data
  ([self data-fn options]
   (let [invokable-data-fn (resolve (symbol (str "spore.data." (resource-helpers/ident->namespace (.ident self))) (name data-fn)))]
     (invokable-data-fn options))))


(defn destroy
  ([self options db-uri]
   (let [connection (d/connect db-uri)]
     (d/transact connection [[:db.fn/retractEntity (.id self)]]))))

(defn ^:private validate-revise-params [self params]
  (let [required-attributes (->> (first (vals (.-manifest self)))
                                 (filter (fn [[key value]]
                                           (util/contains-submap? value {:required true})))
                                 (map first))]
    
    (doseq [[key value] params]
      (if-not (.contains (keys (first (vals (.-manifest self)))) key)
        (throw (ex-info
                "Tried to revise record with a parameter that is not defined on the manifest"
                {:model (.ident self)
                 :parameter key})))

      (if (and
           (.contains required-attributes key)
           (= value nil))
        (throw (ex-info
                "Tried to revise attribute to nil, but the attribute is required by the manifest"
                {:model (.ident self)
                 :parameter key}))))))

(defn revise
  ([self params {:keys [return] :or {return :record} :as options} db-uri]
   (validate-revise-params self params)
   (let [connection (d/connect db-uri)
         tx-fragment (mapv (fn [[key value]] (if (nil? value)
                                              (if-let [current-attribute-value (.attr self key)]
                                                [:db/retract (.id self) key current-attribute-value])
                                              [:db/add (.id self) key value]))
                           (reduce-kv (fn [memo key value] (assoc memo (resource-helpers/resource-attribute (.ident self) key) value)) {} params))
         tx-data (vec (remove nil? tx-fragment))]

     tx-data

     (if (= return :tx-data)
       tx-data
       (let [db-after (:db-after @(d/transact connection tx-data))]
         (condp = return
           :id (.id self)
           :entity (d/entity db-after (.id self))
           :record ((resolve (symbol (str "spore.model." (resource-helpers/ident->namespace (.ident self)))
                                     (str "->" (resource-helpers/ident->namespace (.ident self)))))
                    (var-get (resolve (symbol (str "spore.model." (resource-helpers/ident->namespace (.ident self)))
                                              "manifest")))
                    (d/entity db-after (.id self)))))))))

(defn retract-components
  ([self attribute options db-uri]
   "..."))
