(ns clj-insim.write-test
  (:require [clj-insim.codecs :as codecs]
            [clj-insim.models.packet :as packet]
            [clj-insim.packets :as packets]
            [clj-insim.parse :as parse]
            [clj-insim.write :as sut]
            [clojure.test :refer [deftest testing is]])
  (:import (java.io ByteArrayOutputStream)))

(deftest prepare-test
  (testing "prepare"
    (let [packet (packets/msl {:text "Local message" :sound :error})
          {:keys [body-codec instruction]} (#'sut/prepare false packet)]
      (testing "returns a raw packet associatedd with :instruction"
        (is (packet/raw? instruction)))
      (testing "and the IS_MSL codec associated with :body-codec, or nil"
        ;; This is a bit of a quirk, I compare string versions of the codec
        ;; because structs arn't easy comparable in another way.
        (is (= (str ((:msl codecs/body) packet))
               (str body-codec)))
        (is (nil? (:body-codec (packets/tiny))))))))

(deftest write-instruction!-test
  ;; Todo add test for writing bigger packets than TINY
  (testing "write-instruction!"
    (testing "with old byte size"
      (with-open [baos (ByteArrayOutputStream.)]
        (let [parse-instruction (partial parse/instruction 1)]
          (#'sut/write-instruction! baos nil (parse-instruction (packets/tiny)))
          (testing "writes header data to output stream when no body-codec is supplied"
            (is (= [4 3 0 0]
                   (vec (.toByteArray baos))))))))
    (testing "with new byte size"
      (with-open [baos (ByteArrayOutputStream.)]
        (let [parse-instruction (partial parse/instruction 4)]
          (#'sut/write-instruction! baos nil (parse-instruction (packets/tiny)))
          (testing "writes header data to output stream when no body-codec is supplied"
            (is (= [1 3 0 0]
                   (vec (.toByteArray baos))))))))))
