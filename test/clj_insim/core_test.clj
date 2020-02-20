(ns clj-insim.core-test
  (:require [clj-insim.core :as sut]
            [clj-insim.models.packet :as packet]
            [clj-insim.queues :as queues]
            [clojure.test :as t])
  (:import (java.io ByteArrayInputStream ByteArrayOutputStream)))

(def tiny-none
  #::packet{:header {:size 4, :type :tiny, :request-info 0, :data :none}})

(t/deftest read-input-packets!-test
  (let [{:keys [in-queue]} (queues/make)
        bais (ByteArrayInputStream. (byte-array [4 3 0 0]))]
    (t/testing "Reads byte from input stream and puts packet on input queue"
      (#'sut/read-input-packets! bais in-queue)
      (t/is
       (= tiny-none (queues/peek-and-pop! in-queue))))))

(t/deftest dispatch!-test
  (let [{:keys [in-queue out-queue enqueue!] :as queues} (queues/make)
        return-tiny-none (fn [packet]
                           tiny-none)]
    (queues/->queue in-queue tiny-none)
    (t/testing "Puts packet on output queue"
      (#'sut/dispatch! in-queue return-tiny-none out-queue)
      (t/is
       (= tiny-none (queues/peek-and-pop! out-queue))))))

(t/deftest write-output-packets!-test
  (let [{:keys [enqueue! out-queue]} (queues/make)
        baos (ByteArrayOutputStream.)]
    (enqueue! tiny-none)
    (t/testing "Writes bytes to output stream"
        (#'sut/write-output-packets! out-queue baos)
      (t/is
       (=
        [4 3 0 0]
        (seq (.toByteArray baos)))))))
