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

(def CIPlayer nil)
(def CITeam nil)
(def CIPlayerGame nil)
(def CIBasketballGameEvent nil)

;; Fixtures
(defn define-spore-instances [test-fn]
  (spore/SporeInstance iplayer)
  (spore/SporeInstance iteam)
  (spore/SporeInstance iplayer-game)
  (spore/SporeInstance ibasketball-game-event)
  (test-fn))

(defn define-spore-classes [test-fn]
  (spore/SporeClass IPlayer)
  (alter-var-root #'CIPlayer (fn [data] (->IPlayer player-manifest #(->iplayer %1 %2) [])))
  (spore/SporeClass ITeam)
  (alter-var-root #'CITeam (fn [data] (->ITeam team-manifest #(->iteam %1 %2) [])))
  (spore/SporeClass IPlayerGame)
  (alter-var-root #'CIPlayerGame (fn [data] (->IPlayerGame player-game-manifest #(->iplayer-game %1 %2) [])))
  (spore/SporeClass IBasketballGameEvent)
  (alter-var-root #'CIBasketballGameEvent (fn [data] (->IBasketballGameEvent basketball-game-event-manifest #(->ibasketball-game-event %1 %2) [])))
  (test-fn))

(defn create-database [test-fn]
  (d/create-database db-uri)
  (test-fn)
  (d/delete-database db-uri))

(defn sync-database-schema [test-fn]
  (d/transact (d/connect db-uri) (reduce into [] (vector (.schema CIPlayer)
                                                         (.schema CITeam)
                                                         (.schema CIPlayerGame)
                                                         (.schema CIBasketballGameEvent))))
  (test-fn))

(use-fixtures :once define-spore-instances define-spore-classes)
(use-fixtures :each create-database sync-database-schema)

;; Assertions
(testing "#id"
  (deftest id-returns-datomic-id
    (let [result (.create CIPlayer {:firstname "John" :lastname "Wall"} {} db-uri)]
      (is (= java.lang.Long 
             (.getClass (.id result))))
      (is (= java.util.UUID
             (.getClass (.id result {:source :sporeID}))))

      ;; Throws an error if id is called with an invalid source parameter
      )))

(testing "#attr"
  (deftest attr-returns-simple-attribute
    (let [result (.create CIPlayer {:firstname "John" :lastname "Wall"} {} db-uri)]
      (is (= "John"
             (.attr result :firstname)))))

  (deftest attr-returns-nil-if-no-attribute
    (let [result (.create CIPlayer {:firstname "John" :lastname "Wall"} {} db-uri)]
      (is (= nil
             (.attr result :middlename)))))
  
  ;; These two tests are not passing right now, because when a ref-type ident is defined, spore is finding the manifest
  ;; from the spore.model.x namespace. This needs to be refactored such that it's easier/possible to test
  #_(deftest attr-can-create-instance-of-relation-if-ref-type-is-defined
      (let [player (.create CIPlayer {:firstname "John" :lastname "Wall"} {} db-uri)
            player-game (.create CIPlayerGame {:player (.id player)} {} db-uri)]
        (is (= (.id player)
               (-> player-game (.attr :player) (.id))))))
  
  #_(deftest attr-returns-nil-if-no-attribute-ref-type
      (is (= true false)))

  )

(testing "#display")

(testing "#serialize")

(testing "#data")
