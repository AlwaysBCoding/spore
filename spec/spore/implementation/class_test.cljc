(ns spore.imeplementation.class-test
  (:require [clojure.test :refer :all]
            [spec.helpers.matchers :refer :all]
            [spore.core :as spore]
            [datomic.api :as d]))

;; Configuration + Mock Data
(def db-uri "datomic:mem://spore-db")

(def player-manifest
  {:player
   {:firstname {:type :string}
    :lastname {:type :string}}})

(def player-game-manifest
  {:playerGame
   {:player {:type :ref :ref-type :player}
    :game {:type :ref :ref-type :game}
    :statModel {:type :ref}}})

(def basketball-game-event-manifest
  {:basketball.gameEvent
   {:game {:type :ref :ref-type :game}
    :display {:type :string}}})

;; Fixtures
(defn create-spore-class [test-fn]
  (spore/SporeClass Player player-manifest)
  (spore/SporeClass PlayerGame player-game-manifest)
  (spore/SporeClass BasketballGameEvent basketball-game-event-manifest)
  (test-fn))

(defn create-database [test-fn]
  (d/create-database db-uri)
  (test-fn)
  (d/delete-database db-uri))

(defn sync-database-schema [test-fn]
  (d/transact (d/connect db-uri) (.schema (->Player)))
  (test-fn))

(use-fixtures :once create-spore-class)
(use-fixtures :each create-database sync-database-schema)

;; Assertions
(testing "#manifest"
  (deftest returns-manifest
    (let [Player (->Player)
          PlayerGame (->PlayerGame)
          BasketballGameEvent (->BasketballGameEvent)]
      (is (= (.manifest Player) player-manifest)))))

(testing "#ident"
  (deftest returns-ident-single-word
    (let [Player (->Player)]
      (is (= (.ident Player) :player))))

  (deftest returns-ident-compound-word
    (let [PlayerGame (->PlayerGame)]
      (is (= (.ident PlayerGame) :playerGame))))

  (deftest returns-ident-namespaced-word
    (let [BasketballGameEvent (->BasketballGameEvent)]
      (is (= (.ident BasketballGameEvent) :basketball.gameEvent)))))

(testing "#schema"
  (deftest adds-spore-id-to-schema
    (let [Player (->Player)
          schema (.schema Player)]
      (is
       (= 1
          (->> schema
               (filter #(contains-submap? % {:db/ident :player/sporeID
                                             :db/valueType :db.type/uuid
                                             :db/unique :db.unique/identity}))
               (count)))))))

(testing "#build"
  (deftest builds-simple-record
    (let [Player (->Player)
          tx-data (.build Player {:firstname "John"
                                  :lastname "Wall"})]
      (is (= true
             (contains-submap? tx-data {:player/firstname "John"
                                        :player/lastname "Wall"})))

      (is (= true
             (contains-map-keys? tx-data [:player/sporeID])))))

  (deftest raises-error-if-parameter-is-not-in-manifest
    (let [Player (->Player)]
      (try
        (.build Player {:firstname "John"
                        :middlename "Hildred"})
        (catch Exception e
          (is (= "Called #build with a parameter that is not defined on the manifest"
                 (.getMessage e)))
          (is (= (ex-data e)
                 {:model :player
                  :parameter :middlename})))))))

(testing "#create"
  (deftest transacts-record
    (let [Player (->Player)
          result (.create Player {:firstname "Bradley" :lastname "Beal"} {} db-uri)]
      (is (= java.lang.Long
             (.getClass (:db/id result))))))

  (deftest can-return-id
    (let [Player (->Player)
          result (.create Player {:firstname "Bradley" :lastname "Beal"} {:return :id} db-uri)]
      (is (= java.lang.Long
             (.getClass result))))))

(testing "#all")

(testing "#data")

(testing "#query")
