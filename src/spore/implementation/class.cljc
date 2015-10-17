(ns spore.implementation.class
  (:require [spore.helpers.resource :as resource-helpers]
            [datomic.api :as d]))

(defn manifest
  ([self manifest]
    manifest))

(defn ident
  ([self]
    (first (keys (.manifest self)))))

(defn schema
  ([self]
    (resource-helpers/manifest->schema (.manifest self))))

(defn data
  ([self data-fn options]
    (let [invokable-data-fn (resolve (symbol (str "spore.data." (resource-helpers/resource-ident->resource-namespace (.ident self))) (str (name data-fn))))]
      (invokable-data-fn options))))

(defn query
  ([self query-fn options]
    (let [invokable-query-fn (resolve (symbol (str "spore.query." (resource-helpers/resource-ident->resource-namespace (.ident self))) (str (name query-fn))))]
      (invokable-query-fn options))))

(defn ^:private validate-build-parameter [manifest key value]
  (.contains (keys (first (vals manifest))) key))

(defn build
  ([self params {:keys [] :or {} :as options}]
    (let [tempid (d/tempid :db.part/user)
          tx-fragment (reduce-kv
                       (fn [memo key value]
                         (if (validate-build-parameter (.manifest self) key value)
                           (assoc memo (resource-helpers/resource-attribute (.ident self) key) value)
                           (throw (ex-info
                                   "Called #build with a parameter that is not defined on the manifest"
                                   {:model (.ident self)
                                    :parameter key}))))
                       {} params)
          tx-record (merge
                     {:db/id tempid
                      (resource-helpers/resource-attribute (.ident self) :sporeID) (d/squuid)}
                     tx-fragment)]
      tx-record)))

(defn create
  ([self params {:keys [return] :or {return :record} :as options} db-uri]
   (let [connection (d/connect db-uri)
         tx-record (.build self params)
         tx-data (vector tx-record)
         tx-result @(d/transact connection tx-data)
         record (d/entity (:db-after tx-result) (d/resolve-tempid (:db-after tx-result) (:tempids tx-result) (:db/id tx-record)))]

     (condp = return
       :id (:db/id record)
       :entity record
       :record record))))

(defn all
  ([self {:keys [return] :or {return :records} :as options} db-uri]
    #_(let [db (d/db (d/connect db-uri))
          ids (d/q '[:find [?eid ...]
                     :in $ ?attribute
                     :where
                     [?eid ?attribute ?sporeID]]
                db (resource-helpers/resource-attribute (.ident self)))]
      (condp = return
        :ids ids
        :entities (map #(d/entity db %) ids)
        :records (map #(d/entity db %) ids)))))
;
; (defn one
;   ([self] (one self {}))
;   ([self options] "..."))
;
; (defn where
;   ([self params] (where self params {}))
;   ([self params options] "..."))
;
; (defn detect
;   ([self params] (detect self params {}))
;   ([self params options] "..."))
;
; (defn lookup
;   ([self params] (lookup self params {}))
;   ([self params options] "..."))
;
; (defn destroy-all
;   ([self] (destroy-all self {}))
;   ([self options] "..."))
;
; (defn destroy-where
;   ([self params] (destroy-where self params {}))
;   ([self params options] "..."))
;
;
;
; (defn detect-or-create
;   ([self params] (detect-or-create self params {}))
;   ([self params options] "..."))
