(ns infix.function-call-test
  (:require [clojure.test :refer [deftest is testing]]
            [infix.core :refer [infix]]))

;; Phase 5: Function Call Syntax Tests
;; Enhanced function call integration with infix operators
(deftest enhanced-function-calls  
  (testing "function calls work seamlessly with infix operators"
    ;; Function calls with infix operators
    (is (= 6 (infix (max 3 5) + (min 1 3))))  ; 5 + 1 = 6
    (is (= 5 (infix (max 3 5) * (min 2 1))))  ; 5 * 1 = 5
    
    ;; Function calls preserve normal Clojure syntax
    (is (= 3 (infix (count [1 2 3]))))
    (is (= true (infix (empty? []))))
    (is (= false (infix (empty? [1 2]))))))

(deftest function-calls-with-strings-and-regex
  (testing "function calls with string and regex arguments"
    ;; String operations using standard Clojure syntax
    (is (= "HELLO" (infix (clojure.string/upper-case "hello"))))
    (is (= "hello world" (infix (str "hello" " " "world"))))
    
    ;; String splitting
    (is (= ["hello" "world"] (infix (clojure.string/split "hello world" #" "))))
    
    ;; Regex matching  
    (is (= "world" (infix (re-find #"world" "hello world"))))
    (is (= nil (infix (re-find #"xyz" "hello world"))))))

(deftest nested-function-calls
  (testing "function calls within function calls"
    ;; Nested function calls with standard Clojure syntax
    (is (= 6 (infix (max (min 1 2) (max 3 6)))))
    (is (= 15 (infix (+ (max 5 10) (min 10 5)))))
    
    ;; Deep nesting
    (is (= 4 (infix (count (clojure.string/split (str "a" "b" "c" "d") #"")))))
    
    ;; Mixed with collections
    (is (= [1 4 9] (vec (infix (map (fn [x] (* x x)) [1 2 3])))))
    
    ;; Function calls as arguments
    (is (= 10 (infix (reduce + 0 [1 2 3 4]))))))

(deftest function-calls-with-infix-operators
  (testing "combining function calls with infix operators"
    ;; Arithmetic with function calls
    (is (= 6 (infix (max 3 5) + (min 1 3))))  ; 5 + 1 = 6
    (is (= 10 (infix (max 3 5) * (min 2 3))))  ; 5 * 2 = 10
    
    ;; Comparisons with function calls  
    (is (= true (infix (count [1 2 3]) > 2)))  ; 3 > 2 = true
    (is (= false (infix (max 1 2 3) < 2)))     ; 3 < 2 = false
    
    ;; Boolean logic with function calls
    (is (= true (infix (empty? []) or (count [1]) > 0)))  ; true or true = true
    (is (= false (infix (empty? [1 2]) and (count []) = 0)))  ; false and true = false
    
    ;; Function calls in complex expressions
    (is (= 24 (infix ((max 5 10) + (min 2 8)) * 2)))  ; (10 + 2) * 2 = 24
    ))

(deftest function-calls-with-threading
  (testing "function calls combined with threading operators"
    ;; Function calls with thread-first
    (is (= "HELLO" (infix "hello" -> (clojure.string/upper-case))))
    (is (= 3 (infix {:a 1 :b 2 :c 3} -> (count))))
    
    ;; Function calls with thread-last
    (is (= [2 4 6] (vec (infix [1 2 3] ->> (map (fn [x] (* x 2)))))))
    (is (= 6 (infix [1 2 3] ->> (reduce + 0))))
    
    ;; Mixed threading with function calls
    (is (= 6 (infix {:nums [1 2 3]} -> (get :nums) ->> (reduce + 0))))
    
    ;; Nil-safe with function calls
    (is (= "JOHN" (infix {:user {:name "john"}} some-> (get :user) some-> (get :name) some-> (clojure.string/upper-case))))
    (is (= nil (infix {:user nil} some-> (get :user) some-> (get :name))))))

(deftest function-calls-edge-cases
  (testing "edge cases and complex scenarios"
    ;; Function calls with keywords and maps
    (is (= 1 (infix (get {:a 1 :b 2} :a))))
    (is (= nil (infix (get {:a 1 :b 2} :c))))
    
    ;; Function calls with vectors and access
    (is (= 2 (infix (nth [1 2 3] 1))))
    (is (= [2 3] (infix (subvec [1 2 3 4] 1 3))))
    
    ;; Function calls with complex data structures and threading
    (is (= [:a :c] (infix (keys {:a 1 :b 2 :c 3}) ->> (filter (fn [k] (not= k :b))) ->> vec)))
    
    ;; Function calls with anonymous functions
    (is (= [2 4 6] (vec (infix (map (fn [x] (* x 2)) [1 2 3])))))
    
    ;; Function calls with partial application
    (is (= [11 12 13] (vec (infix (map (partial + 10) [1 2 3])))))
    
    ;; Function calls with Java interop
    (is (= 5 (infix (.length "hello"))))
    (is (= "WORLD" (infix (.toUpperCase "world"))))))

(deftest function-call-precedence
  (testing "function call precedence with operators"
    ;; Function calls have high precedence (evaluated first)
    (is (= 11 (infix (max 5 10) + 1)))  ; max first, then addition: 10 + 1 = 11
    (is (= 50 (infix (max 5 10) * (count [1 2 3 4 5]))))  ; 10 * 5 = 50
    
    ;; Parentheses for grouping arithmetic in function arguments
    (is (= 5 (infix (max (2 + 3) 4))))  ; Should parse as (max (2 + 3) 4) = max(5, 4) = 5
    (is (= 13 (infix (max 2 3) + (count [1 2]) * 5)))  ; 3 + (2 * 5) = 3 + 10 = 13
    
    ;; Complex precedence with boolean logic
    (is (= true (infix (count [1 2 3]) > (max 1 2) and (min 5 10) < 10))))  ; 3 > 2 and 5 < 10 = true and true = true

(deftest function-call-syntax-variations
  (testing "different syntax variations and edge cases"  
    ;; Standard Clojure function syntax works seamlessly
    (is (= 5 (infix (max 3 5))))  ; Standard spacing
    (is (= 5 (infix (max 3 5))))  ; All function calls use standard Clojure syntax
    
    ;; Complex argument expressions with infix inside function calls
    (is (= 5 (infix (max (2 + 3) (4 + 1)))))  ; max(5, 5) = 5
    (is (= 10 (infix (+ ((max 2 3) * 2) (min 4 5)))))  ; (3 * 2) + 4 = 6 + 4 = 10
    
    ;; Function calls with conditional expressions
    (is (= 5 (infix (if true (max 3 5) (min 1 2)))))  ; if true then max(3,5) else min(1,2) = 5
    (is (= 1 (infix (if false (max 3 5) (min 1 2))))))  ; if false then max(3,5) else min(1,2) = 1
    ))