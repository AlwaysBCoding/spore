(ns spore.implementation.instance)

(defn id
  ([self] "..."))

(defn attr
  ([self attribute] (attr self attribute {}))
  ([self attribute options] "..."))

(defn display
  ([self] (display self {}))
  ([self options] "..."))

(defn serialize
  ([self serializer] (serialize self serializer {}))
  ([self serializer options] "..."))

(defn data
  ([self data-fn] (data self data-fn {}))
  ([self data-fn options] "..."))

(defn destroy
  ([self] (destroy self {}))
  ([self options] "..."))

(defn revise
  ([self params] (revise self params {}))
  ([self params options] "..."))

(defn retract-components
  ([self attribute] (retract-components self attribute {}))
  ([self attribute options] "..."))
