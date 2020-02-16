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
