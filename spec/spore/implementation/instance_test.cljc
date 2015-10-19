(ns spore.implementation.instance-test
  (:require [clojure.test :refer :all]
            [spec.helpers.matchers :refer :all]
            [spore.core :as spore]
            [datomic.api :as d]))

;; Configuration + Mock Data
(def db-uri "datomic:mem://spore-db")

(def player-manifest
  {:player
   {:firstname {:type :string}
    :lastname {:type :string :required true}
    :jerseyNumber {:type :long}}})

(def team-manifest
  {:team
   {:market {:type :string}
    :name {:type :string}
    :display {:type :string}}})

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
(defn define-spore-instances [test-fn]
  (spore/SporeInstance iplayer)
  (spore/SporeInstance iteam)
  (spore/SporeInstance iplayer-game)
  (spore/SporeInstance ibasketball-game-event)
  (test-fn))

(defn define-spore-classes [test-fn]
  (spore/SporeClass IPlayer player-manifest #(->iplayer %1 %2) [])
  (spore/SporeClass ITeam team-manifest #(->iteam %1 %2) [])
  (spore/SporeClass IPlayerGame player-game-manifest #(->iplayer-game %1 %2) [])
  (spore/SporeClass IBasketballGameEvent basketball-game-event-manifest #(->ibasketball-game-event %1 %2) [])
  (test-fn))

(defn create-database [test-fn]
  (d/create-database db-uri)
  (test-fn)
  (d/delete-database db-uri))

(defn sync-database-schema [test-fn]
  (d/transact (d/connect db-uri) (reduce into [] (vector (.schema (->IPlayer))
                                                         (.schema (->ITeam))
                                                         (.schema (->IPlayerGame))
                                                         (.schema (->IBasketballGameEvent)))))
  (test-fn))

(use-fixtures :once define-spore-instances define-spore-classes)
(use-fixtures :each create-database sync-database-schema)

;; Assertions
(testing "#id"
  (deftest id-returns-datomic-id
    (let [Player (->IPlayer)
          result (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)]
      (is (= java.lang.Long 
             (.getClass (.id result))))
      (is (= java.util.UUID
             (.getClass (.id result {:source :sporeID}))))

      ;; Throws an error if id is called with an invalid source parameter
)))

(testing "#display")
(testing "#serialize")
(testing "#data")
