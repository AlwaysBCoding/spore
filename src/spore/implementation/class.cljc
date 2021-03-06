(ns spore.implementation.class
  (:require [spore.helpers.resource :as resource-helpers]
            [spore.helpers.util :as util]
            [camel-snake-kebab.core :refer :all]
            [datomic.api :as d]
            [spore.protocol.lifecycle :refer (SporeClassLifecycleProtocol)]
            [spore.implementation.collection :refer (->SporeCollection)]))

(defn to-string
  ([self]
   (str "#<SporeClass::" (->PascalCase (-> self .-manifest .inflections :ident)) ">")))

(defn ident
  ([self]
    (-> self .-manifest .inflections :ident)))

(defn schema
  ([self]
   (resource-helpers/manifest->schema (assoc {} (-> self .-manifest .inflections :datomic-prefix) (-> self .-manifest .schema)))))

(defn data
  ([self data-fn options]
   (let [invokable-data-fn (resolve (symbol (str "spore.data." (-> self .-manifest .inflections :namespace)) (name data-fn)))]
      (invokable-data-fn options))))

(defn query
  ([self query-fn options]
   (let [invokable-query-fn (resolve (symbol (str "spore.query." (-> self .-manifest .inflections :namespace)) (name query-fn)))]
      (invokable-query-fn options))))

(defn build
  ([self params {:keys [] :or {} :as options}]
    (let [tempid (d/tempid :db.part/user)
          tx-fragment (reduce-kv
                       (fn [memo key value]
                         (assoc memo (keyword (str (name (-> self .-manifest .inflections :datomic-prefix)) "/" (name key))) value))
                       {} params)
          tx-record (merge
                     {:db/id tempid
                      (keyword (str (name (-> self .-manifest .inflections :datomic-prefix)) "/" "sporeID")) (d/squuid)}
                     tx-fragment)]
      tx-record)))

(defn ^:private validate-create-params [self params]
  
  (let [required-attributes (->> (-> self .-manifest .schema)
                                 (filter (fn [[key value]]
                                           (util/contains-submap? value {:required true})))
                                 (map first))]
    
    (if-not (util/contains-map-keys? params required-attributes)
      (throw (ex-info
              "Not all required attributes defined on the manifest are present in the params"
              {:model (-> self .-manifest .inflections :ident)
               :required-attributes required-attributes}))))

  (doseq [[key value] params]
    (if-not (.contains (keys (-> self .-manifest .schema)) key)
      (throw (ex-info
              "Tried to build tx-data with a parameter that is not defined on the manifest"
              {:model (.ident self)
               :parameter key})))))

(defn create
  ([self params {:keys [return] :or {return :record} :as options} db-uri]
   (let [connection (d/connect db-uri)
         params-to-use (atom params)]
     
     ;; BEFORE CREATE     
     (if-let [before-create (-> self .-manifest .lifecycle :before-create)]
       (if-let [annotated-params (before-create self params)]
         (reset! params-to-use annotated-params)
         (throw (ex-info
                 "Lifecycle event returned false"
                 {:model (.ident self)
                  :lifecycle-event :before-create}))))

     ;; VALIDATE PARAMS
     (validate-create-params self @params-to-use)

     ;; CREATE RECORD
     (let [tx-record (.build self @params-to-use)
           tx-data (vector tx-record)
           tx-result @(d/transact connection tx-data)
           entity (d/entity (:db-after tx-result) (d/resolve-tempid (:db-after tx-result) (:tempids tx-result) (:db/id tx-record)))
           record (.construct-instance self entity)]

       ;; AFTER CREATE
       (if-let [after-create (-> self .-manifest .lifecycle :after-create)]
         (if-not (after-create self params)
           (throw (ex-info
                   "Lifecycle event returned false"
                   {:model (.ident self)
                    :lifecycle-event :after-create}))))
       
       ;; RETURN
       (condp = return
         :id (:db/id entity)
         :entity entity
         :record record))

     )))

