(ns spore.helpers.resource
  (:require [datomic.api :as d]
            [camel-snake-kebab.core :refer :all]))

(def default-db-uri
  (:db-uri (read-string (slurp "config.edn"))))

(defn resource-attribute
  ([ident]
   (resource-attribute ident :uuid))

  ([ident attr]
   (keyword (str (name (->camelCase ident)) "/" (name attr)))))

(defn format-value [value]
  (cond
    (= (type value) datomic.query.EntityMap) (:db/id value)
    :else value))

(defn load-files [path]
  (let [file  (java.io.File. path)
        files (.listFiles file)]
    (doseq [x files]
      (when (.isFile x)
        (load-file (.getCanonicalPath x))))))

(defn resource-ident->resource-namespace [resource-ident]
  (->kebab-case (clojure.string/replace (str (name resource-ident)) #"\." "-")))

(defn keyword->schema-key [field k]
  (if (nil? k)
    nil
    (-> {:type        {:string  :db.type/string
                       :long    :db.type/long
                       :keyword :db.type/keyword
                       :boolean :db.type/boolean
                       :bigint  :db.type/bigint
                       :float   :db.type/float
                       :double  :db.type/double
                       :bigdec  :db.type/bigdec
                       :ref     :db.type/ref
                       :uuid    :db.type/uuid
                       :instant :db.type/instant
                       :uri     :db.type/uri
                       :bytes   :db.type/bytes}

         :cardinality {:one    :db.cardinality/one
                       :many   :db.cardinality/many}

         :component {:true true
                     :false false}

         :unique      {:identity :db.unique/identity
                       :value    :db.unique/value}}
        field
        k)))

(defn manifest->schema [manifest]
  (vec
   (for [[entity-ns entity-attrs] manifest
         [attr properties] entity-attrs]
     (let [{:keys [type cardinality unique index component]} properties]
       (merge
        {:db/id (d/tempid :db.part/db)
         :db/ident (keyword (str (name entity-ns)
                                 "/"
                                 (name attr)))
         :db/valueType (keyword->schema-key :type
                                            type)
         :db/cardinality (or (keyword->schema-key :cardinality
                                                  cardinality)
                             :db.cardinality/one)

         :db/isComponent (or (keyword->schema-key :component component)
                             false)

         :db.install/_attribute :db.part/db}

        (when unique {:db/unique (keyword->schema-key :unique
                                                      unique)})
        (when index {:db/index true}))))))
