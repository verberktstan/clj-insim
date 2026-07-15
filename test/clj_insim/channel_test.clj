(ns clj-insim.channel-test
  "Unit tests for clj-insim.channel (packet channel plumbing)."
  (:require [clj-insim.channel :as sut]
            [clj-insim.packets :as packets]
            [clojure.core.async :as a]
            [clojure.test :refer [deftest testing is]]))

(deftest >!!-test
  (testing ">!!"
    (let [client {:to-lfs (a/chan 1)}]
      (sut/>!! client (packets/tiny))
      (is (= (packets/tiny) (a/<!! (:to-lfs client)))))))

(deftest <!!-test
  (testing "<!!"
    (let [client {:from-lfs (a/chan 1)}]
      (a/>!! (:from-lfs client) (packets/tiny))
      (is (= (packets/tiny) (sut/<!! client nil))))))

(deftest >!-test
  (testing ">!"
    (let [client {:to-lfs (a/chan 1)}]
      (sut/>! client (packets/tiny))
      (is (= (packets/tiny) (a/<!! (:to-lfs client)))))))

(deftest <!-test
  (testing "<!"
    (let [client {:from-lfs (a/chan 1)}]
      (a/>!! (:from-lfs client) (packets/tiny))
      (is (= (packets/tiny) (a/<!! (sut/<! client nil)))))))

(deftest close!-test
  (testing "close!"
    (let [client {:from-lfs (a/chan 1) :to-lfs (a/chan 1)}]
      (sut/close! client)
      (is (nil? (a/<!! (:from-lfs client))))
      (is (nil? (a/<!! (:to-lfs client)))))))
