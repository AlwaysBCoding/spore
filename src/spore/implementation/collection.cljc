(ns spore.implementation.collection)

(defn scope
  ([self scope-name] (scope self scope-name {}))
  ([self scope-name options] "..."))

(defn sorter
  ([self sorter-name] (sorter self sorter-name {}))
  ([self sorter-name options] "..."))

(defn serialize
  ([self serializer] (serialize self serializer {}))
  ([self serializer options] "..."))

(defn top
  ([self] "...")
  ([self n] "..."))
