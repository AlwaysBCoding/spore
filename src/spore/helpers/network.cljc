(ns spore.helpers.network
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(defn get-json-data [api-endpoint]
  (-> api-endpoint
      (http/get)
      (:body)
      (json/parse-string true)))
