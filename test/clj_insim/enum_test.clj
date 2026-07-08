(ns clj-insim.enum-test
  (:require [clj-insim.enum :as sut]
            [clojure.test :refer [deftest testing is are]]))

(deftest encode-test
  (testing "encode"
    (let [encode (sut/encode [:a :b :c])]
      (are [x y] (= x y)
        0 (encode :a)
        1 (encode :b)
        nil (encode :z))))
  (testing "accepts only sequential enum"
    (is (ifn? (sut/encode '(:x :y :z))))
    (is (thrown? AssertionError (sut/encode {:test "map"})))))

(deftest decode-test
  (testing "decode"
    (let [decode (sut/decode [:a :b :c])]
      (are [x y] (= x y)
        :a (decode 0)
        :b (decode 1)
        nil (decode 100))))
  (testing "accepts only sequential enum"
    (is (fn? (sut/decode '(:x :y :z))))
    (is (thrown? AssertionError (sut/decode {:test "map"})))))

;; AI Packet Enums Tests
(deftest ai-control-inputs-test
  (testing "AI_CONTROL_INPUTS enum"
    (let [encode (sut/encode sut/AI_CONTROL_INPUTS)
          decode (sut/decode sut/AI_CONTROL_INPUTS)]
      (are [x y] (= x y)
        0 (encode :steer)
        1 (encode :throttle)
        2 (encode :brake)
        3 (encode :shift-up)
        4 (encode :shift-down)
        19 (encode :fogfront)
        :steer (decode 0)
        :throttle (decode 1)
        :fogfront (decode 19))))
  (testing "AI_CONTROL_INPUTS has 20 entries"
    (is (= 20 (count sut/AI_CONTROL_INPUTS)))))

(deftest ai-control-special-test
  (testing "AI_CONTROL_SPECIAL map contains special commands"
    (are [x y] (= x y)
      240 (:send-ai-info sut/AI_CONTROL_SPECIAL)
      241 (:repeat-ai-info sut/AI_CONTROL_SPECIAL)
      253 (:set-help-flags sut/AI_CONTROL_SPECIAL)
      254 (:reset-inputs sut/AI_CONTROL_SPECIAL)
      255 (:stop-control sut/AI_CONTROL_SPECIAL))))

(deftest ai-flags-test
  (testing "AI_FLAGS enum"
    (let [encode (sut/encode sut/AI_FLAGS)
          decode (sut/decode sut/AI_FLAGS)]
      (are [x y] (= x y)
        0 (encode :ignition)
        1 (encode :reserved-2)
        2 (encode :shift-up)
        3 (encode :shift-down)
        :ignition (decode 0)
        :shift-up (decode 2))))
  (testing "AI_FLAGS has 4 entries"
    (is (= 4 (count sut/AI_FLAGS)))))

(deftest headlights-test
  (testing "HEADLIGHTS enum"
    (let [encode (sut/encode sut/HEADLIGHTS)
          decode (sut/decode sut/HEADLIGHTS)]
      (are [x y] (= x y)
        0 (encode :off)
        1 (encode :side)
        2 (encode :low)
        3 (encode :high)
        :off (decode 0)
        :high (decode 3))))
  (testing "HEADLIGHTS has 4 entries"
    (is (= 4 (count sut/HEADLIGHTS)))))

(deftest siren-types-test
  (testing "SIREN_TYPES enum"
    (let [encode (sut/encode sut/SIREN_TYPES)
          decode (sut/decode sut/SIREN_TYPES)]
      (are [x y] (= x y)
        0 (encode :off)
        1 (encode :fast)
        2 (encode :slow)
        :off (decode 0)
        :slow (decode 2))))
  (testing "SIREN_TYPES has 3 entries"
    (is (= 3 (count sut/SIREN_TYPES)))))

(deftest header-type-indices-test
  (testing "HEADER_TYPE has correct indices for AI packets"
    (let [encode (sut/encode sut/HEADER_TYPE)]
      (are [x y] (= x y)
        65 (encode :mal)
        66 (encode :plh)
        67 (encode :ipb)
        68 (encode :aic)
        69 (encode :aii)
        70 (encode :set))))
  (testing "HEADER_TYPE decode works for AI packets"
    (let [decode (sut/decode sut/HEADER_TYPE)]
      (are [x y] (= x y)
        :mal (decode 65)
        :plh (decode 66)
        :ipb (decode 67)
        :aic (decode 68)
        :aii (decode 69)
        :set (decode 70)))))

(deftest small-header-data-indices-test
  (testing "SMALL_HEADER_DATA has correct indices"
    (let [encode (sut/encode sut/SMALL_HEADER_DATA)]
      (are [x y] (= x y)
        9 (encode :lcs)
        10 (encode :lcl)
        11 (encode :aii))))
  (testing "SMALL_HEADER_DATA decode works"
    (let [decode (sut/decode sut/SMALL_HEADER_DATA)]
      (are [x y] (= x y)
        :lcs (decode 9)
        :lcl (decode 10)
        :aii (decode 11)))))
