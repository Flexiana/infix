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

;; Step 3.1: Comparison Operators Tests (TDD - RED phase)
(deftest comparison-operators
  (testing "comparison operators"
    (is (= true (infix 5 > 3)))
    (is (= false (infix 2 >= 5)))
    (is (= true (infix 10 = 10)))
    (is (= false (infix 5 not= 5)))
    (is (= '(< x y) (macroexpand-1 '(infix.core/infix x < y))))))

;; Step 3.2: Boolean Logic Tests (TDD - RED phase)
(deftest boolean-logic
  (testing "boolean operators"
    (is (= true (infix true and true)))
    (is (= true (infix false or true)))
    ;; Skip not operator for now - it's unary and needs special handling
    #_(is (= false (infix not true)))
    ;; Test precedence: x < y and y <= z should be (and (< x y) (<= y z))
    (is (= '(and (< x y) (<= y z)) (macroexpand-1 '(infix.core/infix x < y and y <= z))))))

;; Comprehensive integration tests
(deftest comprehensive-integration
  (testing "all operator types in complex expressions"
    ;; Test case: (2 + 3 * 4) > 10 and (8 / 2 = 4) or false
    ;; Expected: (2 + 12) > 10 and (4 = 4) or false => 14 > 10 and true or false => true and true or false => true
    (is (= true (infix (2 + 3 * 4) > 10 and (8 / 2 = 4) or false)))
    
    ;; Complex nested: ((1 + 2) * 3 - 4) >= 5 and (10 / (2 + 3)) <= 2 
    ;; Expected: ((3) * 3 - 4) >= 5 and (10 / 5) <= 2 => (9 - 4) >= 5 and 2 <= 2 => 5 >= 5 and true => true and true => true
    (is (= true (infix ((1 + 2) * 3 - 4) >= 5 and (10 / (2 + 3)) <= 2)))
    
    ;; All operators: 2 * 3 + 1 > 5 and 4 <= 4 or 3 not= 2
    ;; Expected: (2*3) + 1 > 5 and 4 <= 4 or 3 not= 2 => 7 > 5 and true or true => true and true or true => true
    (is (= true (infix 2 * 3 + 1 > 5 and 4 <= 4 or 3 not= 2))))

  (testing "macro expansion correctness for complex cases"
    ;; Verify complex expressions expand to correct Clojure forms
    (is (= '(or (and (> (+ (* 2 3) 1) 5) (<= 4 4)) (not= 3 2))
           (macroexpand-1 '(infix.core/infix 2 * 3 + 1 > 5 and 4 <= 4 or 3 not= 2))))
    
    ;; Nested parentheses expansion
    (is (= '(>= (- (* (+ 1 2) 3) 4) 5)
           (macroexpand-1 '(infix.core/infix ((1 + 2) * 3 - 4) >= 5))))
    
    ;; Mixed boolean and arithmetic
    (is (= '(and (< a (+ b c)) (or (= d e) (> f g)))
           (macroexpand-1 '(infix.core/infix a < b + c and (d = e or f > g))))))

  (testing "real-world mathematical expressions"
    ;; Quadratic discriminant: b² - 4ac
    (let [a 1 b -5 c 6]  ; x² - 5x + 6 = 0, solutions: x=2,3
      (is (= 1 (infix b * b - 4 * a * c)))  ; (-5)² - 4(1)(6) = 25 - 24 = 1
      (is (= true (infix b * b - 4 * a * c > 0))))  ; Discriminant > 0 means two real roots
    
    ;; Distance formula: √((x₂-x₁)² + (y₂-y₁)²) - we'll test the squared distance
    (let [x1 1 y1 1 x2 4 y2 5]
      (is (= 25 (infix (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))))  ; 3² + 4² = 9 + 16 = 25
      (is (= true (infix (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) = 25))))
    
    ;; Compound interest approximation: A ≈ P(1 + r)^t (simplified)
    (let [principal 1000 rate 0.05 time 2]
      (is (= true (infix principal * (1 + rate) > principal)))  ; Basic growth check
      (is (= true (infix principal > 500 and rate > 0 and time > 0))))))  ; Precondition check