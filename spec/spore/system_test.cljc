(ns spore.system-test
  (:require [clojure.test :refer :all]
            [spec.helpers.matchers :refer :all]
            [com.stuartsierra.component :as component]
            [spore.core :as spore]
            [spore.system :as system]))

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
(defn create-spore-classes [test-fn]
  (spore/SporeClass Player player-manifest [:playerGame])
  (spore/SporeClass Team team-manifest [])
  (spore/SporeClass PlayerGame player-game-manifest [])
  (spore/SporeClass BasketballGameEvent basketball-game-event-manifest [])
  (test-fn))

(use-fixtures :once create-spore-classes)

(testing "#generate-system-map"
  (deftest generate-system-map-generates-component-system
    (is (= com.stuartsierra.component.SystemMap
           (-> (vector
                :player (->Player)
                :team (->Team)
                :playerGame (->PlayerGame)
                :basketball.gameEvent (->BasketballGameEvent))
               (system/generate-system-map)
               (.getClass)))))

  (deftest generate-system-map-creates-correct-dependency-graph
    (let [system-map (-> (vector
                          :player (->Player)
                          :team (->Team)
                          :playerGame (->PlayerGame)
                          :basketball.gameEvent (->BasketballGameEvent))
                         (system/generate-system-map)
                         (component/start))]

      (is (= true (nil? (-> system-map :playerGame :player))))
      (is (= false (nil? (-> system-map :player :playerGame)))))))
