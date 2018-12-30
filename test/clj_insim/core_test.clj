(ns clj-insim.core-test
  (:require [clojure.test :refer :all]
            [clj-insim.parser :refer [parse]]
            [clj-insim.packets :as packets]))

(deftest pipeline-test
  (testing "Parsing and constructing packets"
    (is (= (parse (packets/is-tiny))
           {:size 4 :type :tiny, :reqi 1, :sub-type :none}))))
