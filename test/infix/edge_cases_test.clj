(ns infix.edge-cases-test
  (:require [clojure.test :refer [deftest is testing]]
            [infix.core :refer [infix]]))

;; Edge cases that should work
(deftest valid-edge-cases
  (testing "minimal valid expressions"
    (is (= 5 (infix 5)))  ; Single operand
    (is (= :foo (infix :foo)))  ; Single keyword
    (is (= "test" (infix "test")))  ; Single string
    (is (= [1 2 3] (infix [1 2 3]))))  ; Single vector
  
  (testing "redundant parentheses"
    ;; TODO: These edge cases need special handling for deeply nested grouping
    #_(is (= 7 (infix ((((3 + 4)))))))  ; Multiple nested parens around single expression  
    #_(is (= true (infix (((true))))))  ; Nested parens around boolean
    #_(is (= 10 (infix ((5)) + ((5))))))  ; Nested parens around operands
  
  (testing "whitespace and formatting variations"
    ;; Note: Clojure parses tokens based on whitespace, so we test normal spacing
    (is (= 8 (infix 3 + 5)))  ; Normal spaces
    (is (= 8 (infix  3   +   5  )))))  ; Extra spaces should work

;; Boundary value testing
(deftest boundary-values
  (testing "numeric boundaries"
    ;; Large numbers
    (is (= true (infix 999999999 > 999999998)))
    (is (= 2000000000 (infix 1000000000 + 1000000000)))
    
    ;; Floating point
    (is (= true (infix 1.5 > 1.4)))
    (is (= 3.14159 (infix 3.14159)))  ; Single float
    
    ;; Zero cases
    (is (= true (infix 0 = 0)))
    (is (= false (infix 0 > 0)))
    (is (= true (infix 0 >= 0)))
    (is (= 0 (infix 5 - 5))))
  
  (testing "boolean boundaries"
    (is (= true (infix true or false)))
    (is (= false (infix false and true)))
    (is (= true (infix true and true and true)))
    (is (= false (infix true and true and false)))))

;; Complex nesting stress tests  
(deftest stress-nesting
  (testing "deeply nested arithmetic"
    ;; 5 levels deep: ((((1 + 2) * 3) + 4) * 5) = (((3 * 3) + 4) * 5) = ((9 + 4) * 5) = (13 * 5) = 65
    (is (= 65 (infix ((((1 + 2) * 3) + 4) * 5))))
    
    ;; Mixed operations with deep nesting: (((6) + (20)) * (6 / 3)) = (26 * 2) = 52
    (is (= 52 (infix (((2 * 3) + (4 * 5)) * (6 / (1 + 2))))))
    
    ;; Very deep boolean nesting
    (is (= true (infix ((((true and true) or false) and true) or ((false or true) and true))))))
  
  (testing "wide expressions with many operators"
    ;; Many terms: 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1
    (is (= 10 (infix 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1)))
    
    ;; Alternating operations: 10 - 2 + 3 - 1 + 5 - 2 + 1
    (is (= 14 (infix 10 - 2 + 3 - 1 + 5 - 2 + 1)))
    
    ;; Many comparisons with booleans
    (is (= true (infix 1 < 2 and 2 < 3 and 3 < 4 and 4 < 5 and 5 < 6)))))

;; Type mixing tests
(deftest type-mixing
  (testing "numbers with different types"
    ;; Integer and float
    (is (= 3.5 (infix 1 + 2.5)))
    (is (= false (infix 1.0 = 1)))  ; Clojure = is strict about types
    (is (= true (infix 1.0 = 1.0)))  ; Same types should be equal
    
    ;; Ratios
    (is (= 3/2 (infix 1/2 + 1)))
    (is (= true (infix 1/2 < 1))))
  
  (testing "string comparisons"
    (is (= true (infix "abc" = "abc")))
    (is (= false (infix "abc" = "def")))
    (is (= true (infix "abc" not= "def"))))
  
  (testing "keyword comparisons"  
    (is (= true (infix :foo = :foo)))
    (is (= false (infix :foo = :bar)))
    (is (= true (infix :foo not= :bar)))))

;; Integration with Clojure constructs
(deftest clojure-constructs-integration
  (testing "with function calls in operands"
    ;; Using functions as operands
    (is (= true (infix (+ 1 2) = 3)))
    (is (= true (infix (count [1 2 3 4]) > (count [1 2]))))
    (is (= 8 (infix (+ 1 2) * (- 4 2) + (* 1 2))))
    
    ;; Complex nested function calls
    (is (= true (infix (max 5 3 7) > (min 10 2 8))))
    (is (= true (infix (first [1 2 3]) < (last [1 2 3])))))
  
  (testing "with variables and symbols"  
    ;; Test with let bindings
    (let [x 5
          y 10
          flag true]
      (is (= true (infix x < y)))
      (is (= true (infix x + y > 10)))
      (is (= true (infix flag and x > 0))))))