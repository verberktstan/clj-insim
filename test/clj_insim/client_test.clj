(ns clj-insim.client-test
  "Unit tests for every non-public function in clj-insim.client. What's left
   here after extracting clj-insim.logging, clj-insim.connection and
   clj-insim.channel is the orchestration that ties them together:
   1. `close-fn`  - stop orchestration (channel + connection + logging)
   2. `dispatch`  - protocol keepalive"
  (:require [clj-insim.client :as sut]
            [clj-insim.logging :as logging]
            [clj-insim.packets :as packets]
            [clojure.core.async :as a]
            [clojure.string :as str]
            [clojure.test :refer [deftest testing is use-fixtures]])
  (:import (java.io Closeable)))

(defn- reset-state! []
  (reset! logging/ERROR_LOG nil)
  (reset! logging/ERRORS true)
  (reset! logging/VERBOSE false))

(use-fixtures :each (fn [f] (reset-state!) (f) (reset-state!)))

(deftest close-fn-test
  (testing "close-fn"
    (testing "when the client is running"
      (let [closed    (atom #{})
            closeable (fn [k] (proxy [Closeable] []
                                (close [] (swap! closed conj k))))
            from-lfs  (a/chan 1)
            to-lfs    (a/chan 1)
            running?  (atom true)]
        (#'sut/close-fn {:running?      running?
                         :from-lfs      from-lfs
                         :to-lfs        to-lfs
                         :input-stream  (closeable :input-stream)
                         :output-stream (closeable :output-stream)
                         :socket        (closeable :socket)})
        (is (= (packets/tiny {:data :close}) (a/<!! to-lfs))
            "sends a TINY/CLOSE packet first")
        (is (nil? (a/<!! to-lfs)) "to-lfs channels should be closed")
        (is (nil? (a/<!! from-lfs)) "closes the from-lfs channel")
        (is (false? @running?) "sets running? to false")
        (is (= #{:input-stream :output-stream :socket} @closed)
            "closes streams and socket")))
    (is (nil? (#'sut/close-fn {:running? (atom false)}))
        "does nothring when client is not running")))

(deftest dispatch-test
  (testing "dispatch"
    (let [to-lfs (a/chan 1)]
      (#'sut/dispatch {:to-lfs to-lfs} #:header{:type :tiny :data :none})
      (is (= (packets/tiny) (a/<!! to-lfs))
          "replies with TINY/NONE packet when given maintain-connection? packet"))
    (let [to-lfs (a/chan 1)]
      (#'sut/dispatch {:to-lfs to-lfs} #:header{:type :small :data :vta})
      (is (nil? (a/poll! to-lfs)) "does not reply for other packets"))
    (let [_      (reset! logging/VERBOSE true)
          to-lfs (a/chan 1)
          output (with-out-str
                   (#'sut/dispatch {:to-lfs to-lfs} #:header{:type :small :data :vta}))]
      (is (str/includes? output "IS_SMALL packet!")
          "delegates to print-verbose regardless of packet type"))))
