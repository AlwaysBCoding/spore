(ns spore.core
  (:require [spore.protocol.class :as class-protocol]
            [spore.protocol.instance :as instance-protocol]
            [spore.implementation.class :as class-implementation]
            [spore.implementation.instance :as instance-implementation]
            [com.stuartsierra.component :as component]
            [spore.helpers.resource :as resource-helpers]))

(defmacro SporeInstance [instance-name & body]
  `(defrecord ~instance-name [~'ident ~'entity]

     instance-protocol/SporeInstanceProtocol

     (instance-protocol/id [self#] (instance-protocol/id self# {}))
     (instance-protocol/id [self# options#] (instance-implementation/id self# options#))

     (instance-protocol/display [self#] (instance-protocol/display self# {}))
     (instance-protocol/display [self# options#] (instance-implementation/display self# options#))

     (instance-protocol/serialize [self# serializer#] (instance-protocol/serialize self# serializer# {}))
     (instance-protocol/serialize [self# serializer# options#]
       ((resolve (symbol (str "spore.serializer." (resource-helpers/ident->namespace ~'ident)) (name serializer#)))
        self# options#))
     
     (instance-protocol/data [self# data-fn#] (instance-protocol/data self# data-fn# {}))
     (instance-protocol/data [self# data-fn# options#]
       ((resolve (symbol (str "spore.data." (resource-helpers/ident->namespace ~'ident)) (name serializer#)))
        self# options#))
     
     ~@body))

(defmacro SporeClass [class-name manifest instance-constructor dependencies & body]
  `(defrecord ~class-name []

     component/Lifecycle

     (component/start [self#]
      self#)

     (component/stop [self#]
       self#)
     
     class-protocol/SporeClassProtocol

     (class-protocol/manifest [self#] ~manifest)
     (class-protocol/dependencies [self#] ~dependencies)
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
     (class-protocol/create [self# params# options# db-uri#]
       (~instance-constructor
        (class-protocol/ident self#)
        (class-implementation/create self# params# options# db-uri#)))

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
     (class-protocol/destroy-where [self# params# options# db-uri#] (class-implementation/destroy-where self# params# options# db-uri#))

     ~@body))
