(ns spore.implementation.collection
  (:require [spore.protocol.collection :as collection-protocol]
            [spore.helpers.resource :as resource-helpers]))

(defrecord SporeCollection [manifest records]
  collection-protocol/SporeCollectionProtocol

  (collection-protocol/ident [self]
    (first (keys manifest)))
  
  (collection-protocol/total [self] (count records))

  (collection-protocol/scope [self scope-name] (collection-protocol/scope self scope-name {}))
  (collection-protocol/scope
    [self scope-name {:keys [] :or {} :as options}]
    (->SporeCollection
     manifest
     (filter
      #((resolve
         (symbol (str "spore.scope." (resource-helpers/ident->namespace (collection-protocol/ident self))) (name scope-name)))
        %1 options)
      records)))
  
  (collection-protocol/sorter [self sorter-name] (collection-protocol/sorter self sorter-name {}))
  (collection-protocol/sorter
    [self sorter-name {:keys [] :or {} :as options}]
    (->SporeCollection
     manifest
     (sort
      ((resolve
        (symbol (str "spore.sorter." (resource-helpers/ident->namespace (collection-protocol/ident self))) (name sorter-name)))
       options)
      records)))

  (collection-protocol/serialize [self serializer] (collection-protocol/serialize self serializer {}))
  (collection-protocol/serialize
    [self serializer {:keys [] :or {} :as options}]
    (map #(.serialize % serializer options) records))

  (collection-protocol/top [self] (first records))
  (collection-protocol/top [self n] (->SporeCollection manifest (take n records))))
