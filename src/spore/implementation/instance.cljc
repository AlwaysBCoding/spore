(ns spore.implementation.instance
  (:require [spore.helpers.resource :as resource-helpers]))

(defn id
  ([self {:keys [source] :or {source :datomic} :as options}]
   (condp = source
     :datomic (:db/id (.-entity self))
     :sporeID ((resource-helpers/resource-attribute (.-ident self) :sporeID) (.-entity self))
     (throw (ex-info
             "Called id on a resource with an unknown source parameter"
             {:resource (.-ident self)
              :source source})))))

(defn display
  ([self {:keys [] :or {} :as options}]
   (into [] (.-entity self))))

;; (defn attr
;;   ([self attribute] (attr self attribute {}))
;;   ([self attribute options] "..."))

;; (defn serialize
;;   ([self serializer] (serialize self serializer {}))
;;   ([self serializer options] "..."))

;; (defn data
;;   ([self data-fn] (data self data-fn {}))
;;   ([self data-fn options] "..."))

;; (defn destroy
;;   ([self] (destroy self {}))
;;   ([self options] "..."))

;; (defn revise
;;   ([self params] (revise self params {}))
;;   ([self params options] "..."))

;; (defn retract-components
;;   ([self attribute] (retract-components self attribute {}))
;;   ([self attribute options] "..."))
