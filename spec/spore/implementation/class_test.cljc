(ns spore.imeplementation.class-test
  (:require [clojure.test :refer :all]
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
(defn create-database [test-fn]
  (d/create-database db-uri)  
  (test-fn)
  (d/delete-database db-uri))

(defn create-spore-class [test-fn]
  (spore/SporeClass Player player-manifest)
  (spore/SporeClass PlayerGame player-game-manifest)
  (spore/SporeClass BasketballGameEvent basketball-game-event-manifest)
  (test-fn))

(use-fixtures :once create-database create-spore-class)

;; Assertions
(testing "#manifest"
  (deftest returns-manifest
    (let [Player (->Player)
          PlayerGame (->PlayerGame)
          BasketballGameEvent (->BasketballGameEvent)]
      (is (= (.manifest Player) player-manifest))
      (is (= (.manifest PlayerGame) player-game-manifest))
      (is (= (.manifest BasketballGameEvent) basketball-game-event-manifest)))))

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

(testing "#schema")

(testing "#all")

(testing "#data")

(testing "#query")
