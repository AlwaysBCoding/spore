(ns spore.core
  (:require [spore.protocol.class :as class-protocol]
            [spore.protocol.instance :as instance-protocol]
            [spore.implementation.class :as class-implementation]
            [spore.implementation.instance :as instance-implementation]
            [com.stuartsierra.component :as component]
            [spore.helpers.resource :as resource-helpers]))

(defmacro SporeInstance [instance-name & body]
  `(defrecord ~instance-name [~'manifest ~'entity]

     instance-protocol/SporeInstanceProtocol

     (instance-protocol/ident [self#] (instance-implementation/ident self#))

     (instance-protocol/id [self#] (instance-protocol/id self# {}))
     (instance-protocol/id [self# options#] (instance-implementation/id self# options#))

     (instance-protocol/display [self#] (instance-protocol/display self# {}))
     (instance-protocol/display [self# options#] (instance-implementation/display self# options#))
     
     (instance-protocol/serialize [self# serializer#] (instance-protocol/serialize self# serializer# {}))
     (instance-protocol/serialize [self# serializer# options#] (instance-implementation/serialize self# serializer# options#))
     
     (instance-protocol/data [self# data-fn#] (instance-protocol/data self# data-fn# {}))
     (instance-protocol/data [self# data-fn# options#] (instance-implementation/data self# data-fn# options#))

     (instance-protocol/attr [self# attribute#] (instance-protocol/attr self# attribute# {}))
     (instance-protocol/attr [self# attribute# options#] (instance-implementation/attr self# attribute# options#))

     (instance-protocol/destroy [self#] (instance-protocol/destroy self# {}))
     (instance-protocol/destroy [self# options#] (instance-protocol/destroy self# options# (var-get (resolve (symbol "spore.config/default-db-uri")))))
     (instance-protocol/destroy [self# options# db-uri#] (instance-implementation/destroy self# options# db-uri#))

     (instance-protocol/revise [self# params#] (instance-protocol/revise self# params# {}))
     (instance-protocol/revise [self# params# options#] (instance-protocol/revise self# params# options# (var-get (resolve (symbol "spore.config/default-db-uri")))))
     (instance-protocol/revise [self# params# options# db-uri#] (instance-implementation/revise self# params# options# db-uri#))

     (instance-protocol/retract-components [self# attribute#] (instance-protocol/retract-components self# attribute# {}))
     (instance-protocol/retract-components [self# attribute# options#] (instance-protocol/retract-components self# attribute# options# (var-get (resolve (symbol "spore.config/default-db-uri")))))
     (instance-protocol/retract-components [self# attribute# options# db-uri#] (instance-implementation/retract-components self# attribute# options# db-uri#))

     
     
     ~@body))
        
(defmacro SporeClass [class-name & body]
  `(defrecord ~class-name [~'manifest ~'instance-constructor ~'dependencies]

     component/Lifecycle

     (component/start [self#]
      self#)

     (component/stop [self#]
       self#)
     
     class-protocol/SporeClassProtocol

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
     (class-protocol/create [self# params# options# db-uri#] (class-implementation/create self# params# (merge options# {:instance-constructor ~'instance-constructor}) db-uri#))

     (class-protocol/all [self#] (class-protocol/all self# {}))
     (class-protocol/all [self# options#] (class-protocol/all self# options# (var-get (resolve (symbol "spore.config/default-db-uri")))))
     (class-protocol/all [self# options# db-uri#] (class-implementation/all self# (merge options# {:instance-constructor ~'instance-constructor}) db-uri#))

     (class-protocol/where [self# params#] (class-protocol/where self# params# {}))
     (class-protocol/where [self# params# options#] (class-protocol/where self# params# options# (var-get (resolve (symbol "spore.config/default-db-uri")))))
     (class-protocol/where [self# params# options# db-uri#] (class-implementation/where self# params# (merge options# {:instance-constructor ~'instance-constructor}) db-uri#))

     (class-protocol/detect [self# params#] (class-protocol/detect self# params# {}))
     (class-protocol/detect [self# params# options#] (class-protocol/detect self# params# options# (var-get (resolve (symbol "spore.config/default-db-uri")))))
     (class-protocol/detect [self# params# options# db-uri#] (class-implementation/detect self# params# (merge options# {:instance-constructor ~'instance-constructor}) db-uri#))

     (class-protocol/lookup [self# id#] (class-protocol/lookup self# id# {}))
     (class-protocol/lookup [self# id# options#] (class-protocol/lookup self# id# options# (var-get (resolve (symbol "spore.config/default-db-uri")))))
     (class-protocol/lookup [self# id# options# db-uri#] (class-implementation/lookup self# id# (merge options# {:instance-constructor ~'instance-constructor}) db-uri#))

     (class-protocol/one [self#] (class-protocol/one self# {}))
     (class-protocol/one [self# options#] (class-protocol/one self# options# (var-get (resolve (symbol "spore.config/default-db-uri")))))
     (class-protocol/one [self# options# db-uri#] (class-implementation/one self# (merge options# {:instance-constructor ~'instance-constructor}) db-uri#))

     (class-protocol/detect-or-create [self# params#] (class-protocol/detect-or-create self# params# {}))
     (class-protocol/detect-or-create [self# params# options#] (class-protocol/detect-or-create self# params# options# (var-get (resolve (symbol "spore.config/default-db-uri")))))
     (class-protocol/detect-or-create [self# params# options# db-uri#] (class-implementation/detect-or-create self# params# (merge options# {:instance-constructor ~'instance-constructor}) db-uri#))

     (class-protocol/destroy-all [self#] (class-protocol/destroy-all self# {}))
     (class-protocol/destroy-all [self# options#] (class-protocol/destroy-all self# options# (var-get (resolve (symbol "spore.config/default-db-uri")))))
     (class-protocol/destroy-all [self# options# db-uri#] (class-implementation/destroy-all self# options# db-uri#))

     (class-protocol/destroy-where [self# params#] (class-protocol/destroy-where self# params# {}))
     (class-protocol/destroy-where [self# params# options#] (class-protocol/destroy-where self# params# options# (var-get (resolve (symbol "spore.config/default-db-uri")))))
     (class-protocol/destroy-where [self# params# options# db-uri#] (class-implementation/destroy-where self# params# options# db-uri#))

     ~@body))
