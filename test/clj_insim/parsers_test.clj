(ns clj-insim.parsers-test
  (:require [clojure.test :refer :all]
            [clj-insim.enums :as enums]
            [clj-insim.parsers :refer :all]))

(deftest parse-is-ver-test
  (testing "Parsing an IS_VER packet"
    (let [packet [2 1 0 48 46 54 84 0 0 0 0 83 51 0 0 0 0 8 0]
          type-key (enums/isp-key (first packet))
          parsed (parse type-key packet)]
      (is (= parsed
             {:type :ver, :reqi 1, :zero 0,
              :version "0.6T", :product "S3",
              :insim-version 8, :spare 0})))))

(deftest parse-is-flg-test
  (testing "Parsing an IS_FLG packet"
    (let [packet [32 0 3 0 2 0 0]
          type-key (enums/isp-key (first packet))
          parsed (parse type-key packet)]
      (is (= parsed
             {:type :flg, :reqi 0, :player-id 3, :off-on 0, :flag :yellow-caused, :car-behind 0, :spare-3 0})))))
