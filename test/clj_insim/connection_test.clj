(ns clj-insim.connection-test
  "Unit tests for clj-insim.connection (socket & stream lifecycle)."
  (:require [clj-insim.connection :as sut]
            [clojure.string :as str]
            [clojure.test :refer [deftest testing is]])
  (:import (java.io Closeable)
           (java.net ServerSocket Socket)))

(deftest make-socket-test
  (testing "make-socket"
    (with-open [server (ServerSocket. 0)]
      (let [socket (sut/make-socket "127.0.0.1" (.getLocalPort server))]
        (try
          (is (instance? Socket socket) "returns a Socket instance")
          (is (.isConnected socket) "returns a connected socket")
          (finally (.close socket)))))
    (testing "returns nil and prints a message when the connection is refused"
      (let [port (with-open [server (ServerSocket. 0)] (.getLocalPort server))
            output (with-out-str
                     (is (nil? (sut/make-socket "127.0.0.1" port))))]
        (is (str/includes? output (str port)))))))

(deftest close-socket!-test
  (testing "close-socket!"
    (let [closed    (atom #{})
          closeable (fn [k] (proxy [Closeable] []
                              (close [] (swap! closed conj k))))]
      (sut/close-socket! (closeable :socket) (closeable :input-stream) (closeable :output-stream))
      (is (= #{:socket :input-stream :output-stream} @closed)
          "closes socket, input-stream and output-stream"))))
