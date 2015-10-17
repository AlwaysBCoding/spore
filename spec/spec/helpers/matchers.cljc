(ns spec.helpers.matchers)

(defn contains-submap? [target-map submap]
  (clojure.set/subset? (set submap) (set target-map)))

(defn contains-map-keys? [target-map search-keys]
  (every? (set (keys target-map)) (set search-keys)))
