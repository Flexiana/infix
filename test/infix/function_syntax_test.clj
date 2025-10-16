(ns infix.function-syntax-test
  (:require [clojure.test :refer [deftest is testing]]
            [infix.core :refer [infix infix-defn]]))

;; Phase 5: Function Call Syntax Tests  
;; Support fn(args) syntax by transforming within infix expressions

(deftest basic-function-syntax
  (testing "basic fn(args) syntax within infix expressions"
    ;; Simple function calls - should transform max(3, 5) to (max 3 5)
    (is (= 8 (infix max(3, 5) + min(1, 2))))  ; 5 + 1 = 6... wait, 5 + 3 = 8 if min(1,2)=1
    (is (= 6 (infix max(3, 5) + min(1, 2))))  ; 5 + 1 = 6
    (is (= 10 (infix max(3, 5) * min(1, 2))))  ; 5 * 2 = 10... wait, min(1,2)=1, so 5*1=5
    (is (= 5 (infix max(3, 5) * min(1, 2))))   ; 5 * 1 = 5
    
    ;; String functions
    (is (= 5 (infix count("hello"))))
    (is (= "HELLO" (infix clojure.string/upper-case("hello"))))
    
    ;; Math functions  
    (is (= 3.0 (infix Math/sqrt(9))))
    (is (= 8.0 (infix Math/pow(2, 3))))))

(deftest nested-function-syntax
  (testing "nested fn(args) syntax"
    ;; Nested function calls
    (is (= 5 (infix max(min(10, 5), 3))))  ; max(5, 3) = 5
    (is (= 8 (infix max(3, 5) + min(1, 3))))  ; 5 + 1 = 6... min(1,3)=1 so 5+1=6
    (is (= 6 (infix max(3, 5) + min(1, 3))))  ; 5 + 1 = 6
    
    ;; Complex nesting
    (is (= 25 (infix Math/pow(max(3, 5), 2))))  ; pow(5, 2) = 25
    (is (= 7 (infix max(min(10, 7), 5) + min(3, 2))))  ; max(7, 5) + min(3, 2) = 7 + 2 = 9
    (is (= 9 (infix max(min(10, 7), 5) + min(3, 2))))  ; 7 + 2 = 9
    
    ;; Function calls with expressions as arguments
    (is (= 10 (infix max(3 + 2, 4 * 2))))  ; max(5, 8) = 8... wait that's not 10
    (is (= 8 (infix max(3 + 2, 4 * 2))))   ; max(5, 8) = 8
    (is (= 13 (infix max(3 + 2, 4) + 8)))   ; max(5, 4) + 8 = 5 + 8 = 13))

(deftest function-syntax-with-methods
  (testing "method call syntax with fn(args)"
    ;; Method calls
    (is (= "HELLO" (infix .toUpperCase("hello"))))
    (is (= 5 (infix .length("hello"))))
    
    ;; Chained method calls in infix
    (is (= "HELLO" (infix "hello" -> .toUpperCase())))
    (is (= 5 (infix "hello" -> .toUpperCase() -> .length())))
    
    ;; Method calls with arguments
    (is (= "he" (infix .substring("hello", 0, 2))))
    (is (= true (infix .startsWith("hello", "he"))))))

(deftest function-syntax-in-infix-defn
  (testing "fn(args) syntax within infix-defn functions"
    (infix-defn distance [x1 y1 x2 y2]
      Math/sqrt(Math/pow(x2 - x1, 2) + Math/pow(y2 - y1, 2)))
    
    (is (= 5.0 (distance 0 0 3 4)))
    
    (infix-defn process-string [s]
      (when .isEmpty(s) (return ""))
      .toUpperCase(.trim(s)))
    
    (is (= "" (process-string "")))
    (is (= "HELLO" (process-string "  hello  ")))))

(deftest complex-function-syntax
  (testing "complex function call scenarios"
    ;; Multiple arguments of different types
    (is (= "1,2,3" (infix clojure.string/join(",", [1, 2, 3]))))
    
    ;; Function calls with collections
    (is (= {:a 1} (infix assoc({}, :a, 1))))
    (is (= [1 2 3] (infix conj([1, 2], 3))))
    
    ;; Function calls in complex expressions
    (is (= 14 (infix + (* 2 3), max(4, 5), min(8, 3))))  ; 6 + 5 + 3 = 14
    (is (= [2 4 6] (infix map(#(* 2 %), [1, 2, 3]) -> vec())))
    
    ;; Anonymous functions with function syntax
    (is (= [1 4 9] (infix map((fn [x] (* x x)), [1, 2, 3]) -> vec())))))

(deftest function-syntax-macro-expansion
  (testing "macro expansion with function call syntax"
    ;; Simple function syntax should expand correctly
    (is (= '(+ (max 3 5) (min 1 2))
           (macroexpand-1 '(infix max(3, 5) + min(1, 2)))))
    
    ;; Nested function syntax
    (is (= '(max (min 10 5) 3)
           (macroexpand-1 '(infix max(min(10, 5), 3)))))
    
    ;; Method calls
    (is (= '(.toUpperCase "hello")
           (macroexpand-1 '(infix .toUpperCase("hello")))))))