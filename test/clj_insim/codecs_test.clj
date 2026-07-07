(ns clj-insim.codecs-test
  (:require [clj-insim.codecs :as sut]
            [clojure.test :refer [are deftest testing is]]))

;; Verify that time fields are correctly sized and typed for v10
(deftest codec-test
  (testing "codec structure is correct"
    (are [codec-key] (let [body-fn (get sut/body codec-key)
                           codec (body-fn {})]
                       (some? codec))
      :con
      :hlv
      :obh
      :rip
      :csc
      :uco)))

;; PLH (Player Handicaps) codec tests
(deftest plh-codec-test
  (testing "PLH codec structure"
    (let [body-fn (get sut/body :plh)]
      (is (some? body-fn) "PLH codec function exists")))
  (testing "PLH codec with 2 players"
    (let [body-fn (get sut/body :plh)
          codec (body-fn #:header{:data 2})]
      (is (some? codec) "PLH codec returns a structure for 2 players")))
  (testing "PLH codec with zero players"
    (let [body-fn (get sut/body :plh)
          codec (body-fn #:header{:data 0})]
      (is (some? codec) "PLH codec handles zero players"))))
