(ns clj-insim.utils-test
  (:require [clojure.test :refer [deftest testing is are]]
            [clj-insim.utils :as sut]))

(deftest c-str-test
  (testing "c-str adds null chars up to length, always overrides last char"
    (is (= ["a    " "abc  " "abcd "]
           (map #(sut/c-str % 5) ["a" "abc" "abcde"])))))

(deftest clip-str-test
  (is (= "abcde   " (sut/clip-str "abcde" 8 4)))
  (is (= "abcde " (sut/clip-str "abcde" 12 3)))
  (is (= "abc " (sut/clip-str "abcde" 4 4))))

(deftest map-kv-test
  (testing "map-kv"
    (testing "returns coll when emtpy fns map is supplied"
      (is (= [:a :b] (sut/map-kv {} [:a :b])))
      (is (= {:a 1 :b 10} (sut/map-kv {} {:a 1 :b 10}))))
    (testing "returns coll with functions applied"
      (is (= [2 9] (sut/map-kv {0 inc 1 dec} [1 10])))
      (is (= {:a 2 :b 9} (sut/map-kv {:a inc :b dec} {:a 1 :b 10}))))))

(deftest index-of-test
  (testing "index-of"
    (let [f (sut/index-of [:a :b])]
      (testing "returns a function"
        (is (ifn? f))
        (testing "..that returns the index of an item (or nil)"
          (is (= [0 1 nil]
                 (map f [:a :b :c]))))))))
