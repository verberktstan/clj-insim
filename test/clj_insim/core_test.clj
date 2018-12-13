(ns clj-insim.core-test
  (:require [clojure.test :refer :all]
            [clj-insim.parsers :refer [parse]]
            [clj-insim.packets :as packets]))

(deftest pipeline-test
  (testing "Parsing and constructing packets"
    (is (= (parse (drop 1 (packets/is-tiny)))
           {:type :tiny, :reqi 0, :subt-type :none}))))
