(ns spore.router
  (:require [cheshire.core :as json]
            [spore.helpers.resource :as resource-helpers]))

(defn route-request [request]
  (let [controller (-> request :params :controller)
        action (-> request :params :action)
        options (-> request :params :options)
        response-format (if-let [format (-> request :headers (get "x-spore-return-format") (keyword))] format :edn)
        response-data (eval
                        (list
                          (symbol (str "spore.controller." (resource-helpers/ident->namespace controller)) (name action))
                          options))]

    {:status 200
     :headers {"Content-Type" "text/plain"}
     :body (pr-str response-data)}))
