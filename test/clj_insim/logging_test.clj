(ns clj-insim.logging-test
  "Unit tests for every non-public function in clj-insim.logging."
  (:require [clj-insim.logging :as sut]
            [clojure.string :as str]
            [clojure.test :refer [deftest testing is use-fixtures]]))

(defn- reset-state! []
  (reset! sut/ERROR_LOG nil)
  (reset! sut/ERRORS true)
  (reset! sut/VERBOSE false))

(use-fixtures :each (fn [f] (reset-state!) (f) (reset-state!)))

(deftest print-verbose-test
  (testing "print-verbose"
    (reset! sut/VERBOSE false)
    (is (= "" (with-out-str (sut/print-verbose #:header{:type :tiny :data :none})))
        "prints nothing when VERBOSE is false")
    (testing "when VERBOSE is true"
      (reset! sut/VERBOSE true)
      (let [packet #:header{:type :tiny :data :none}
            output (with-out-str (sut/print-verbose packet))]
        (is (str/includes? output "IS_TINY packet!") "prints the packet type")
        (is (str/includes? output (str packet)) "prints the packet value")))))

(deftest log-throwable-test
  (testing "log-throwable"
    (testing "when ERRORS is true, appends to ERROR_LOG and prints the message"
      (reset! sut/ERRORS true)
      (let [output (with-out-str (sut/log-throwable (ex-info "boom" {})))]
        (is (str/includes? output "boom"))
        (is (= 1 (count @sut/ERROR_LOG)))
        (is (= "boom" (:cause (first @sut/ERROR_LOG))))))
    (testing "when ERRORS is false, does nothing"
      (reset! sut/ERRORS false)
      (reset! sut/ERROR_LOG nil)
      (let [output (with-out-str (sut/log-throwable (ex-info "boom" {})))]
        (is (= "" output))
        (is (nil? @sut/ERROR_LOG))))))

(deftest wrap-try-catch-test
  (testing "wrap-try-catch"
    (testing "returns the result of f when it doesn't throw"
      (is (= 3 (sut/wrap-try-catch + 1 2))))
    (testing "catches throwables, delegates to log-throwable, and returns nil"
      (reset! sut/ERRORS true)
      (let [output (with-out-str
                     (is (nil? (sut/wrap-try-catch (fn [] (throw (ex-info "kaboom" {})))))))]
        (is (str/includes? output "kaboom"))
        (is (= 1 (count @sut/ERROR_LOG)))))))
