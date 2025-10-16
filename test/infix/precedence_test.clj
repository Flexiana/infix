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

;; Step 3.1: Comparison Operators Tests (TDD - RED phase)
(deftest comparison-operator-precedence
  (testing "comparison operators have lower precedence than arithmetic"
    (is (< (prec/precedence '<) (prec/precedence '+)))
    (is (< (prec/precedence '=) (prec/precedence '*)))
    (is (= (prec/precedence '<) (prec/precedence '<=)))
    (is (= (prec/precedence '>) (prec/precedence '>=)))
    (is (= (prec/precedence '=) (prec/precedence 'not=)))))

;; Step 3.2: Boolean Logic Tests (TDD - RED phase)
(deftest boolean-operator-precedence
  (testing "boolean operators have lower precedence than comparisons"
    (is (< (prec/precedence 'and) (prec/precedence '<)))
    (is (< (prec/precedence 'or) (prec/precedence '=)))
    (is (< (prec/precedence 'or) (prec/precedence 'and)))  ; or has lower precedence than and
    (is (> (prec/precedence 'not) (prec/precedence 'and)))))  ; not has higher precedence