(defn all
  ([self {:keys [return] :or {return :records} :as options} db-uri]
   (let [db (d/db (d/connect db-uri))
         ids (d/q '[:find [?eid ...]
                    :in $ ?attribute
                    :where
                    [?eid ?attribute ?sporeID]]
                  db (keyword (str (name (-> self .-manifest .inflections :datomic-prefix)) "/" (name :sporeID))))]
     (condp = return
       :ids ids
       :entities (map #(d/entity db %) ids)
       :records (->SporeCollection
                 (.-manifest self)
                 (map #(.construct-instance self (d/entity db %)) ids))))))

(defn ^:private validate-query-params [self params]

  (doseq [[key value] params]
    (if-not (.contains (keys (-> self .-manifest .schema)) key)
      (throw (ex-info
              "Tried to build a query with a parameter that is not defined on the manifest"
              {:model (.ident self)
               :parameter key})))))

(defn where
  ([self params {:keys [return] :or {return :records} :as options} db-uri]

   (validate-query-params self params)

   (let [db (d/db (d/connect db-uri))
         name-fn (comp symbol (partial str "?") name)
         param-names (map name-fn (keys params))
         param-vals (vals params)
         attribute-names (map #(keyword (str (name (-> self .-manifest .inflections :datomic-prefix)) "/" (name %))) (keys params))
         where-clause (map #(vector '?eid %1 %2) attribute-names param-names)
         in-clause (conj param-names '$)
         final-clause (concat [:find '[?eid ...]]
                              [:in] in-clause
                              [:where] where-clause)
         ids (apply d/q final-clause db param-vals)]

     (condp = return
       :ids ids
       :entities (map #(d/entity db %) ids)
       :records (->SporeCollection
                 (.-manifest self)
                 (map #(.construct-instance self (d/entity db %)) ids))))))

(defn detect
  ([self params {:keys [return] :or {return :record} :as options} db-uri]

   (validate-query-params self params)

   (let [db (d/db (d/connect db-uri))
         name-fn (comp symbol (partial str "?") name)
         param-names (map name-fn (keys params))
         param-vals (vals params)
         attribute-names (map #(keyword (str (name (-> self .-manifest .inflections :datomic-prefix)) "/" (name %))) (keys params))
         where-clause (map #(vector '?eid %1 %2) attribute-names param-names)
         in-clause (conj param-names '$)
         final-clause (concat [:find '?eid '.]
                              [:in] in-clause
                              [:where] where-clause)
         id (apply d/q final-clause db param-vals)
         entity (d/entity db id)]

     (if entity
       (condp = return
         :id id
         :entity entity
         :record (.construct-instance self entity))
       nil))))

(defn lookup
  ([self id {:keys [return] :or {return :record} :as options} db-uri]
   (let [db (d/db (d/connect db-uri))
         entity (d/entity db id)]

     (if (not (empty? (into [] entity)))
       (condp = return
         :id id
         :entity entity
         :record (.construct-instance self entity))
       nil))))

(defn one
  ([self {:keys [return] :or {return :record} :as options} db-uri]
   (let [db (d/db (d/connect db-uri))
         id (d/q '[:find ?eid .
                   :in $ ?attribute
                   :where
                   [?eid ?attribute ?sporeID]]
                 db (resource-helpers/resource-attribute (.ident self)))]

     (condp = return
       :id id
       :entity (d/entity db id)
       :record (.construct-instance self (d/entity db id))))))

(defn detect-or-create
  ([self params {:keys [return] :or {return :record} :as options} db-uri]
   (if-let [record (.detect self params options db-uri)]
     record
     (.create self params options db-uri))))

(defn destroy-all
  ([self {:keys [] :or {} :as options} db-uri]
   (let [connection (d/connect db-uri)
         tx-data (mapv #(vector :db.fn/retractEntity %) (.all self {:return :ids} db-uri))]
     (d/transact connection tx-data))))

(defn destroy-where
  ([self params options db-uri]
   (let [connection (d/connect db-uri)
         tx-data (mapv #(vector :db.fn/retractEntity %) (.where self params {:return :ids} db-uri))]
     (d/transact connection tx-data))))
