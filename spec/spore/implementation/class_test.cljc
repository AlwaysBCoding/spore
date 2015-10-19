(ns spore.implementation.class-test
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
  (spore/SporeInstance cplayer)
  (spore/SporeInstance cteam)
  (spore/SporeInstance cplayer-game)
  (spore/SporeInstance cbasketball-game-event)
  (test-fn))

(defn define-spore-classes [test-fn]
  (spore/SporeClass CPlayer player-manifest #(->cplayer %1 %2) [])
  (spore/SporeClass CTeam team-manifest #(->cteam %1 %2) [])
  (spore/SporeClass CPlayerGame player-game-manifest #(->cplayer-game %1 %2) [])
  (spore/SporeClass CBasketballGameEvent basketball-game-event-manifest #(->cbasketball-game-event %1 %2) [])
  (test-fn))

(defn create-database [test-fn]
  (d/create-database db-uri)
  (test-fn)
  (d/delete-database db-uri))

(defn sync-database-schema [test-fn]
  (d/transact (d/connect db-uri) (reduce into [] (vector (.schema (->CPlayer))
                                                         (.schema (->CTeam))
                                                         (.schema (->CPlayerGame))
                                                         (.schema (->CBasketballGameEvent)))))
  (test-fn))

(use-fixtures :once define-spore-instances define-spore-classes)
(use-fixtures :each create-database sync-database-schema)

;; Assertions
(testing "#ident"
  (deftest ident-returns-ident-single-word
    (let [Player (->CPlayer)]
      (is (= (.ident Player) :player))))

  (deftest ident-returns-ident-compound-word
    (let [PlayerGame (->CPlayerGame)]
      (is (= (.ident PlayerGame) :playerGame))))

  (deftest ident-returns-ident-namespaced-word
    (let [BasketballGameEvent (->CBasketballGameEvent)]
      (is (= (.ident BasketballGameEvent) :basketball.gameEvent)))))

(testing "#schema"
  (deftest schema-adds-spore-id-to-schema
    (let [Player (->CPlayer)]
      (is (= 1
             (->> (.schema Player)
                  (filter #(contains-submap? % {:db/ident :player/sporeID
                                                :db/valueType :db.type/uuid
                                                :db/unique :db.unique/identity}))
                  (count)))))))

(testing "#build"
  (deftest build-builds-simple-record
    (let [Player (->CPlayer)
          tx-data (.build Player {:firstname "John"
                                  :lastname "Wall"})]
      (is (= true
             (contains-submap? tx-data {:player/firstname "John"
                                        :player/lastname "Wall"})))

      (is (= true
             (contains-map-keys? tx-data [:player/sporeID]))))))

(testing "#create"
  (deftest create-transacts-record
    (let [Player (->CPlayer)
          result (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)]
      (is (= java.lang.Long
             (.getClass (:db/id result))))))

  (deftest create-can-return-id
    (let [Player (->CPlayer)
          result (.create Player {:firstname "John" :lastname "Wall"} {:return :id} db-uri)]
      (is (= java.lang.Long
             (.getClass result)))))

  (deftest create-raises-error-if-parameter-is-not-in-manifest
    (let [Player (->CPlayer)]
      (try
        (.create Player {:firstname "John"
                         :middlename "Hildred"
                         :lastname "Wall"} {} db-uri)
        (throw (ex-info "" {:message "No exception thrown in function that is expected to error"}))
        (catch Exception e
          (is (= (ex-data e)
                 {:model :player
                  :parameter :middlename}))))))

  (deftest create-raises-error-if-required-field-is-not-present
    (let [Player (->CPlayer)]
      (try
        (.create Player {:firstname "John"} {} db-uri)
        (throw (ex-info "" {:message "No exception thrown in function that is expected to error"}))
        (catch Exception e
          (is (= (ex-data e)
                 {:model :player
                  :required-attributes '(:lastname)})))))))

(testing "#all"
  (deftest all-returns-all-instances
    (let [Player (->CPlayer)
          Team (->CTeam)]
      (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)
      (.create Player {:firstname "Bradley" :lastname "Beal"} {} db-uri)
      (.create Team {:market "Washington"} {} db-uri)
      (is (= 2
             (count (.all Player {} db-uri)))))))

(testing "#where"
  (deftest where-returns-all-instances-single-attribute
    (let [Player (->CPlayer)]
      (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)
      (.create Player {:firstname "John" :lastname "Smith"} {} db-uri)
      (.create Player {:firstname "John" :lastname "Tyler"} {} db-uri)
      (.create Player {:firstname "Bradley" :lastname "Beal"} {} db-uri)
      (is (= 3
             (count (.where Player {:firstname "John"} {} db-uri))))))

  (deftest where-returns-all-instances-compound-attribute
    (let [Player (->CPlayer)]
      (.create Player {:firstname "John" :lastname "Wall" :jerseyNumber 2} {} db-uri)
      (.create Player {:firstname "John" :lastname "Smith" :jerseyNumber 2} {} db-uri)
      (.create Player {:firstname "John" :lastname "Tyler" :jerseyNumber 15} {} db-uri)
      (.create Player {:firstname "Bradley" :lastname "Beal" :jerseyNumber 3} {} db-uri)
      (is (= 2
             (count (.where Player {:firstname "John" :jerseyNumber 2} {} db-uri))))))

  (deftest where-raises-error-if-parameter-is-not-in-manifest
    (let [Player (->CPlayer)]
      (.create Player {:firstname "John" :lastname "Wall" :jerseyNumber 2} {} db-uri)
      (try
        (.where Player {:firstname "John" :middlename "Hildred"} {} db-uri)
        (throw (ex-info "" {:message "No exception thrown in function that is expected to error"}))
        (catch Exception e
          (is (= (ex-data e)
                 {:model :player
                  :parameter :middlename})))))))

(testing "#detect"
  (deftest detect-returns-instance-single-attribute
    (let [Player (->CPlayer)]
      (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)
      (.create Player {:firstname "Bradley" :lastname "Beal"} {} db-uri)
      (is (= "John"
             (:player/firstname (.detect Player {:lastname "Wall"} {} db-uri))))))

  (deftest detect-returns-instance-compound-attribute
    (let [Player (->CPlayer)]
      (.create Player {:firstname "John" :lastname "Wall" :jerseyNumber 2} {} db-uri)
      (.create Player {:firstname "John" :lastname "Tyler" :jerseyNumber 15} {} db-uri)
      (.create Player {:firstname "Bradley" :lastname "Beal" :jerseyNumber 3} {} db-uri)
      (is (= "Wall"
             (:player/lastname (.detect Player {:firstname "John" :jerseyNumber 2} {} db-uri))))))
  
  (deftest detect-raises-error-if-parameter-is-not-in-manifest
    (let [Player (->CPlayer)]
      (.create Player {:firstname "John" :lastname "Wall" :jerseyNumber 2} {} db-uri)
      (try
        (.detect Player {:firstname "John" :middlename "Hildred"} {} db-uri)
        (throw (ex-info "" {:message "No exception thrown in function that is expected to error"}))
        (catch Exception e
          (is (= (ex-data e)
                 {:model :player
                  :parameter :middlename}))))))

  (deftest detect-returns-nil-if-no-record-present
    (let [Player (->CPlayer)]
      (is (= nil
             (.detect Player {:lastname "Wall"} {} db-uri))))))

(testing "#lookup"
  (deftest lookup-returns-record
    (let [Player (->CPlayer)
          player (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)]
      (is (= "Wall"
             (:player/lastname (.lookup Player (:db/id player) {} db-uri))))))

  (deftest lookup-returns-nil-if-no-record-found
    (let [Player (->CPlayer)
          player (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)]
      (is (= nil
             (.lookup Player (inc (:db/id player)) {} db-uri))))))

(testing "#one"
  (deftest one-returns-record
    (let [Player (->CPlayer)]
      (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)
      (.create Player {:firstname "Bradley" :lastname "Beal"} {} db-uri)
      (is (= true
             (not (nil? (:db/id (.one Player {} db-uri)))))))))

(testing "#detect-or-create"
  (deftest detect-or-create-detects-properly
    (let [Player (->CPlayer)
          player (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)]
      (is (= (:db/id player)
             (:db/id (.detect-or-create Player {:firstname "John" :lastname "Wall"} {} db-uri))))))

  (deftest detect-or-create-creates-properly
    (let [Player (->CPlayer)
          player (.detect-or-create Player {:firstname "John" :lastname "Wall"} {} db-uri)]
      (is (= java.lang.Long
             (.getClass (:db/id player)))))))

(testing "#destroy-all"
  (deftest destroy-all-destroys-properly
    (let [Player (->CPlayer)]
      (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)
      (.create Player {:firstname "Bradley" :lastname "Beal"} {} db-uri)
      (is (= 2
             (count (.all Player {} db-uri))))
      (.destroy-all Player {} db-uri)
      (is (= 0
             (count (.all Player {} db-uri)))))))

(testing "#destroy-where"
  (deftest destroy-where-destroys-properly
    (let [Player (->CPlayer)]
      (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)
      (.create Player {:firstname "John" :lastname "Tyler"} {} db-uri)
      (.create Player {:firstname "Bradley" :lastname "Beal"} {} db-uri)
      (is (= 3
             (count (.all Player {} db-uri))))
      (.destroy-where Player {:firstname "John"} {} db-uri)
      (is (= 1
             (count (.all Player {} db-uri)))))))

(testing "#data")
(testing "#query")
