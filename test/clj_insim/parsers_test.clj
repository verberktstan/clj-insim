(ns clj-insim.parsers-test
  (:require [clojure.test :refer :all]
            [clj-insim.enums :as enums]
            [clj-insim.parsers :refer :all]))

(deftest parse-test
  (testing "Parsing an IS_VER packet"
    (let [packet [2 1 0 48 46 54 84 0 0 0 0 83 51 0 0 0 0 8 0]
          type-key (enums/isp-key (first packet))
          parsed (parse type-key packet)]
      (is (= parsed
             {:type :ver, :reqi 1, :zero 0,
              :version "0.6T", :product "S3",
              :insim-version 8, :spare 0})))))
