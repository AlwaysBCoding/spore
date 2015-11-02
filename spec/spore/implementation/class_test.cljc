(ns spore.implementation.class-test
  (:require [clojure.test :refer :all]
            [spec.helpers.matchers :refer :all]
            [spore.core :as spore]
            [spore.protocol.manifest :refer (SporeManifest)]
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
     :lastname {:type :string}
     :jerseyNumber {:type :string}})

  (relations [self]
    {})

  (lifecycle [self]
    {}))

(defrecord TeamManifest []
  SporeManifest

  (inflections [self]
    {:ident :team
     :namespace :team
     :plural :teams
     :datomic-prefix :team})

  (schema [self]
    {:market {:type :string}
     :name {:type :string}
     :display {:type :string}})
  
  (relations [self]
    {})

  (lifecycle [self]
    {}))

;; Dynamic Vars
(def Player nil)
(def Team nil)

;; Fixtures
(defn define-spore-classes [test-fn]
  (spore/SporeClass CPlayer)
  (alter-var-root #'Player (fn [data] (->CPlayer (->PlayerManifest) [])))
  (spore/SporeClass CTeam)
  (alter-var-root #'Team (fn [data] (->CTeam (->TeamManifest) [])))
  (test-fn))

(defn create-database [test-fn]
  (d/create-database db-uri)
  (test-fn)
  (d/delete-database db-uri))

(use-fixtures :once define-spore-classes)
(use-fixtures :each create-database)

;; Assertions
(testing "Spore Manifest"
  (testing "inflections"
    (deftest ident-returns-ident-single-word
      (is (= (.ident Player) :player))))
  
  (testing "schema"
    (deftest schema-adds-spore-id-to-schema
      (is (= 1
             (->> (.schema Player)
                  (filter #(contains-submap? % {:db/ident :player/sporeID
                                                :db/valueType :db.type/uuid
                                                :db/unique :db.unique/identity}))
                  (count)))))))

