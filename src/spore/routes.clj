(ns spore.routing
  (:require [bidi.bidi :as bidi]
            [bidi.ring :as bidi-ring]))

(defn create-handler [route-data]
  (bidi-ring/make-handler route-data))
