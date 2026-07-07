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
    (is (false? (sut/new-byte-size?)) "v8 and below use old byte size (version <= 8)"))
  (sut/set-insim-version! 9))

;; Phase 2.9: Version-aware parsing support (codecs and parsers updated)
;; Time-aware parsing is integrated into INFO_BODY_PARSERS for OBH, HLV, CON, CSC, UCO
;; The parse-time-ms helper respects the parse context (insim-version)
;; v9: time values multiplied by 10 (hundredths -> milliseconds)
;; v10: time values used as-is (already milliseconds)

;; PLH (Player Handicaps) parsing tests
(deftest plh-header-rename-test
  (testing "PLH header renaming"
    (testing "renames :header/data to :header/num-players"
      (is (= #:header{:size 12 :type :plh :request-info 0 :num-players 2}
             (sut/header #:header{:size 12 :type 66 :request-info 0 :data 2}))))))

(deftest plh-body-parse-test
  (testing "PLH body parsing (incoming) - flags byte to set"
    (let [packet (merge #:header{:type :plh}
                        #:body{:player-handicaps [{:player-handicap/player-id 1
                                                   :player-handicap/flags 3
                                                   :player-handicap/mass 50
                                                   :player-handicap/restriction 20}]})]
      (let [parsed (sut/body packet)
            parsed-hcaps (get parsed :body/player-handicaps)]
        (is (= #{:set-mass :set-restriction}
               (:player-handicap/flags (first parsed-hcaps)))
            "Flags byte (3) converted to set of keywords"))))
  (testing "PLH body parsing with empty handicaps"
    (let [packet (merge #:header{:type :plh}
                        #:body{:player-handicaps []})]
      (let [parsed (sut/body packet)]
        (is (= [] (get parsed :body/player-handicaps)))))))

(deftest plh-instruction-test
  (testing "PLH instruction (outgoing) - flags set to byte"
    (let [packet (merge #:header{:size 8 :type :plh :request-info 0}
                        #:body{:player-handicaps [{:player-handicap/player-id 1
                                                   :player-handicap/flags #{:set-mass :set-restriction}
                                                   :player-handicap/mass 50
                                                   :player-handicap/restriction 20}]})]
      (let [result ((comp sut/body sut/instruction) 10 packet)
            result-hcaps (get result :body/player-handicaps)]
        (is (= 3
               (:player-handicap/flags (first result-hcaps)))
            "Flag set #{:set-mass :set-restriction} converted to byte value 3")))))
