(ns spore.implementation.class
  (:require [spore.helpers.resource :as resource-helpers]))

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

(defn all
  ([self options]
    (var-get (resolve (symbol "spore.config/default-db-uri")))))
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
; (defn build
;   ([self params] (build self params {}))
;   ([self params options] "..."))
;
; (defn create
;   ([self params] (create self params {}))
;   ([self params options] "..."))
;
; (defn detect-or-create
;   ([self params] (detect-or-create self params {}))
;   ([self params options] "..."))
