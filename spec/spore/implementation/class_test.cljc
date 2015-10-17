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
    :lastname {:type :string :required true}}})

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
  (spore/SporeClass Player player-manifest)
  (spore/SporeClass Team team-manifest)
  (spore/SporeClass PlayerGame player-game-manifest)
  (spore/SporeClass BasketballGameEvent basketball-game-event-manifest)
  (test-fn))

(defn create-database [test-fn]
  (d/create-database db-uri)
  (test-fn)
  (d/delete-database db-uri))

(defn sync-database-schema [test-fn]
  (d/transact (d/connect db-uri) (reduce into [] (vector (.schema (->Player))
                                                         (.schema (->Team))
                                                         (.schema (->PlayerGame))
                                                         (.schema (->BasketballGameEvent)))))
  (test-fn))

(use-fixtures :once create-spore-classes)
(use-fixtures :each create-database sync-database-schema)

;; Assertions
(testing "#manifest"
  (deftest returns-manifest
    (let [Player (->Player)]
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
    (let [Player (->Player)]
      (is (= 1
             (->> (.schema Player)
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
             (contains-map-keys? tx-data [:player/sporeID]))))))

(testing "#create"
  (deftest transacts-record
    (let [Player (->Player)
          result (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)]
      (is (= java.lang.Long
             (.getClass (:db/id result))))))

  (deftest can-return-id
    (let [Player (->Player)
          result (.create Player {:firstname "John" :lastname "Wall"} {:return :id} db-uri)]
      (is (= java.lang.Long
             (.getClass result)))))

  (deftest raises-error-if-parameter-is-not-in-manifest
    (let [Player (->Player)]
      (try
        (.create Player {:firstname "John"
                         :middlename "Hildred"
                         :lastname "Wall"} {} db-uri)
        (throw (ex-info "" {:message "No exception thrown in function that is expected to error"}))
        (catch Exception e
          (is (= (ex-data e)
                 {:model :player
                  :parameter :middlename}))))))

  (deftest raises-error-if-required-field-is-not-present
    (let [Player (->Player)]
      (try
        (.create Player {:firstname "John"} {} db-uri)
        (throw (ex-info "" {:message "No exception thrown in function that is expected to error"}))
        (catch Exception e
          (is (= (ex-data e)
                 {:model :player
                  :required-attributes '(:lastname)})))))))

(testing "#all"
  (deftest returns-all-instances
    (let [Player (->Player)
          Team (->Team)]
      (.create Player {:firstname "John" :lastname "Wall"} {} db-uri)
      (.create Player {:firstname "Bradley" :lastname "Beal"} {} db-uri)
      (.create Team {:market "Washington"} {} db-uri)
      (is (= 2
             (count (.all Player {} db-uri)))))))

(testing "#data")

(testing "#query")
