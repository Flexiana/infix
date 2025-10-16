(ns infix.arrow-lambda-test
  (:require [clojure.test :refer [deftest is testing]]
            [infix.core :refer [infix]]))

;; Phase 7: Arrow Lambda Tests
;; Support x => expr syntax for anonymous functions

(deftest simple-arrow-lambdas
  (testing "basic arrow lambda syntax"
    ;; Single parameter arithmetic
    (is (= [1 4 9] (map (infix x => x * x) [1 2 3])))
    (is (= [2 4 6] (map (infix x => x * 2) [1 2 3])))
    
    ;; Single parameter with function calls
    (is (= [1 2 3] (map (infix x => (abs x)) [-1 -2 -3])))
    
    ;; Single parameter comparisons
    (is (= [2 3 4] (filter (infix x => x > 1) [1 2 3 4])))
    (is (= [false true true] (map (infix x => x >= 2) [1 2 3])))))

(deftest arrow-lambdas-with-complex-expressions
  (testing "arrow lambdas with complex expressions"
    ;; Multiple operations
    (is (= [2 6 12] (map (infix x => x * x + x) [1 2 3])))  ; x² + x: 1+1=2, 4+2=6, 9+3=12
    
    ;; Boolean logic
    (is (= [true false false] (map (infix x => x > 0 and x < 3) [1 2 3])))
    
    ;; Threading operations
    (is (= ["1" "2" "3"] (map (infix x => x -> str) [1 2 3])))
    
    ;; Mixed operations  
    (is (= [2.5 5.0 7.5] (map (infix x => (x * 2) + (x / 2)) [1 2 3])))))

(deftest multi-parameter-arrow-lambdas
  (testing "multi-parameter arrow lambdas"
    ;; Two parameters
    (is (= 7 ((infix (x y) => x + y) 3 4)))
    (is (= 12 ((infix (x y) => x * y) 3 4)))
    
    ;; Three parameters
    (is (= 14 ((infix (x y z) => x + y * z) 2 3 4)))  ; 2 + (3 * 4) = 14
    
    ;; Parameters in complex expressions
    (is (= 40 ((infix (a b) => (a + b) * (a - b)) 7 3))))  ; (7+3) * (7-3) = 10 * 4 = 40

(deftest arrow-lambdas-with-function-calls
  (testing "arrow lambdas with function calls and nested expressions"
    ;; Function calls in body
    (is (= [5 8 10] (map (infix x => (max x 5)) [3 8 10])))
    (is (= [1 2 3] (map (infix x => (min x 3)) [1 2 5])))
    
    ;; Nested function calls
    (is (= [6 8 10] (map (infix x => (max (x + 3) (x * 2))) [3 4 5])))
    
    ;; Function calls with threading
    (is (= ["ABC" "DEF"] (map (infix s => s -> clojure.string/upper-case) ["abc" "def"])))))

(deftest arrow-lambdas-integration
  (testing "arrow lambdas integrated with other infix features"
    ;; With infix-let
    (is (= [2 8 18] 
           (let [multiplier 2]
             (map (infix x => x * multiplier) [1 4 9]))))
    
    ;; Nested usage
    (is (= [[1 4] [4 16] [9 36]] 
           (map (fn [x] 
                  [(infix y => y) x
                   (infix y => y * y) x]) [1 4 9])))
    
    ;; With reduce
    (is (= 10 (reduce (infix (acc x) => acc + x) 0 [1 2 3 4])))
    
    ;; Complex data processing
    (is (= [6 8 10] 
           (->> [{:value 1} {:value 2} {:value 3}]
                (map (infix item => (:value item) * 2))
                (map (infix x => x + 4)))))))

(deftest arrow-lambda-precedence
  (testing "arrow lambda precedence and grouping"
    ;; Arrow should have lower precedence than arithmetic
    (is (= [3 6 9] (map (infix x => x + 1 + 1) [1 4 7])))  ; (x + 1) + 1, not x + (1 + 1)
    
    ;; Parentheses for grouping
    (is (= [6 12 18] (map (infix x => x * (2 + 1)) [2 4 6])))  ; x * 3
    
    ;; Complex precedence
    (is (= [4 6 8] (map (infix x => x * 2 + 1 - 1) [2 3 4]))))  ; (x * 2) + 1 - 1 = x * 2

(deftest arrow-lambda-edge-cases
  (testing "edge cases and error scenarios"
    ;; Identity function
    (is (= [1 2 3] (map (infix x => x) [1 2 3])))
    
    ;; Constant function
    (is (= [42 42 42] (map (infix x => 42) [1 2 3])))
    
    ;; Single parameter in parentheses
    (is (= [2 4 6] (map (infix (x) => x * 2) [1 2 3])))
    
    ;; Complex parameter destructuring (if supported)
    ;; (is (= [4 6] (map (infix {:keys [a b]} => a + b) [{:a 1 :b 3} {:a 2 :b 4}])))
    ))

(deftest arrow-lambda-macro-expansion
  (testing "macro expansion produces correct fn forms"
    ;; Simple lambda
    (is (= '(fn [x] (* x x))
           (macroexpand-1 '(infix x => x * x))))
    
    ;; Multi-parameter lambda
    (is (= '(fn [x y] (+ x y))
           (macroexpand-1 '(infix (x y) => x + y))))
    
    ;; Complex expression
    (is (= '(fn [x] (+ (* x x) x))
           (macroexpand-1 '(infix x => x * x + x))))))