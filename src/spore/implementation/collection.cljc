(ns spore.implementation.collection
  (:require [spore.protocol.internal.collection :as collection-protocol]
            [spore.helpers.resource :as resource-helpers]))

(defrecord SporeCollection [manifest records]
  collection-protocol/SporeInternalCollectionProtocol

  (collection-protocol/ident [self]
    (-> manifest .inflections :ident))

  (collection-protocol/total [self] (count records))

  (collection-protocol/scope [self scope-name] (collection-protocol/scope self scope-name {}))
  (collection-protocol/scope
    [self scope-name {:keys [] :or {} :as options}]
    (->SporeCollection
     manifest
     (filter
      #((resolve
         (symbol (str "spore.scope." (name (-> manifest .inflections :namespace))) (name scope-name)))
        %1 options)
      records)))

  (collection-protocol/sorter [self sorter-name] (collection-protocol/sorter self sorter-name {}))
  (collection-protocol/sorter
    [self sorter-name {:keys [] :or {} :as options}]
    (->SporeCollection
     manifest
     (sort
      ((resolve
        (symbol (str "spore.sorter." (name (-> manifest .inflections :namespace))) (name sorter-name)))
       options)
      records)))

  (collection-protocol/serialize [self serializer] (collection-protocol/serialize self serializer {}))
  (collection-protocol/serialize
    [self serializer {:keys [] :or {} :as options}]
    (map #(.serialize % serializer options) records))

  (collection-protocol/top [self] (first records))
  (collection-protocol/top [self n] (->SporeCollection manifest (take n records))))

(defmethod clojure.core/print-method SporeCollection
  [self ^java.io.Writer writer]
  (.write writer
   (str "#<SporeCollection"
        " "
        "" (name (.ident self)) ""
        " "
        "{ " (.format (java.text.NumberFormat/getInstance (java.util.Locale/US)) (bigdec (.total self))) " }"
        ">")))
