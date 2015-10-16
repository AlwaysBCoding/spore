(defproject alwaysbcoding/spore "0.1.0-SNAPSHOT"
  :description "A Clojure framework for building web applications"
  :url "https://github.com/AlwaysBCoding/spore"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.datomic/datomic-pro "0.9.5327" :exclusions [org.clojure/clojure ... joda-time]]
                 [camel-snake-kebab "0.3.2"]]

   :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                    :username :env/DATOMIC_USERNAME
                                    :password :env/DATOMIC_PASSWORD}}

  :global-vars {*print-length* 25}

  :min-lein-version "2.0.0"

  :source-paths ["src"])

  ;  [org.apache.httpcomponents/httpclient "4.5"]
  ;  [com.datomic/datomic-pro "0.9.5130"]
  ;  [org.clojure/core.async "0.1.346.0-17112a-alpha"]
  ;  [com.stuartsierra/component "0.2.3"]
   ;
  ;  [ring "1.3.2"]
  ;  [ring-basic-authentication "1.0.5"]
  ;  [ring/ring-defaults "0.1.5"]
  ;  [ring/ring-codec "1.0.0"]
  ;  [ring/ring-json "0.4.0"]
  ;  [fogus/ring-edn "0.3.0"]
   ;
  ;  [http-kit "2.1.19"]
  ;  [cheshire "5.5.0"]
  ;  [clj-time "0.9.0"]
   ;
  ;  [crypto-password "0.1.3"]
  ;  [crypto-random "1.2.0"]
  ;  [bidi "1.19.0"]
