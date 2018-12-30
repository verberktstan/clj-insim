(ns clj-insim.parser-test
  (:require [clojure.test :refer :all]
            [clj-insim.parser :refer [parse]]))

(deftest parse-test
  (testing "Parsing an IS_VER packet"
    (is (= (parse [20 2 1 0 48 46 54 84 0 0 0 0 83 51 0 0 0 0 8 0])
           {:size 20 :type :ver, :reqi 1, :data 0,
            :version "0.6T", :product "S3",
            :insim-version 8, :spare 0})))
  (testing "Parsing an IS_FLG packet"
    (is (= (parse [8 32 0 3 0 2 0 0])
           {:size 8 :type :flg, :reqi 0, :player-id 3, :off-on 0, :flag :yellow-caused, :car-behind 0, :spare-3 0}))))
