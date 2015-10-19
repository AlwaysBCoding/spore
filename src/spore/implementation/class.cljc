(ns spore.implementation.class
  (:require [spore.helpers.resource :as resource-helpers]
            [spore.helpers.util :as util]
            [datomic.api :as d]))

(defn ident
  ([self]
    (first (keys (.-manifest self)))))

(defn schema
  ([self]
    (resource-helpers/manifest->schema (.-manifest self))))

(defn data
  ([self data-fn options]
   (let [invokable-data-fn (resolve (symbol (str "spore.data." (resource-helpers/ident->namespace (.ident self))) (name data-fn)))]
      (invokable-data-fn options))))

(defn query
  ([self query-fn options]
   (let [invokable-query-fn (resolve (symbol (str "spore.query." (resource-helpers/ident->namespace (.ident self))) (name query-fn)))]
      (invokable-query-fn options))))

(defn build
  ([self params {:keys [] :or {} :as options}]
    (let [tempid (d/tempid :db.part/user)
          tx-fragment (reduce-kv
                       (fn [memo key value]
                         (assoc memo (resource-helpers/resource-attribute (.ident self) key) value))
                       {} params)
          tx-record (merge
                     {:db/id tempid
                      (resource-helpers/resource-attribute (.ident self) :sporeID) (d/squuid)}
                     tx-fragment)]
      tx-record)))

(defn ^:private validate-create-params [self params]
  (let [required-attributes (->> (first (vals (.-manifest self)))
                                 (filter (fn [[key value]]
                                           (util/contains-submap? value {:required true})))
                                 (map first))]

    (if-not (util/contains-map-keys? params required-attributes)
      (throw (ex-info
              "Not all required attributes defined on the manifest are present in the params"
              {:model (.ident self)
               :required-attributes required-attributes})))

    (doseq [[key value] params]
      (if-not (.contains (keys (first (vals (.-manifest self)))) key)
        (throw (ex-info
                "Tried to build tx-data with a parameter that is not defined on the manifest"
                {:model (.ident self)
                 :parameter key}))))))

(defn create
  ([self params {:keys [return instance-constructor] :or {return :record} :as options} db-uri]
   (validate-create-params self params)
   (let [connection (d/connect db-uri)
         tx-record (.build self params)
         tx-data (vector tx-record)
         tx-result @(d/transact connection tx-data)
         record (d/entity (:db-after tx-result) (d/resolve-tempid (:db-after tx-result) (:tempids tx-result) (:db/id tx-record)))]

     (condp = return
       :id (:db/id record)
       :entity record
       :record (instance-constructor (.-manifest self) record)))))

(defn all
  ([self {:keys [return instance-constructor] :or {return :records} :as options} db-uri]
   (let [db (d/db (d/connect db-uri))
         ids (d/q '[:find [?eid ...]
                    :in $ ?attribute
                    :where
                    [?eid ?attribute ?sporeID]]
                  db (resource-helpers/resource-attribute (.ident self)))]
     (condp = return
       :ids ids
       :entities (map #(d/entity db %) ids)
       :records (map #(d/entity db %) ids)))))

(defn ^:private validate-query-params [self params]

  (doseq [[key value] params]
    (if-not (.contains (keys (first (vals (.-manifest self)))) key)
      (throw (ex-info
              "Tried to build a query with a parameter that is not defined on the manifest"
              {:model (.ident self)
               :parameter key})))))

(defn where
  ([self params {:keys [return instance-constructor] :or {return :records} :as options} db-uri]
   (validate-query-params self params)
   (let [db (d/db (d/connect db-uri))
         name-fn (comp symbol (partial str "?") name)
         param-names (map name-fn (keys params))
         param-vals (vals params)
         attribute-names (map #(resource-helpers/resource-attribute (.ident self) %) (keys params))
         where-clause (map #(vector '?eid %1 %2) attribute-names param-names)
         in-clause (conj param-names '$)
         final-clause (concat [:find '[?eid ...]]
                              [:in] in-clause
                              [:where] where-clause)
         ids (apply d/q final-clause db param-vals)]

     (condp = return
       :ids ids
       :entities (map #(d/entity db %) ids)
       :records (map #(d/entity db %) ids)))))

(defn detect
  ([self params {:keys [return instance-constructor] :or {return :record} :as options} db-uri]
   (validate-query-params self params)
   (let [db (d/db (d/connect db-uri))
         name-fn (comp symbol (partial str "?") name)
         param-names (map name-fn (keys params))
         param-vals (vals params)
         attribute-names (map #(resource-helpers/resource-attribute (.ident self) %) (keys params))
         where-clause (map #(vector '?eid %1 %2) attribute-names param-names)
         in-clause (conj param-names '$)
         final-clause (concat [:find '?eid '.]
                              [:in] in-clause
                              [:where] where-clause)
         id (apply d/q final-clause db param-vals)
         record (d/entity db id)]

     (if record
       (condp = return
         :id id
         :entity record
         :record (instance-constructor (.-manifest self) record))
       nil))))

(defn lookup
  ([self id {:keys [return instance-constructor] :or {return :record} :as options} db-uri]
   (let [db (d/db (d/connect db-uri))
         record (d/entity db id)]

     (if (not (empty? (into [] record)))
       (condp = return
         :id id
         :entity record
         :record (instance-constructor (.-manifest self) record))
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
       :record (d/entity db id)))))

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
