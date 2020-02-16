(ns clj-insim.packets-test
  (:require [clj-insim.packets :as sut]
            [clojure.test :as t]))

(t/deftest ->c-string-test
  (let [text "This is a test"
        result (#'sut/->c-string text 8)
        result2 (#'sut/->c-string text 16)]
    (t/testing "Limits to max-length characters"
      (t/is (= 8 (count result))))
    (t/testing "The last char is null"
      (t/is (= (str "This is" (char 0))
               result)))
    (t/testing "Pads extra null chars un until max-length"
      (t/is (= (apply str "This is a test" (repeat 2 (char 0)))
               result2)))))