(testing "Spore Class"
  (testing "#build"
    (deftest build-builds-simple-record
      (let [tx-data (.build Player {:firstname "John"
                                    :lastname "Wall"})]
        (is (= true
               (contains-submap? tx-data {:player/firstname "John"
                                          :player/lastname "Wall"})))
        
        (is (= true
               (contains-map-keys? tx-data [:player/sporeID]))))))
  
  (testing "#create"
    (deftest create-transacts-record
      (let [result (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)]
        (is (= java.lang.Long
               (.getClass (.id result))))))
    
    (deftest create-can-return-id
      (let [result (.create Player {:firstname "John" :lastname "Wall"} {:return :id} db-uri)]
        (is (= java.lang.Long
               (.getClass result)))))
    
    (deftest create-raises-error-if-parameter-is-not-in-manifest
      (try
        (.create Player {:firstname "John"
                         :middlename "Hildred"
                         :lastname "Wall"} {} db-uri)
        (throw (ex-info "" {:message "No exception thrown in function that is expected to error"}))
        (catch Exception e
          (is (= (ex-data e)
                 {:model :player
                  :parameter :middlename})))))

    (deftest create-raises-error-if-required-field-is-not-present
      (try
        (.create Player {:firstname "John"} {} db-uri)
        (throw (ex-info "" {:message "No exception thrown in function that is expected to error"}))
        (catch Exception e
          (is (= (ex-data e)
                 {:model :player
                  :required-attributes '(:lastname)}))))))
  
  (testing "#all"
    (deftest all-returns-all-instances
      (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)
      (.create Player {:firstname "Bradley" :lastname "Beal"} {} db-uri)
      (.create Team {:market "Washington"} {} db-uri)
      (is (= 2
             (count (.all Player {} db-uri))))))
  
  (testing "#where"
    (deftest where-returns-all-instances-single-attribute
      (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)
      (.create Player {:firstname "John" :lastname "Smith"} {} db-uri)
      (.create Player {:firstname "John" :lastname "Tyler"} {} db-uri)
      (.create Player {:firstname "Bradley" :lastname "Beal"} {} db-uri)
      (is (= 3
             (count (.where Player {:firstname "John"} {} db-uri)))))
    
    (deftest where-returns-all-instances-compound-attribute
      (.create Player {:firstname "John" :lastname "Wall" :jerseyNumber 2} {} db-uri)
      (.create Player {:firstname "John" :lastname "Smith" :jerseyNumber 2} {} db-uri)
      (.create Player {:firstname "John" :lastname "Tyler" :jerseyNumber 15} {} db-uri)
      (.create Player {:firstname "Bradley" :lastname "Beal" :jerseyNumber 3} {} db-uri)
      (is (= 2
             (count (.where Player {:firstname "John" :jerseyNumber 2} {} db-uri)))))

    (deftest where-raises-error-if-parameter-is-not-in-manifest
      (.create Player {:firstname "John" :lastname "Wall" :jerseyNumber 2} {} db-uri)
      (try
        (.where Player {:firstname "John" :middlename "Hildred"} {} db-uri)
        (throw (ex-info "" {:message "No exception thrown in function that is expected to error"}))
        (catch Exception e
          (is (= (ex-data e)
                 {:model :player
                  :parameter :middlename}))))))

  (testing "#detect"
    (deftest detect-returns-instance-single-attribute
      (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)
      (.create Player {:firstname "Bradley" :lastname "Beal"} {} db-uri)
      (is (= "John"
             (:player/firstname (.detect Player {:lastname "Wall"} {:return :entity} db-uri)))))
    
    (deftest detect-returns-instance-compound-attribute
      (.create Player {:firstname "John" :lastname "Wall" :jerseyNumber 2} {} db-uri)
      (.create Player {:firstname "John" :lastname "Tyler" :jerseyNumber 15} {} db-uri)
      (.create Player {:firstname "Bradley" :lastname "Beal" :jerseyNumber 3} {} db-uri)
      (is (= "Wall"
             (:player/lastname (.detect Player {:firstname "John" :jerseyNumber 2} {:return :entity} db-uri)))))
    
    (deftest detect-raises-error-if-parameter-is-not-in-manifest
      (.create Player {:firstname "John" :lastname "Wall" :jerseyNumber 2} {} db-uri)
      (try
        (.detect Player {:firstname "John" :middlename "Hildred"} {} db-uri)
        (throw (ex-info "" {:message "No exception thrown in function that is expected to error"}))
        (catch Exception e
          (is (= (ex-data e)
                 {:model :player
                  :parameter :middlename})))))

    (deftest detect-returns-nil-if-no-record-present
      (is (= nil
             (.detect Player {:lastname "Wall"} {} db-uri)))))

  (testing "#lookup"
    (deftest lookup-returns-record
      (let [player (.create Player {:firstname "John" :lastname "Wall"} {:return :entity} db-uri)]
        (is (= "Wall"
               (:player/lastname (.lookup Player (:db/id player) {:return :entity} db-uri))))))
    
    (deftest lookup-returns-nil-if-no-record-found
      (let [player (.create Player {:firstname "John" :lastname "Wall"} {:return :entity} db-uri)]
        (is (= nil
               (.lookup Player 123456789 {} db-uri))))))
  
  (testing "#one"
    (deftest one-returns-record
      (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)
      (.create Player {:firstname "Bradley" :lastname "Beal"} {} db-uri)
      (is (= true
             (not (nil? (:db/id (.one Player {:return :entity} db-uri))))))))

  (testing "#detect-or-create"
    (deftest detect-or-create-detects-properly
      (let [player (.create Player {:firstname "John" :lastname "Wall"} {:return :entity} db-uri)]
        (is (= (:db/id player)
               (:db/id (.detect-or-create Player {:firstname "John" :lastname "Wall"} {:return :entity} db-uri))))))
    
    (deftest detect-or-create-creates-properly
      (let [player (.detect-or-create Player {:firstname "John" :lastname "Wall"} {:return :entity} db-uri)]
        (is (= java.lang.Long
               (.getClass (:db/id player)))))))
  
  (testing "#destroy-all"
    (deftest destroy-all-destroys-properly
      (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)
      (.create Player {:firstname "Bradley" :lastname "Beal"} {} db-uri)
      (is (= 2
             (count (.all Player {} db-uri))))
      (.destroy-all Player {} db-uri)
      (is (= 0
             (count (.all Player {} db-uri))))))
  
  (testing "#destroy-where"
    (deftest destroy-where-destroys-properly
      (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)
      (.create Player {:firstname "John" :lastname "Tyler"} {} db-uri)
      (.create Player {:firstname "Bradley" :lastname "Beal"} {} db-uri)
      (is (= 3
             (count (.all Player {} db-uri))))
      (.destroy-where Player {:firstname "John"} {} db-uri)
      (is (= 1
             (count (.all Player {} db-uri))))))
  
  
  (testing "#data")
  (testing "#query"))

;; (defn sync-database-schema [test-fn]
;;   (d/transact (d/connect db-uri) (reduce into [] (vector (.schema CRPlayer)
;;                                                          (.schema CRTeam)
;;                                                          (.schema CRPlayerGame)
;;                                                          (.schema CRBasketballGameEvent))))
;;   (test-fn))

;; (use-fixtures :once define-spore-instances define-spore-classes)
;; (use-fixtures :each create-database sync-database-schema)

;; (def player-game-manifest
;;   {:playerGame
;;    {:player {:type :ref :ref-type :player}
;;     :game {:type :ref :ref-type :game}
;;     :statModel {:type :ref}}})

;; (def basketball-game-event-manifest
;;   {:basketball.gameEvent
;;    {:game {:type :ref :ref-type :game}
;;     :display {:type :string}}})

;; (def CRPlayer nil)
;; (def CRTeam nil)
;; (def CRPlayerGame nil)
;; (def CRBasketballGameEvent nil)

;; ;; Fixtures
;; (defn define-spore-instances [test-fn]
;;   (spore/SporeInstance cplayer)
;;   (spore/SporeInstance cteam)
;;   (spore/SporeInstance cplayer-game)
;;   (spore/SporeInstance cbasketball-game-event)
;;   (test-fn))

;; (defn define-spore-classes [test-fn]
;;   (spore/SporeClass CPlayer)
;;   (alter-var-root #'CRPlayer (fn [data] (->CPlayer player-manifest #(->cplayer %1 %2) [])))
;;   (spore/SporeClass CTeam)
;;   (alter-var-root #'CRTeam (fn [data] (->CTeam team-manifest #(->cteam %1 %2) [])))
;;   (spore/SporeClass CPlayerGame)
;;   (alter-var-root #'CRPlayerGame (fn [data] (->CPlayerGame player-game-manifest #(->cplayer-game %1 %2) [])))
;;   (spore/SporeClass CBasketballGameEvent)
;;   (alter-var-root #'CRBasketballGameEvent (fn [data] (->CBasketballGameEvent basketball-game-event-manifest #(->cbasketball-game-event %1 %2) [])))
;;   (test-fn))

;; ;; Assertions
