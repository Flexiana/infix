(ns infix.extreme_test
  (:require [clojure.test :refer [deftest is testing]]
            [infix.core :refer [infix]]))

;; Extreme nesting and complex control flow tests
(deftest extreme-nesting-tests
  (testing "deeply nested let bindings with infix"
    ;; Nested lets with infix expressions at each level
    (let [outer-x 10]
      (let [level1 (infix outer-x * 2 + 5)]  ; 25
        (let [level2 (infix level1 / 5 + outer-x)]  ; 5 + 10 = 15
          (let [level3 (infix level2 * 2 - 5)]  ; 30 - 5 = 25
            (let [level4 (infix level3 + level2 * 3 - outer-x)]  ; 25 + 45 - 10 = 60
              (is (= 60 level4))
              ;; Complex infix using all nested variables
              (is (= true (infix level4 > level3 and level3 > level2 and level2 > outer-x)))))))))

  (testing "infix within conditional expressions"
    (let [x 15
          y 8 
          threshold 10]
      ;; if with infix condition and infix branches
      (is (= 45 (if (infix x > threshold and y < threshold)
                  (infix x * 3)  ; true branch
                  (infix y * 2))))  ; false branch
      
      ;; Nested if expressions with infix
      (is (= "large-positive" 
             (if (infix x > 0)
               (if (infix x > threshold)
                 "large-positive"
                 (if (infix x > 5)
                   "medium-positive" 
                   "small-positive"))
               "non-positive"))))

    ;; cond with infix conditions
    (let [score 85]
      (is (= "A" (cond
                   (infix score >= 90) "A"
                   (infix score >= 80 and score < 90) "A"  ; This should match
                   (infix score >= 70 and score < 80) "B"
                   :else "F")))))

  (testing "extreme operator precedence nesting"
    ;; 10 levels of nested precedence
    (is (= 33 (infix 2 + 3 * 4 + 5 * 6 / 2 - 1 + 2 * 3 - 4 / 2 + 1)))
    ;; = 2 + (3*4) + (5*6)/2 - 1 + (2*3) - (4/2) + 1
    ;; = 2 + 12 + 15 - 1 + 6 - 2 + 1 = 33
    
    ;; Complex boolean with multiple levels
    (is (= true (infix true and (false or true) and (true or false and true) and (not false or true))))
    ;; = true and true and (true or false) and (true or true)
    ;; = true and true and true and true = true
    
    ;; Mixed arithmetic and boolean with deep precedence
    (is (= true (infix 2 * 3 + 4 > 5 and 10 / 2 = 5 or 3 * 4 < 15 and false)))
    ;; = (2*3)+4 > 5 and (10/2) = 5 or (3*4) < 15 and false
    ;; = 10 > 5 and 5 = 5 or 12 < 15 and false  
    ;; = true and true or true and false
    ;; = true or false = true
    )

  (testing "extreme function call nesting"
    ;; Functions returning functions called in infix
    (let [add-n (fn [n] (fn [x] (+ x n)))
          mult-n (fn [n] (fn [x] (* x n)))]
      (is (= true (infix ((add-n 5) 10) > ((mult-n 3) 4)))))  ; 15 > 12
    
    ;; Nested function calls with collections
    (is (= true (infix (count (filter odd? [1 2 3 4 5])) < (count (map inc [1 2 3 4])))))  ; 3 < 4
    
    ;; Higher-order function results in infix
    (let [operations [(partial + 10) (partial * 2) (partial - 5)]]
      (is (= true (infix ((first operations) 5) > ((second operations) 7)))))  ; 15 > 14
    ))

