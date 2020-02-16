(ns clj-insim.parse-test
  (:require [clj-insim.parse :as sut]
            [clojure.test :as t]))

(t/deftest header-test
  (let [packet {:type 3 :data 0}]
    (t/is
     (= {:type :tiny :data :none}
        (sut/header packet)))))

(t/deftest unparse-test
  (let [packet {:type :tiny :data :none}]
    (t/is
     (= {:type 3 :data 0}
        (sut/unparse packet)))))

(t/deftest body-test
  (let [packet {:in-game-cam 3
                :race-laps 1}]
    (t/is
     (= {:in-game-cam :driver
         :race-laps 1}
        (sut/body packet)))))
