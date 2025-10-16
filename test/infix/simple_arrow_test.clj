(ns infix.simple-arrow-test
  (:require [clojure.test :refer [deftest is testing]]
            [infix.core :refer [infix]]))

(deftest basic-arrow-lambdas
  (testing "basic arrow lambda functionality"
    ;; Single parameter arithmetic
    (is (= [1 4 9] (map (infix x => x * x) [1 2 3])))
    (is (= [2 4 6] (map (infix x => x * 2) [1 2 3])))
    
    ;; Single parameter comparisons
    (is (= [2 3 4] (filter (infix x => x > 1) [1 2 3 4])))
    
    ;; Multi-parameter lambdas
    (is (= 7 ((infix (x y) => x + y) 3 4)))
    (is (= 12 ((infix (x y) => x * y) 3 4)))
    
    ;; Complex expressions
    (is (= [2 6 12] (map (infix x => x * x + x) [1 2 3])))
    
    ;; With reduce
    (is (= 10 (reduce (infix (acc x) => acc + x) 0 [1 2 3 4])))
    
    ;; Test that lambdas actually work
    (is (= 9 ((infix x => x * x) 3)))
    (is (= 7 ((infix (x y) => x + y) 3 4)))))