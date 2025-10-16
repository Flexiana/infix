(ns infix.precedence-test
  (:require [clojure.test :refer [deftest is testing]]
            [infix.precedence :as prec]))

;; Step 2.1: Precedence Table Tests (TDD - RED phase)
(deftest operator-precedence
  (testing "basic operator precedence rules"
    (is (< (prec/precedence '+) (prec/precedence '*)))
    (is (= (prec/precedence '+) (prec/precedence '-)))
    (is (= (prec/precedence '*) (prec/precedence '/)))))

(deftest operator-associativity
  (testing "operator associativity"
    (is (prec/left-associative? '+))
    (is (prec/left-associative? '*))))