(ns spore.implementation.instance
  (:require [spore.helpers.resource :as resource-helpers]
            [spore.helpers.util :as util]
            [camel-snake-kebab.core :refer :all]
            [datomic.api :as d]
            [spore.protocol.lifecycle :refer (SporeInstanceLifecycleProtocol)]
            [spore.implementation.collection :refer (->SporeCollection)]))

(defn to-string
  ([self]
   (str "#<SporeInstance::" (resource-helpers/ident->namespace (.ident self)) ">")))

(defn ident
  ([self]
   (-> self .-manifest .inflections :ident)))

(defn id
  ([self {:keys [source] :or {source :datomic} :as options}]
   (condp = source
     :datomic (:db/id (.-entity self))
     :sporeID ((resource-helpers/resource-attribute (-> self .-manifest .inflections :datomic-prefix) :sporeID) (.-entity self))
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
        default-value
        {})

       (and (= (:ref (:type attribute-manifest)))
            (:ref-type attribute-manifest false)
            (= (:many (:cardinality attribute-manifest)))
            (not (empty? default-value)))
       (->SporeCollection
        (.-manifest self)
        (map
         (fn [entity]
           ((resolve (symbol (str "spore.model." (resource-helpers/ident->namespace (:ref-type attribute-manifest)))
                             (str "->" (resource-helpers/ident->namespace (:ref-type attribute-manifest)))))
            (var-get (resolve (symbol (str "spore.model." (resource-helpers/ident->namespace (:ref-type attribute-manifest)))
                                      "manifest")))
            entity {}))
         default-value))

       :else
       default-value))))

(defn serialize
  ([self serializer options]
   (let [invokable-serializer (resolve (symbol (str "spore.serializer." (name (-> self .-manifest .inflections :namespace))) (name serializer)))]
     (invokable-serializer self options))))

(defn data
  ([self data-fn options]
   (let [invokable-data-fn (resolve (symbol (str "spore.data." (name (-> self .-manifest .inflections :namespace))) (name data-fn)))]
     (invokable-data-fn options))))

(defn destroy
  ([self options db-uri]
   (let [connection (d/connect db-uri)]

     ;; BEFORE DESTROY
     (if-let [before-destroy (-> self .-manifest .lifecycle :before-destroy)]
       (if-not (before-destroy self)
         (throw (ex-info
                 "Lifecycle event returned false"
                 {:model (.ident self)
                  :lifecycle-event :before-destroy}))))

     ;; DESTROY RECORD
     (let [tx-result @(d/transact connection [[:db.fn/retractEntity (.id self)]])]

       ;; AFTER DESTROY
       (if-let [after-destroy (-> self .-manifest .lifecycle :after-destroy)]
         (if-not (after-destroy self)
           (throw (ex-info
                 "Lifecycle event returned false"
                 {:model (.ident self)
                  :lifecycle-event :after-destroy}))))

       ;; RETURN
       tx-result))))

(defn ^:private validate-revise-params [self params]
  (let [required-attributes (->> (-> self .-manifest .schema)
                                 (filter (fn [[key value]]
                                           (util/contains-submap? value {:required true})))
                                 (map first))]

    (doseq [[key value] params]
      (if-not (.contains (keys (-> self .-manifest .schema)) key)
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
   (let [connection (d/connect db-uri)
         params-to-use (atom params)]

     ;; BEFORE SAVE
     (if-let [before-save (-> self .-manifest .lifecycle :before-save)]
       (if-let [annotated-params (before-save self params)]
         (reset! params-to-use annotated-params)
         (throw (ex-info
                 "Lifecycle event returned false"
                 {:model (.ident self)
                  :lifecycle-event :before-save}))))
     
     ;; VALIDATE PARAMS
     (validate-revise-params self @params-to-use)

     ;; REVISE RECORD
     (let [tx-fragment (mapv (fn [[key value]] (if (nil? value)
                                                 (if-let [current-attribute-value (.attr self key)]
                                                   [:db/retract (.id self) key current-attribute-value])
                                                 [:db/add (.id self) key value]))
                             (reduce-kv (fn [memo key value] (assoc memo (resource-helpers/resource-attribute (-> self .-manifest .inflections :datomic-prefix) key) value)) {} @params-to-use))
           tx-data (vec (remove nil? tx-fragment))]
       
       (if (= return :tx-data)
         tx-data
         (let [tx-result @(d/transact connection tx-data)
               entity (d/entity (:db-after tx-result) (.id self))
               record ((resolve (symbol (str "spore.model." (-> self .-manifest .inflections :namespace))
                                        (str "->" (resource-helpers/ident->namespace (.ident self)))))
                       (var-get (resolve (symbol (str "spore.model." (-> self .-manifest .inflections :namespace))
                                                 "manifest")))
                       entity (.-dependencies self))]
           
           ;; AFTER SAVE
           (if-let [after-save (-> self .-manifest .lifecycle :after-save)]
             (if-not (after-save self params)
               (throw (ex-info
                       "Lifecycle event returned false"
                       {:model (.ident self)
                        :lifecycle-event :after-save}))))

           ;; RETURN
           (condp = return
             :id (.id self)
             :entity entity
             :record record)))))))

(defn retract-components
  ([self attribute options db-uri]
   "..."))
