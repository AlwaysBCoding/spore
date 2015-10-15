(ns spore.implementation.class)

(defn manifest
  ([self] "..."))

(defn ident
  ([self] "..."))

(defn schema
  ([self] "..."))

(defn data
  ([self data-fn] (data self data-fn {}))
  ([self data-fn options] "..."))

(defn query
  ([self query-fn] (query self query-fn {}))
  ([self query-fn options] "..."))

(defn all
  ([self] (all self {}))
  ([self options] "..."))

(defn one
  ([self] (one self {}))
  ([self options] "..."))

(defn where
  ([self params] (where self params {}))
  ([self params options] "..."))

(defn detect
  ([self params] (detect self params {}))
  ([self params options] "..."))

(defn lookup
  ([self params] (lookup self params {}))
  ([self params options] "..."))

(defn destroy-all
  ([self] (destroy-all self {}))
  ([self options] "..."))

(defn destroy-where
  ([self params] (destroy-where self params {}))
  ([self params options] "..."))

(defn build
  ([self params] (build self params {}))
  ([self params options] "..."))

(defn create
  ([self params] (create self params {}))
  ([self params options] "..."))

(defn detect-or-create
  ([self params] (detect-or-create self params {}))
  ([self params options] "..."))
