(ns infix.infix-let-test
  (:require [clojure.test :refer [deftest is testing]]
            [infix.core :refer [infix infix-let]]))

;; Phase 6: infix-let Macro Tests
;; Sequential bindings with infix expressions on the RHS

(deftest basic-infix-let
  (testing "basic let bindings with infix RHS"
    ;; Simple arithmetic binding
    (is (= 7 (infix-let [a (1 + 2)
                         b (a + 4)]
               b)))
    
    ;; Multiple operations
    (is (= 14 (infix-let [x (2 * 3)
                          y (x + 2)]
                (y * 2))))
    
    ;; Using bindings in body
    (is (= 10 (infix-let [a (3 + 2)]
                (a * 2))))))

(deftest infix-let-with-comparisons
  (testing "infix-let with comparison and boolean operators"
    ;; Boolean result
    (is (= true (infix-let [a (5 + 3)
                            b (a > 7)]
                  b)))
    
    ;; Complex boolean expression
    (is (= false (infix-let [x (10 / 2)
                             y (x < 4)]
                   (y and true))))
    
    ;; Mixed arithmetic and comparison
    (is (= true (infix-let [result ((2 * 3) + (4 / 2))]
                  (result = 8))))))

(deftest infix-let-with-threading
  (testing "infix-let with threading operators"
    ;; Thread-first in binding
    (is (= "HELLO" (infix-let [result ("hello" -> clojure.string/upper-case)]
                     result)))
    
    ;; Thread-last in binding
    (is (= [2 4 6] (infix-let [doubled ([1 2 3] ->> (map #(* 2 %)))]
                     (vec doubled))))
    
    ;; Mixed threading and arithmetic
    (is (= 8 (infix-let [count-val ([1 2 3] ->> count)
                         result (count-val + 5)]
               result)))))

(deftest infix-let-with-function-calls
  (testing "infix-let with function calls and nested expressions"
    ;; Function calls in bindings
    (is (= 8 (infix-let [a (max 3 5)
                         b (min 8 10)
                         result (a + (b / 4))]
               result)))
    
    ;; Nested infix in function calls
    (is (= 9 (infix-let [x ((max (2 + 3) 4) + (min (1 + 1) 3))]
               x)))
    
    ;; Complex nesting
    (is (= 15 (infix-let [data {:nums [1 2 3 4 5]}
                          sum (data -> (get :nums) ->> (reduce +))]
                sum)))))

(deftest infix-let-complex-expressions
  (testing "complex expressions in bindings and body"
    ;; Multi-step calculation
    (is (= 42 (infix-let [base (5 * 8)
                          bonus (base / 4)
                          total (base + bonus - 3)]
                total)))
    
    ;; Business logic example
    (is (= 90.0 (infix-let [price 100
                            discount (price * 0.1)
                            final-price (price - discount)]
                   final-price)))
    
    ;; Vector operations
    (is (= [1 4 9] (infix-let [nums [1 2 3]
                               squared (nums ->> (map #(* % %)))]
                     (vec squared))))))

(deftest infix-let-edge-cases
  (testing "edge cases and error scenarios"
    ;; Empty bindings
    (is (= 5 (infix-let [] (2 + 3))))
    
    ;; Single binding
    (is (= 7 (infix-let [x (3 + 4)] x)))
    
    ;; Shadowing
    (let [x 10]
      (is (= 5 (infix-let [x (2 + 3)] x))))
    
    ;; Multiple let nesting
    (is (= 21 (infix-let [a (1 + 2)]
                (infix-let [b (a * 3)]
                  (infix-let [c (b * 2)]
                    (c + 3)))))))

(deftest infix-let-macro-expansion
  (testing "macro expansion produces correct let forms"
    ;; Test macro expansion
    (is (= '(let [a (+ 1 2) b (+ a 4)] b)
           (macroexpand-1 '(infix-let [a (1 + 2) b (a + 4)] b))))
    
    ;; Complex expansion
    (is (= '(let [x (* 2 3) y (+ x 2)] (* y 2))
           (macroexpand-1 '(infix-let [x (2 * 3) y (x + 2)] (y * 2)))))))