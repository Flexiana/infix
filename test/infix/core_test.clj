(ns infix.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [infix.core :refer [infix]]))

;; Integration tests - will be implemented after basic components work
(deftest basic-infix-macro
  (testing "basic infix macro functionality"
    (is (= 7 (infix 3 + 4)))
    ;; Test complex precedence: 2 * 3 + 4 * 2 = 6 + 8 = 14
    (is (= 14 (infix 2 * 3 + 4 * 2)))
    ;; Test macro expansion with simple case first
    (is (= '(+ 3 4) (macroexpand-1 '(infix.core/infix 3 + 4))))))