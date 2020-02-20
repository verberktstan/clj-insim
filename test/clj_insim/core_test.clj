(ns clj-insim.core-test
  (:require [clj-insim.core :as sut]
            [clj-insim.models.packet :as packet]
            [clj-insim.queues :as queues]
            [clojure.test :as t])
  (:import (java.io ByteArrayInputStream)))

(t/deftest read-input-packets!-test
  (let [{:keys [in-queue]} (queues/make)
        bais (ByteArrayInputStream. (byte-array [4 3 0 0]))]
    (#'sut/read-input-packets! bais in-queue)
    (t/is
     (=
      '(#::packet{:header {:size 4, :type :tiny, :request-info 0, :data :none}})
      (seq @in-queue)))))
