(defproject alwaysbcoding/spore "0.1.0-SNAPSHOT"

  :description "A Clojure framework for building web applications"

  :url "https://github.com/AlwaysBCoding/spore"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.datomic/datomic-pro "0.9.5327" :exclusions [org.clojure/clojure ... joda-time]]
                 [camel-snake-kebab "0.3.2"]
                 [com.stuartsierra/component "0.3.0"]
                 [bidi "1.21.1"]]

  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :username :env/DATOMIC_USERNAME
                                   :password :env/DATOMIC_PASSWORD}}

  :global-vars {*print-length* 25}

  :jvm-opts ["-Xss1g" "-Xmx4g" "-XX:MaxPermSize=256m"]

  :min-lein-version "2.0.0"

  :source-paths ["src"]

  :test-paths ["spec"]

)
