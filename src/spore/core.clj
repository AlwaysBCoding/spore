(ns spore.core
  (:require [spore.protocol.class :as class-protocol :refer (SporeClassProtocol)]
            [spore.implementation.class :as class-implementation]))

(defmacro SporeClass [class-name manifest & body]
  `(defrecord ~class-name []
     SporeClassProtocol

     (class-protocol/manifest [self#] (class-implementation/manifest self# ~manifest))
     (class-protocol/ident [self#] (class-implementation/ident self#))
     (class-protocol/schema [self#] (class-implementation/schema self#))))