;; Nested let bindings within if expressions
(deftest nested-lets-in-conditionals
  (testing "let within if branches with infix"
    (let [base-value 20]
      (is (= 65 (if (infix base-value > 15)
                  (let [multiplier 3
                        bonus 5]
                    (infix base-value * multiplier + bonus))  ; 20*3+5 = 60+5 = 65
                  base-value))))
    
    ;; Multiple nested lets in conditional branches
    (let [x 8 y 12]
      (is (= 26 (cond
                  (infix x > y) (let [diff (infix y - x)] (infix x * diff))
                  (infix y > x) (let [sum (infix x + y)  ; 8 + 12 = 20
                                      bonus (infix sum / 4)]  ; 20 / 4 = 5
                                  (infix sum + bonus + 1))  ; 20 + 5 + 1 = 26
                  :else 0)))))

  (testing "deeply nested control structures"
    (let [data {:level1 {:level2 {:level3 {:value 42}}}}]
      ;; Nested access with infix conditions
      (is (= "deep-success"
             (if (infix (-> data :level1 :level2 :level3 :value) > 40)
               (let [extracted (-> data :level1 :level2 :level3 :value)]
                 (if (infix extracted * 2 > 80)
                   (let [doubled (infix extracted * 2)]
                     (if (infix doubled - extracted = extracted)  ; 84 - 42 = 42
                       "deep-success"
                       "calculation-error"))
                   "too-small"))
               "not-found")))))

  (testing "recursive-like nested structures"
    ;; Simulate recursive calculation with nested lets
    (letfn [(factorial-like [n acc level]
              (if (infix level <= 0)
                acc
                (let [new-acc (infix acc * n)
                      new-n (infix n - 1)
                      new-level (infix level - 1)]
                  (if (infix new-level > 0)
                    (let [intermediate (infix new-acc + new-n)]
                      (factorial-like new-n intermediate new-level))
                    new-acc))))]
      (is (number? (factorial-like 5 1 3))))))

