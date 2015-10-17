(ns spore.core
  (:require [spore.protocol.class :as class-protocol :refer (SporeClassProtocol)]
            [spore.implementation.class :as class-implementation]))

(defmacro SporeClass [class-name manifest & body]
  `(defrecord ~class-name []
     SporeClassProtocol

     (class-protocol/manifest [self#] (class-implementation/manifest self# ~manifest))
     (class-protocol/ident [self#] (class-implementation/ident self#))
     (class-protocol/schema [self#] (class-implementation/schema self#))

     (class-protocol/data [self# data-fn#] (class-protocol/data self# data-fn# {}))
     (class-protocol/data [self# data-fn# options#] (class-implementation/data self# data-fn# options#))

     (class-protocol/query [self# query-fn#] (class-protocol/query self# query-fn# {}))
     (class-protocol/query [self# query-fn# options#] (class-implementation/query self# query-fn# options#))

     (class-protocol/build [self# params#] (class-protocol/build self# params# {}))
     (class-protocol/build [self# params# options#] (class-implementation/build self# params# options#))

     (class-protocol/create [self# params#] (class-protocol/create self# params# {}))
     (class-protocol/create [self# params# options#] (class-protocol/create self# params# options# (var-get (resolve (symbol "spore.config/default-db-uri")))))
     (class-protocol/create [self# params# options# db-uri#] (class-implementation/create self# params# options# db-uri#))

     (class-protocol/all [self#] (class-protocol/all self# {}))
     (class-protocol/all [self# options#] (class-protocol/all self# options# (var-get (resolve (symbol "spore.config/default-db-uri")))))
     (class-protocol/all [self# options# db-uri#] (class-implementation/all self# options# db-uri#))))
