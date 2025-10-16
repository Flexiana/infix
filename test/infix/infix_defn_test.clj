(ns infix.infix-defn-test
  (:require [clojure.test :refer [deftest is testing]]
            [infix.core :refer [infix-defn]]))

;; Phase 8: infix-defn Tests
;; Support function definitions with infix syntax

(deftest basic-infix-defn
  (testing "basic function definition with infix body"
    ;; Simple arithmetic function
    (infix-defn square [x] x * x)
    (is (= 9 (square 3)))
    (is (= 16 (square 4)))
    
    ;; Multiple parameters
    (infix-defn add-multiply [x y z] x + y * z)
    (is (= 11 (add-multiply 1 2 5)))  ; 1 + (2 * 5) = 11
    
    ;; Function with comparison
    (infix-defn greater-than-ten? [x] x > 10)
    (is (= true (greater-than-ten? 15)))
    (is (= false (greater-than-ten? 5)))))

(deftest infix-defn-with-complex-expressions
  (testing "complex expressions in function body"
    ;; Boolean logic
    (infix-defn in-range? [x min-val max-val] 
      x >= min-val and x <= max-val)
    (is (= true (in-range? 5 1 10)))
    (is (= false (in-range? 15 1 10)))
    
    ;; Threading operations
    (infix-defn process-data [data]
      data -> :items ->> (filter :active?) ->> count)
    (is (= 2 (process-data {:items [{:active? true} {:active? false} {:active? true}]})))
    
    ;; Complex arithmetic
    (infix-defn quadratic [a b c x]
      a * x * x + b * x + c)
    (is (= 6 (quadratic 1 2 3 1)))))  ; 1*1*1 + 2*1 + 3 = 6

(deftest infix-defn-with-docstrings
  (testing "function definitions with docstrings"
    (infix-defn circle-area 
      "Calculate the area of a circle given radius"
      [radius]
      3.14159 * radius * radius)
    
    (is (= "Calculate the area of a circle given radius" 
           (:doc (meta #'circle-area))))
    (is (< (abs (- (circle-area 2) 12.56636)) 0.001))))

(deftest infix-defn-macro-expansion
  (testing "macro expansion produces correct defn forms"
    ;; Simple case
    (is (= '(clojure.core/defn square [x] (* x x))
           (macroexpand-1 '(infix.core/infix-defn square [x] x * x))))
    
    ;; With docstring
    (is (= '(clojure.core/defn documented-fn "A test function" [x] (+ x 1))
           (macroexpand-1 '(infix.core/infix-defn documented-fn "A test function" [x] x + 1))))
    
    ;; Multiple parameters
    (is (= '(clojure.core/defn add-two [x y] (+ x y))
           (macroexpand-1 '(infix.core/infix-defn add-two [x y] x + y))))))