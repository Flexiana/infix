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
    ;; Boolean logic - single expression body
    (infix-defn in-range? [x min-val max-val] 
      x >= min-val and x <= max-val)
    (is (= true (in-range? 5 1 10)))
    (is (= false (in-range? 15 1 10)))
    
    ;; Threading operations - single expression body
    (infix-defn process-data [data]
      data -> :items ->> (filter :active?) ->> count)
    (is (= 2 (process-data {:items [{:active? true} {:active? false} {:active? true}]})))
    
    ;; Complex arithmetic - single expression body
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

(deftest early-return-mechanism
  (testing "early return functionality"
    ;; Basic early return  
    (infix-defn safe-divide [x y]
      (when (infix y = 0) (return nil))
      (infix x / y))
    
    (is (= nil (safe-divide 10 0)))
    (is (= 2.5 (safe-divide 10 4)))
    
    ;; Early return with guard clauses
    (infix-defn validate-age [age]
      (when (infix age < 0) (return "Invalid: negative age"))
      (when (infix age > 150) (return "Invalid: too old"))
      (when (infix age < 18) (return "Minor"))
      "Adult")
    
    (is (= "Invalid: negative age" (validate-age -5)))
    (is (= "Invalid: too old" (validate-age 200)))
    (is (= "Minor" (validate-age 15)))
    (is (= "Adult" (validate-age 25)))
    
    ;; Early return in complex flow
    (infix-defn process-user [user]
      (when (not (:active? user)) (return {:error "User not active"}))
      (when (not (:email user)) (return {:error "Missing email"}))
      {:result (infix user -> :email -> clojure.string/upper-case)})
    
    (is (= {:error "User not active"} 
           (process-user {:active? false :email "test@example.com"})))
    (is (= {:error "Missing email"} 
           (process-user {:active? true})))
    (is (= {:result "TEST@EXAMPLE.COM"} 
           (process-user {:active? true :email "test@example.com"})))))

(deftest return-macro-expansion
  (testing "return statement macro expansion"
    ;; Simple return should expand to throw with Return exception
    (is (seq? (macroexpand-1 '(infix.core/return 42))))
    
    ;; Function with return should be wrapped in try-catch
    (let [expanded (macroexpand-1 '(infix.core/infix-defn test-fn [x] 
                                     (when x > 10 (return "big"))
                                     "small"))]
      (is (= 'clojure.core/defn (first expanded)))
      (is (= 'test-fn (second expanded))))))

(deftest multiple-returns
  (testing "functions with multiple return points"
    (infix-defn categorize-number [n]
      (when (infix n < 0) (return "negative"))
      (when (infix n = 0) (return "zero"))
      (when (infix n <= 10) (return "small positive"))
      (when (infix n <= 100) (return "medium positive"))
      "large positive")
    
    (is (= "negative" (categorize-number -5)))
    (is (= "zero" (categorize-number 0)))
    (is (= "small positive" (categorize-number 7)))
    (is (= "medium positive" (categorize-number 50)))
    (is (= "large positive" (categorize-number 500)))))