;; Threading macros and destructuring with infix  
(deftest complex-clojure-constructs
  (testing "infix with threading macros"
    ;; -> threading with infix
    (is (= true (infix (-> 5 (+ 3) (* 2) (- 4)) > 10)))  ; (5+3)*2-4 = 16-4 = 12 > 10
    
    ;; ->> threading with infix  
    (is (= true (infix (->> [1 2 3 4 5] (filter odd?) (map inc) count) >= 3)))  ; [2 4 6] count = 3
    
    ;; Complex threading chain as operand
    (let [data [1 2 3 4 5 6 7 8 9 10]]
      (is (= true (infix (->> data 
                              (filter even?)
                              (map #(* % 2)) 
                              (reduce +)) > (-> data count (* 5))))))  ; even sum vs count*5
    
    ;; as-> with infix
    (is (= false (infix (as-> 10 x
                         (+ x 5)
                         (* x 2)
                         (- x 5)) > 25)))  ; ((10+5)*2)-5 = 30-5 = 25, so 25 > 25 is false
    )

  (testing "destructuring with infix"
    ;; Vector destructuring
    (let [[a b c :as all] [10 20 30]]
      (is (= true (infix a + b + c = (apply + all))))  ; 10+20+30 = 60 = 60
      (is (= false (infix a * 3 = b + c))))  ; 10*3 = 30, 20+30 = 50, so 30 = 50 is false
    
    ;; Map destructuring  
    (let [{:keys [x y z] :or {z 0}} {:x 15 :y 25}]
      (is (= true (infix x + y > 35 and z = 0)))
      (is (= false (infix x * y > (x + y) * 10))))  ; 15*25=375, (15+25)*10=400, so 375 > 400 is false
    
    ;; Nested destructuring with infix
    (let [{:keys [coords stats]} {:coords [100 200] :stats {:hp 50 :mp 30}}
          [cx cy] coords
          {:keys [hp mp]} stats]
      (is (= false (infix cx + cy > hp + mp * 10)))  ; 100+200=300, 50+(30*10)=50+300=350, so 300 > 350 is false
      (is (= true (infix cx / 10 + cy / 10 > hp / 5)))))  ; 10 + 20 > 10, so 30 > 10

  (testing "higher-order functions with infix"
    ;; map with infix in anonymous functions
    (is (= [14 23 35] (map #(infix % * 3 + 5) [3 6 10])))  ; 3*3+5=14, 6*3+5=23, 10*3+5=35
    
    ;; filter with infix predicates
    (is (= [15 20 25] (filter #(infix % > 10 and % <= 25) [5 10 15 20 25 30])))
    
    ;; reduce with infix
    (is (= 120 (reduce (fn [acc x] (infix acc + x * 2)) 0 [10 20 30])))  ; 0+(10*2)+(20*2)+(30*2) = 0+20+40+60 = 120
    
    ;; Complex composition
    (let [process-fn (comp #(infix % * 2 + 1) #(+ % 10))]
      (is (= true (infix (process-fn 5) > 30))))))  ; (5+10)*2+1 = 31 > 30

;; Macro composition and edge cases
(deftest macro-composition
  (testing "infix within other macro contexts"
    ;; infix inside when
    (is (= 42 (when (infix 5 > 3) (infix 6 * 7))))
    
    ;; infix inside dosync (if STM is needed)
    (let [r (ref 10)]
      (is (= 25 (dosync 
                 (alter r (fn [x] (infix x + 5)))
                 (infix @r + 10)))))
    
    ;; infix inside future
    (is (= 30 @(future (infix 10 * 3))))
    
    ;; infix inside lazy sequences
    (is (= [11 13 15] (take 3 (map #(infix % * 2 + 1) (iterate inc 5)))))  ; 5*2+1=11, 6*2+1=13, 7*2+1=15
    )

  (testing "nested macro expansions"  
    ;; Simple macro expansion test with direct evaluation
    (letfn [(calc-if [condition true-expr false-expr]
              (if condition true-expr false-expr))]
      (is (= 20 (calc-if (infix 5 > 3) 
                         (infix 4 * 5)
                         (infix 2 * 3)))))
    
    ;; Test infix within let bindings simulating macro expansion
    (let [doubled (infix 10 * 2)]
      (is (= 35 (infix doubled + 15)))))

  (testing "infix with Java interop"
    ;; Method calls as operands
    (is (= true (infix (.length "hello") < (.length "hello world"))))  ; 5 < 11
    
    ;; Field access in infix  
    (let [point (java.awt.Point. 10 20)]
      (is (= true (infix (.-x point) + (.-y point) = 30))))
    
    ;; Complex Java interop
    (let [list (java.util.ArrayList. [1 2 3 4 5])]
      (is (= true (infix (.size list) = 5 and (.get list 0) = 1))))))

;; Stress testing for limits
(deftest stress-test-limits
  (testing "extremely wide expressions"
    ;; 100 additions
    (let [numbers (range 1 101)
          expr-parts (interpose '+ numbers)
          full-expr (cons 'infix.core/infix expr-parts)]
      (is (= 5050 (eval full-expr))))  ; Sum of 1-100
    
    ;; Very wide boolean expression
    (let [conditions (repeat 50 true)
          expr-parts (interpose 'and conditions)
          full-expr (cons 'infix.core/infix expr-parts)]
      (is (= true (eval full-expr)))))
  
  (testing "extremely deep expressions"
    ;; 20 levels of nested arithmetic
    (let [base-expr 1]
      (is (= 21 (loop [expr base-expr level 0]
                  (if (>= level 20)
                    (eval (list 'infix.core/infix expr))
                    (recur (list expr '+ 1) (inc level)))))))
    
    ;; Deep boolean nesting
    (let [deep-bool (reduce (fn [acc _] (list 'true 'and acc))
                           'true
                           (range 15))]
      (is (= true (eval (list 'infix.core/infix deep-bool))))))
  
  (testing "memory and performance under stress"
    ;; Large data structures in infix
    (let [large-vec (vec (range 10000))]
      (is (= true (infix (count large-vec) = 10000)))
      (is (= true (infix (first large-vec) = 0 and (last large-vec) = 9999))))
    
    ;; Complex calculations that might cause stack overflow
    (let [result (infix ((((((1 + 2) * 3) + 4) * 5) + 6) * 7))]  ; Deeply nested arithmetic
      (is (number? result))
      (is (= 497 result)))))  ; ((((1+2)*3)+4)*5)+6)*7 = (((3*3)+4)*5)+6)*7 = ((13*5)+6)*7 = (71*7) = 497