(ns examples.leaderboard-test
  (:require [examples.leaderboard :as sut]
            [clojure.test :refer [deftest testing is are]]))

(deftest parse-nodelap-test
  (testing "parse-nodelap"
    (is (nil? (sut/parse-nodelap nil)))
    (is (= {:player-id 1 :position 1}
           (sut/parse-nodelap #:nlp{:player-id 1 :position 1})))))

(deftest diff?-test
  (testing "diff?"
    (is (false? (sut/diff? #{{:player-id 1 :position 1}}
                           [{:player-id 1 :position 1}])))
    (is (true? (sut/diff? #{{:player-id 1 :position 1}
                            {:player-id 2 :position 2}}
                          [{:player-id 1 :position 2}
                           {:player-id 2 :position 1}])))))
