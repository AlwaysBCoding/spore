(ns spore.core
  (:require [spore.protocol.class :as class-protocol :refer (SporeClassProtocol)]
            [spore.implementation.class :as class-implementation]
            [com.stuartsierra.component :as component]))

(defmacro SporeClass [class-name manifest & body]
  `(defrecord ~class-name []

     component/Lifecycle

     (start [self#]
      self#)

     (stop [self#]
       self#)
     
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
     (class-protocol/all [self# options# db-uri#] (class-implementation/all self# options# db-uri#))

     (class-protocol/where [self# params#] (class-protocol/where self# params# {}))
     (class-protocol/where [self# params# options#] (class-protocol/where self# params# options# (var-get (resolve (symbol "spore.config/default-db-uri")))))
     (class-protocol/where [self# params# options# db-uri#] (class-implementation/where self# params# options# db-uri#))

     (class-protocol/detect [self# params#] (class-protocol/detect self# params# {}))
     (class-protocol/detect [self# params# options#] (class-protocol/detect self# params# options# (var-get (resolve (symbol "spore.config/default-db-uri")))))
     (class-protocol/detect [self# params# options# db-uri#] (class-implementation/detect self# params# options# db-uri#))

     (class-protocol/lookup [self# id#] (class-protocol/lookup self# id# {}))
     (class-protocol/lookup [self# id# options#] (class-protocol/lookup self# id# options# (var-get (resolve (symbol "spore.config/default-db-uri")))))
     (class-protocol/lookup [self# id# options# db-uri#] (class-implementation/lookup self# id# options# db-uri#))

     (class-protocol/one [self#] (class-protocol/one self# {}))
     (class-protocol/one [self# options#] (class-protocol/one self# options# (var-get (resolve (symbol "spore.config/default-db-uri")))))
     (class-protocol/one [self# options# db-uri#] (class-implementation/one self# options# db-uri#))

     (class-protocol/detect-or-create [self# params#] (class-protocol/detect-or-create self# params# {}))
     (class-protocol/detect-or-create [self# params# options#] (class-protocol/detect-or-create self# params# options# (var-get (resolve (symbol "spore.config/default-db-uri")))))
     (class-protocol/detect-or-create [self# params# options# db-uri#] (class-implementation/detect-or-create self# params# options# db-uri#))

     (class-protocol/destroy-all [self#] (class-protocol/destroy-all self# {}))
     (class-protocol/destroy-all [self# options#] (class-protocol/destroy-all self# options# (var-get (resolve (symbol "spore.config/default-db-uri")))))
     (class-protocol/destroy-all [self# options# db-uri#] (class-implementation/destroy-all self# options# db-uri#))

     (class-protocol/destroy-where [self# params#] (class-protocol/destroy-where self# params# {}))
     (class-protocol/destroy-where [self# params# options#] (class-protocol/destroy-where self# params# options# (var-get (resolve (symbol "spore.config/default-db-uri")))))
     (class-protocol/destroy-where [self# params# options# db-uri#] (class-implementation/destroy-where self# params# options# db-uri#))))
