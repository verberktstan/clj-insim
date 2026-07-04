(ns clj-insim.parse-test
  (:require [clojure.test :refer [deftest testing is]]
            [clj-insim.parse :as sut]))

(deftest header-test
  (testing "header"
    (testing "returns header with parsed `:header/type` and `:header/data`"
      (is (= #:header{:size 4 :type :tiny :request-info 0 :data :none}
             (sut/header
              #:header{:size 4 :type 3 :request-info 0 :data 0})))
      (is (= #:header{:size 4 :type :tiny :request-info 0 :data :close}
             (sut/header
              #:header{:size 4 :type 3 :request-info 0 :data 2}))))))

(deftest body-test
  (testing "body"
    (let [packet (merge #:header{:type :small :data :vta}
                        #:body{:unique-connection-id 1 :action 1})]
      (is (= (merge #:header{:type :small :data :vta}
                    #:body{:unique-connection-id 1 :action :end})
             (sut/body packet))))))

(deftest pipeline-test
  (testing "pipe instruction & header/body"
    (testing "with old byte size"
      (let [packet (merge #:header{:size 8 :type :small :request-info 1 :data :ssp}
                          #:body{:unsigned-value 500})]
        (is (= packet
               ((comp sut/body sut/header) (sut/instruction 1 packet))))))
    (testing "with new byte size"
      (let [packet (merge #:header{:size 8 :type :small :request-info 1 :data :ssp}
                          #:body{:unsigned-value 500})]
        (is (= (assoc packet :header/size 2)
               ((comp sut/body sut/header) (sut/instruction 4 packet))))))))

;; Phase 2: Parse Context - InSimVer tracking for backward compatibility
;; Version tracking allows time-format aware parsing for v9 (hundredths) vs v10 (ms)
(deftest insim-version-context-test
  (testing "insim-version context tracking"
    (sut/set-insim-version! 9)
    (is (= 9 (sut/insim-version)) "Can track v9")
    (sut/set-insim-version! 10)
    (is (= 10 (sut/insim-version)) "Can track v10")))

(deftest new-byte-size-test
  (testing "new-byte-size? derives from insim-version"
    ;; Both v9 and v10 use new byte size (> 8)
    (sut/set-insim-version! 9)
    (is (sut/new-byte-size?) "v9 uses new byte size (version > 8)")
    (sut/set-insim-version! 10)
    (is (sut/new-byte-size?) "v10 uses new byte size (version > 8)")
    (sut/set-insim-version! 8)
    (is (false? (sut/new-byte-size?)) "v8 and below use old byte size (version <= 8)")))
