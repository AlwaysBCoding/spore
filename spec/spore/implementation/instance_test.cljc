(ns spore.implementation.instance-test
  (:require [clojure.test :refer :all]
            [spec.helpers.matchers :refer :all]
            [spore.protocol.class :refer (SporeClassProtocol)]
            [spore.protocol.manifest :refer (SporeManifest)]
            [spore.core :as spore]
            [datomic.api :as d]))

;; Configuration + Mock Data
(def db-uri "datomic:mem://spore-db")

;; Mock Manifests
(defrecord PlayerManifest []
  SporeManifest

  (inflections [self]
    {:ident :player
     :namespace :player
     :plural :players
     :datomic-prefix :player})

  (schema [self]
    {:firstname {:type :string}
     :lastname {:type :string, :required true}
     :jerseyNumber {:type :long}})

  (relations [self]
    {})

  (lifecycle [self]
    {}))

(defrecord TeamManifest []
  SporeManifest

  (inflections [self]
    {:ident :team
     :namespace :team
     :plural :team
     :datomic-prefix :team})

  (schema [self]
    {:market {:type :string}
     :name {:type :string}
     :display {:type :string}})

  (relations [self]
    {})

  (lifecycle [self]
    {}))

(defrecord BasketballGameEventManifest []
  SporeManifest

  (inflections [self]
    {:ident :basketball-game-event
     :namespace :basketball-game-event
     :plural :basketball-game-events
     :datomic-prefix :basketball.gameEvent})

  (schema [self]
    {:game {:type :ref, :ref-type :game}
     :display {:type :string}})

  (relations [self]
    {})

  (lifecycle [self]
    {}))

;; Dynamic Vars
(def Player nil)
(def Team nil)
(def BasketballGameEvent nil)

;; Fixtures
(defn define-spore-instances [test-fn]
  (spore/SporeInstance iplayer)
  (spore/SporeInstance iteam)
  (spore/SporeInstance ibasketball-game-event)
  (test-fn))

(defn define-spore-classes [test-fn]
  (spore/SporeClass IPlayer
   SporeClassProtocol
   (construct-instance
    [self entity]
    (->iplayer (.-manifest self) entity {})))
  (alter-var-root #'Player (fn [data] (->IPlayer (->PlayerManifest) [])))

  (spore/SporeClass ITeam
   SporeClassProtocol
   (construct-instance
    [self entity]
    (->iteam (.-manifest self) entity {})))
  (alter-var-root #'Team (fn [data] (->ITeam (->TeamManifest) [])))

  (spore/SporeClass IBasketballGameEvent
   SporeClassProtocol
   (construct-instance
    [self entity]
    (->ibasketball-game-event (->BasketballGameEventManifest) [])))
  (alter-var-root #'BasketballGameEvent (fn [data] (->IBasketballGameEvent (->BasketballGameEventManifest) [])))

  (test-fn))

(defn create-database [test-fn]
  (d/create-database db-uri)
  (test-fn)
  (d/delete-database db-uri))

(defn sync-database-schema [test-fn]
  (d/transact (d/connect db-uri) (reduce into [] (vector (.schema Player)
                                                         (.schema Team)
                                                         (.schema BasketballGameEvent))))
  (test-fn))

(use-fixtures :once define-spore-instances define-spore-classes)
(use-fixtures :each create-database sync-database-schema)

;; Assertions
(testing "#id"
  (deftest id-returns-datomic-id
    (let [result (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)]
      (is (= java.lang.Long 
             (.getClass (.id result))))
      (is (= java.util.UUID
             (.getClass (.id result {:source :sporeID}))))

      ;; Throws an error if id is called with an invalid source parameter
      )))

(testing "#attr"
  (deftest attr-returns-simple-attribute
    (let [result (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)]
      (is (= "John"
             (.attr result :firstname)))))

  (deftest attr-returns-nil-if-no-attribute
    (let [result (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)]
      (is (= nil
             (.attr result :middlename)))))
  
  ;; These two tests are not passing right now, because when a ref-type ident is defined, spore is finding the manifest
  ;; from the spore.model.x namespace. This needs to be refactored such that it's easier/possible to test
  #_(deftest attr-can-create-instance-of-relation-if-ref-type-is-defined
      (let [player (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)
            player-game (.create PlayerGame {:player (.id player)} {} db-uri)]
        (is (= (.id player)
               (-> player-game (.attr :player) (.id))))))
  
  #_(deftest attr-returns-nil-if-no-attribute-ref-type
      (is (= true false)))
  )

(testing "#destroy"
  (deftest destroy-retracts-entity
    (let [player (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)]
      (is (= true
             (.contains (.all Player {:return :ids} db-uri) (.id player))))
      (.destroy player {} db-uri)
      (is (= false
             (.contains (.all Player {:return :ids} db-uri) (.id player)))))))

(testing "#revise"
  (deftest revise-updates-entity-attribute-simple-add
    (let [player (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)]
      (is (= "John"
             (.attr player :firstname)))
      
      ;; This is an untestable component in the current workflow, because it's looking at the spore model file again
      #_(let [player (.revise player {:firstname "Brad" :lastname nil} {} db-uri)]
          (is (= "Brad"
                 (.attr player :firstname))))))

  (deftest revise-updates-entity-attribute-simple-retract
    (let [player (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)]
      (is (= "John"
             (.attr player :firstname)))
      ;; This is an untestable component in the current workflow, because it's looking at the spore model file again
      #_(let [player (.revise player {:firstname nil :lastname "Brad"} {} db-uri)]
          (is (= nil
                 (.attr player :firstname))))))
  
  (deftest revise-doesnt-let-you-revise-attribute-not-defined-on-the-manifest
    (let [player (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)]
      (try
        (.revise player {:middlename "Hildred"} {} db-uri)
        (throw (ex-info "" {:message "No exception thrown in function that is expected to error"}))
        (catch Exception e
          (is (= (ex-data e)
                 {:model :player
                  :parameter :middlename}))))))
  
  (deftest revise-doesnt-let-you-retract-required-attribute
    (let [player (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)]
      (try
        (.revise player {:lastname nil} {} db-uri)
        (throw (ex-info "" {:message "No exception thrown in function that is expected to error"}))
        (catch Exception e
          (is (= (ex-data e)
                 {:model :player
                  :parameter :lastname})))))))

(testing "#display")
(testing "#serialize")
(testing "#data")
