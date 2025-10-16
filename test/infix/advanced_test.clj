(ns infix.advanced-test
  (:require [clojure.test :refer [deftest is testing]]
            [infix.core :refer [infix]]))

;; Comprehensive nested expression tests
(deftest deeply-nested-expressions
  (testing "deep nesting with mixed operators"
    ;; Test: ((1 + 2) * (3 + 4)) > (5 * 2) and (10 / 2) = 5
    ;; Should be: (3 * 7) > 10 and 5 = 5 => 21 > 10 and true => true and true => true
    (is (= true (infix ((1 + 2) * (3 + 4)) > (5 * 2) and (10 / 2) = 5)))
    
    ;; Test multiple levels: (((1 + 2) * 3) + ((4 * 5) / 2)) > 15
    ;; Should be: ((3 * 3) + (20 / 2)) > 15 => (9 + 10) > 15 => 19 > 15 => true
    (is (= true (infix (((1 + 2) * 3) + ((4 * 5) / 2)) > 15)))
    
    ;; Test complex boolean nesting: (true and (false or true)) and ((3 > 2) or (1 = 2))
    ;; Should be: (true and true) and (true or false) => true and true => true
    (is (= true (infix (true and (false or true)) and ((3 > 2) or (1 = 2))))))

  (testing "precedence with deep nesting"
    ;; Without parens: 1 + 2 * 3 + 4 > 5 and 6 < 7 or false
    ;; Should be: 1 + 6 + 4 > 5 and 6 < 7 or false => 11 > 5 and true or false => true and true or false => true or false => true
    (is (= true (infix 1 + 2 * 3 + 4 > 5 and 6 < 7 or false)))
    
    ;; With strategic parens: (1 + 2) * (3 + 4) > (5 and 6) < 7 or false
    ;; Note: this should fail because (5 and 6) is not valid - let's fix this
    ;; Better test: (1 + 2) * (3 + 4) > 5 and (6 < 7 or false)
    (is (= true (infix (1 + 2) * (3 + 4) > 5 and (6 < 7 or false))))))

;; Edge cases and boundary conditions  
(deftest edge-cases
  (testing "single operands and simple expressions"
    (is (= 42 (infix 42)))  ; Single number
    (is (= true (infix true)))  ; Single boolean
    (is (= :keyword (infix :keyword)))  ; Single keyword
    
    ;; Simple binary operations
    (is (= 5 (infix 2 + 3)))
    (is (= false (infix true and false)))
    (is (= true (infix 5 > 3))))

  (testing "operator precedence edge cases"
    ;; All arithmetic operators: 1 + 2 - 3 * 4 / 2
    ;; Should be: 1 + 2 - (3 * 4) / 2 => 1 + 2 - 12/2 => 1 + 2 - 6 => -3
    (is (= -3 (infix 1 + 2 - 3 * 4 / 2)))
    
    ;; Mixed comparison: 1 < 2 <= 2 >= 1 > 0  
    ;; Should be: ((((1 < 2) <= 2) >= 1) > 0) - but this doesn't make sense
    ;; Better: multiple separate comparisons combined with boolean
    (is (= true (infix 1 < 2 and 2 <= 2 and 2 >= 1 and 1 > 0)))
    
    ;; All boolean precedence: false or true and false or true
    ;; Should be: false or (true and false) or true => false or false or true => true
    (is (= true (infix false or true and false or true)))))

;; Advanced Clojure integration tests
(deftest clojure-integration
  (testing "keywords and strings"
    (is (= true (infix :a = :a)))
    (is (= false (infix :a = :b)))
    (is (= true (infix "hello" = "hello")))
    (is (= false (infix "hello" = "world"))))
  
  (testing "nil values"
    (is (= true (infix nil = nil)))
    (is (= false (infix nil = 5)))
    (is (= false (infix nil not= nil)))
    (is (= true (infix nil not= 5))))
  
  (testing "with Clojure collections"
    ;; Vector operations
    (is (= true (infix [1 2 3] = [1 2 3])))
    (is (= false (infix [1 2] = [1 2 3])))
    
    ;; Map operations  
    (is (= true (infix {:a 1} = {:a 1})))
    (is (= false (infix {:a 1} = {:a 2})))
    
    ;; Set operations
    (is (= true (infix #{1 2 3} = #{3 2 1})))  ; Sets are equal regardless of order
    (is (= false (infix #{1 2} = #{1 2 3}))))
  
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

;; Complex real-world scenarios
(deftest real-world-scenarios
  (testing "business logic expressions"
    ;; Age and income validation
    (let [age 25
          income 50000
          has-job true
          credit-score 720]
      (is (= true (infix age >= 18 and age <= 65 and income > 30000)))
      (is (= true (infix has-job and credit-score >= 700)))
      (is (= true (infix (age >= 21 and income > 40000) or (has-job and credit-score >= 650)))))
    
    ;; Mathematical formulas with precedence
    (let [a 2
          b 3  
          c 4
          discriminant (- (* b b) (* 4 a c))]
      ;; Test discriminant calculation: b² - 4ac
      (is (= -23 discriminant))
      (is (= true (infix discriminant < 0)))
      ;; Quadratic formula condition
      (is (= true (infix discriminant < 0 and a not= 0)))))

  (testing "data validation scenarios"
    ;; Email and password validation simulation
    (let [email-length 15
          has-at-symbol true  
          password-length 12
          has-special-char true
          has-upper-case true]
      (is (= true (infix email-length > 5 and has-at-symbol)))
      (is (= true (infix password-length >= 8 and has-special-char and has-upper-case)))
      (is (= true (infix (email-length > 5 and has-at-symbol) and (password-length >= 8 and has-special-char)))))))