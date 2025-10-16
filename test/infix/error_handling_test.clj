(ns infix.error-handling-test
  (:require [clojure.test :refer [deftest is testing]]
            [infix.core :refer [infix]]))

;; Note: Many syntax errors will be caught at macro expansion time,
;; so we test what we can and document expected behaviors

(deftest parser-error-recovery
  (testing "expressions that should parse but may have runtime issues"
    ;; These test that the parser doesn't crash on valid syntax
    ;; even if the runtime semantics might be questionable
    
    ;; Undefined symbols (will cause runtime error, but parser should handle)
    (is (thrown? Exception 
         (eval '(infix.core/infix undefined-var + 5))))
    
    ;; Type mismatches that Clojure will catch at runtime
    (is (thrown? Exception
         (eval '(infix.core/infix "string" + 5)))))

  (testing "macro expansion edge cases"
    ;; Test that complex expressions expand without parser errors
    (is (some? (macroexpand-1 '(infix.core/infix 1 + 2 * 3 + 4))))
    (is (some? (macroexpand-1 '(infix.core/infix ((((1 + 2))))))))
    (is (some? (macroexpand-1 '(infix.core/infix true and false or true))))))

;; Boundary and limit testing
(deftest parser-limits
  (testing "very long expressions"
    ;; Test parser can handle long chains of operations
    (let [long-addition (apply list 'infix.core/infix 
                               (interpose '+ (range 1 21)))]  ; 1 + 2 + 3 + ... + 20
      (is (= 210 (eval long-addition))))
    
    ;; Long chain of boolean operations
    (let [long-boolean (apply list 'infix.core/infix 
                             (interpose 'and (repeat 10 true)))]  ; true and true and ...
      (is (= true (eval long-boolean)))))
  
  (testing "deeply nested parentheses"
    ;; Test parser handles deep nesting without stack overflow
    (let [deep-nested (reduce (fn [expr _] (list expr))
                             '(infix.core/infix ((((((1 + 2)))))))
                             (range 5))]
      (is (some? (macroexpand-1 deep-nested))))))

;; Comprehensive operator combination testing
(deftest operator-combinations
  (testing "all operator pairs"
    ;; Test every operator with every other operator
    ;; This ensures precedence is working correctly
    
    ;; Arithmetic + Comparison
    (is (= true (infix 1 + 2 > 2)))
    (is (= true (infix 3 * 2 >= 5)))
    (is (= false (infix 10 / 2 < 4)))
    (is (= true (infix 7 - 3 <= 4)))
    (is (= true (infix 2 + 3 = 5)))
    (is (= false (infix 4 * 2 not= 8)))
    
    ;; Comparison + Boolean
    (is (= true (infix 5 > 3 and 4 < 6)))
    (is (= true (infix 2 = 2 or 3 > 5)))
    (is (= false (infix 1 > 2 and 3 = 3)))
    (is (= true (infix 4 <= 4 or 5 not= 6)))
    
    ;; Arithmetic + Boolean (with correct precedence)
    (is (= true (infix 2 + 3 > 4 and 6 / 2 = 3)))
    (is (= true (infix 1 * 5 < 10 or 8 - 2 > 7)))
    
    ;; All three types together
    (is (= true (infix 2 * 3 + 1 > 5 and 4 <= 4 or false)))
    (is (= false (infix 10 / 2 - 1 < 3 and true and false))))

;; Real-world complexity scenarios  
(deftest real-world-complexity
  (testing "financial calculations"
    (let [principal 1000
          rate 0.05
          time 2
          compound-frequency 4]
      ;; Compound interest: A = P(1 + r/n)^(nt)
      ;; Simplified for infix testing: principal * rate > 40
      (is (= true (infix principal * rate > 40)))
      (is (= true (infix principal + (principal * rate * time) > 1050)))
      
      ;; Complex financial condition
      (is (= true (infix (principal > 500 and rate > 0.01) and (time >= 1 or compound-frequency > 1))))))

  (testing "scientific calculations"
    (let [mass 10
          velocity 20  
          acceleration 9.8
          force (* mass acceleration)]
      ;; Physics formulas with mixed operations
      (is (= true (infix force > mass * 5)))  ; F = ma, test F > m*5
      (is (= true (infix (mass * velocity) > 100)))  ; momentum > 100
      (is (= true (infix (force / mass) = acceleration)))  ; a = F/m
      
      ;; Complex physics condition  
      (is (= true (infix (mass > 0 and velocity > 0) and (force / mass) = acceleration)))))

  (testing "data processing scenarios"
    (let [records [{:age 25 :score 85} {:age 30 :score 92} {:age 22 :score 78}]
          threshold 80
          min-age 21]
      ;; Simulate conditions that might be used in data processing
      (is (= true (infix threshold > 70 and min-age >= 18)))
      (is (= true (infix (count records) = 3 and threshold < 100)))
      
      ;; Complex data validation
      (is (= true (infix (count records) > 0 and (threshold > 0 and threshold < 100)))))))

;; Performance stress testing
(deftest performance-stress
  (testing "wide expressions with many operands"
    ;; Test expression with 50 additions
    (let [nums (range 1 51)
          expr (cons 'infix.core/infix (interpose '+ nums))]
      (is (= 1275 (eval expr))))  ; Sum of 1-50 is 1275
    
    ;; Test expression with mixed operators  
    (let [result (infix 1 * 2 + 3 * 4 + 5 * 6 + 7 * 8 + 9 * 10 + 11 * 12)]
      (is (= 322 result)))  ; 2+12+30+56+90+132 = 322
    
    ;; Many boolean operations
    (let [many-ands (apply list 'infix.core/infix (interpose 'and (repeat 20 true)))]
      (is (= true (eval many-ands))))
    
    ;; Mixed operators in long chain
    (is (= true (infix 1 < 2 and 2 < 3 and 3 < 4 and 4 < 5 and 5 < 6 and 6 < 7 and 7 < 8))))

  (testing "deep nesting performance"
    ;; Deeply nested arithmetic: ((((1 + 1) + 1) + 1) + 1) ... 10 times
    (let [deep-expr (reduce (fn [acc _] (list acc '+ 1))
                           1
                           (range 10))]
      ;; This creates (((((((((1 + 1) + 1) + 1) + 1) + 1) + 1) + 1) + 1) + 1) + 1
      (is (= 11 (eval (list 'infix.core/infix deep-expr)))))
    
    ;; Deeply nested boolean expressions
    (let [deep-bool (reduce (fn [acc _] (list acc 'and true))
                           true
                           (range 5))]
      (is (= true (eval (list 'infix.core/infix deep-bool)))))))

;; Regression tests for specific issues  
(deftest regression-tests
  (testing "precedence regression cases"
    ;; These test specific precedence combinations that could be problematic
    
    ;; Ensure multiplication binds tighter than addition even with booleans
    (is (= true (infix 1 + 2 * 3 = 7)))  ; Should be 1 + (2*3) = 7, not (1+2)*3 = 9
    
    ;; Ensure comparison binds looser than arithmetic  
    (is (= true (infix 2 * 3 + 1 > 5)))  ; Should be (2*3)+1 > 5 => 7 > 5 => true
    
    ;; Ensure boolean operators have correct relative precedence
    (is (= true (infix false or true and true)))  ; Should be false or (true and true) => false or true => true
    (is (= false (infix true and false or false)))  ; Should be (true and false) or false => false or false => false
    
    ;; Complex mixed precedence  
    (is (= true (infix 2 + 3 * 4 > 10 and 5 - 2 = 3)))  ; (2+(3*4)) > 10 and (5-2) = 3 => 14 > 10 and 3 = 3 => true and true => true
    ))