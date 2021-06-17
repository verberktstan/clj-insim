(ns clj-insim.read-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest testing is]]
            [clj-insim.read :as sut]))

(deftest read-header-test
  (testing "read-header"
    (with-open [tiny-none-is (io/input-stream (byte-array [4 3 0 0]))
                tiny-close-is (io/input-stream (byte-array [4 3 0 2]))]
      (testing "returns the header read from a input-stream"
        (is (= #:header{:size 4, :type 3, :request-info 0, :data 0}
               (#'sut/read-header tiny-none-is)))
        (is (= #:header{:size 4, :type 3, :request-info 0, :data 2}
               (#'sut/read-header tiny-close-is)))))))

(def ^:private RAW_VER_BODY
  (byte-array [97 98 99 100 0 0 0 0, 48 49 50 0 0 0, 8, 0]))

(def ^:private RAW_SMALL_ALC_BODY
  (byte-array [0 0 0 1]))

(deftest read-body-test
  (testing "read-body"
    (let [header #:header{:size 20 :type :ver :request-info 0, :data 0}]
      (with-open [version-is (io/input-stream RAW_VER_BODY)]
        (is (= #:body{:version "abcd"
                      :product "012"
                      :insim-version 8
                      :spare 0}
               (#'sut/read-body version-is header)))))))

(deftest body-test
  (testing "body"
    (let [header #:header{:size 8 :type :small :request-info 0 :data :alc}]
      (with-open [alc-is (io/input-stream  RAW_SMALL_ALC_BODY)]
        (is (= (merge header #:body{:cars #{"FBM"}})
               (#'sut/body alc-is header)))))))
