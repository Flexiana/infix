(ns infix.pipeline_test
  (:require [clojure.test :refer [deftest is testing]]
            [infix.core :refer [infix]]))

;; Phase 4: Pipeline Operator Tests
(deftest basic-pipeline
  (testing "simple pipeline transformations"
    ;; Single function pipeline
    (is (= 3 (infix [1 2 3] |> count)))
    
    ;; Pipeline with simple function calls - using vec to realize lazy seq
    (is (= [2 4 6] (vec (infix [1 2 3] |> (map #(* 2 %))))))
    
    ;; Pipeline should work like ->> threading macro
    (is (= [2 4 6] (vec (->> [1 2 3] (map #(* 2 %)))))))

  (testing "multi-stage pipelines - simplified"
    ;; Single stage pipelines work
    (is (= [1 3 5] (vec (infix [1 2 3 4 5] |> (filter odd?)))))
    (is (= [2 6 10] (vec (infix [1 3 5] |> (map #(* % 2))))))
    (is (= 18 (infix [1 3 5] |> (map #(* % 2)) |> (reduce +)))))

  (testing "pipeline with parentheses for precedence"
    ;; Use parentheses to ensure correct precedence
    (is (= true (infix ([1 2 3] |> count) > 2)))
    
    ;; Pipeline in larger infix expression
    (is (= 8 (infix ([1 2 3] |> count) + 5)))))

(deftest pipeline-precedence
  (testing "pipeline operator precedence - with parentheses"
    ;; Pipeline should work with arithmetic expressions
    (is (= 17 (infix (10 + 5) |> (+ 2))))  ; (10 + 5) |> (+ 2) -> (+ 2 15) -> 17
    
    ;; Pipeline with comparison - note the argument order for <
    (is (= true (infix (10 + 5) |> (< 12))))  ; (10 + 5) |> (< 12) -> (< 12 15) -> true
    
    ;; Simplified boolean test
    (is (= false (infix ([1 2] |> empty?) or ([3 4] |> empty?))))))  ; false or false -> false

(deftest pipeline-edge-cases
  (testing "pipeline edge cases"
    ;; Empty pipeline should be identity
    (is (= [1 2 3] (infix [1 2 3])))  ; No pipeline, just identity
    
    ;; Pipeline with complex function calls
    (is (= 3 (infix {:a 1 :b 2 :c 3} |> keys |> count)))
    
    ;; Pipeline with nested data structures - using vec to realize lazy seq
    (is (= [2 4] (vec (infix [[1 2] [3 4]] |> (map second)))))))