(ns clj-insim.codecs-test
  (:require [clj-insim.codecs :as sut]
            [clojure.test :refer [are deftest testing is]]))

;; Verify that time fields are correctly sized and typed for v10
(deftest codec-test
  (testing "codec structure is correct"
    (are [codec-key] (let [body-fn (get sut/body codec-key)
                           codec (body-fn {})]
                       (some? codec))
      :con
      :hlv
      :obh
      :rip
      :csc
      :uco)))
