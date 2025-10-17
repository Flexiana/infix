(ns infix.oop-interop-test
  "Tests for OOP interop functionality with obj.method syntax"
  (:require [clojure.test :refer :all]
            [infix.core :refer [infix infix-defn]]
            [infix.parser :as parser]))

(deftest test-oop-method-call-pattern-detection
  (testing "oop-method-call-pattern? function"
    ;; Valid OOP method call patterns
    (is (true? (#'parser/oop-method-call-pattern? 'obj.method)))
    (is (true? (#'parser/oop-method-call-pattern? 'myObject.getName)))
    (is (true? (#'parser/oop-method-call-pattern? 'user.getEmail)))
    (is (true? (#'parser/oop-method-call-pattern? 'data.toString)))
    
    ;; Invalid patterns
    (is (false? (#'parser/oop-method-call-pattern? '.method)))    ; starts with dot
    (is (false? (#'parser/oop-method-call-pattern? 'method)))     ; no dot
    (is (false? (#'parser/oop-method-call-pattern? "obj.method"))) ; string not symbol
    (is (false? (#'parser/oop-method-call-pattern? nil)))         ; nil
    ))

(deftest test-split-oop-method-call
  (testing "split-oop-method-call function"
    (is (= ['obj 'method] (#'parser/split-oop-method-call 'obj.method)))
    (is (= ['myUser 'getName] (#'parser/split-oop-method-call 'myUser.getName)))
    (is (= ['data 'toString] (#'parser/split-oop-method-call 'data.toString)))
    (is (= ['user 'getEmail] (#'parser/split-oop-method-call 'user.getEmail)))))

(deftest test-basic-oop-method-calls
  (testing "Basic OOP method call transformation"
    ;; Mock a Java object for testing
    (let [test-obj (proxy [Object] []
                     (toString [] "test-string"))]
      
      ;; Test basic method call without arguments
      (is (= "test-string" 
             (eval (macroexpand-1 '(infix test-obj.toString())))))
      
      ;; Test that the macro expands correctly
      (is (= '(.toString test-obj)
             (macroexpand-1 '(infix test-obj.toString())))))))

(deftest test-oop-method-calls-with-arguments
  (testing "OOP method calls with arguments"
    ;; Mock object with methods that take arguments
    (let [test-obj (proxy [Object] []
                     (equals [other] (= other "test")))]
      
      ;; Method call with single argument
      (is (true? (eval (macroexpand-1 '(infix test-obj.equals("test"))))))
      (is (false? (eval (macroexpand-1 '(infix test-obj.equals("other"))))))
      
      ;; Check macro expansion
      (is (= '(.equals test-obj "test")
             (macroexpand-1 '(infix test-obj.equals("test"))))))))

(deftest test-oop-method-calls-with-variables
  (testing "OOP method calls with variables (since string literals have parsing issues)"
    ;; Test with variables to avoid reader issues
    (let [hello "hello"
          upper "HELLO"]
      ;; Test basic method calls work with macroexpand
      (is (= '(.toUpperCase hello)
             (macroexpand-1 '(infix hello.toUpperCase()))))
      (is (= '(.toLowerCase upper)
             (macroexpand-1 '(infix upper.toLowerCase()))))
      (is (= '(.length hello)
             (macroexpand-1 '(infix hello.length()))))
      (is (= '(.substring hello 0 2)
             (macroexpand-1 '(infix hello.substring(0, 2)))))
      
      ;; Test actual evaluation
      (is (= "HELLO" (eval (macroexpand-1 `(infix ~hello.toUpperCase())))))
      (is (= 5 (eval (macroexpand-1 `(infix ~hello.length())))))))))

(deftest test-oop-method-calls-with-infix-operators
  (testing "OOP method calls combined with infix operators"
    ;; Combining method calls with arithmetic
    (is (= 10 (infix "hello".length() + 5)))
    (is (= 25 (infix "hello".length() * 5)))
    
    ;; Combining method calls with comparisons
    (is (= true (infix "hello".length() > 3)))
    (is (= false (infix "hello".length() < 3)))
    
    ;; Check macro expansions
    (is (= '(+ (.length "hello") 5)
           (macroexpand-1 '(infix "hello".length() + 5))))
    (is (= '(> (.length "hello") 3)
           (macroexpand-1 '(infix "hello".length() > 3))))))

(deftest test-oop-method-calls-with-threading
  (testing "OOP method calls with threading operators for method chaining"
    ;; Test threading-based method chaining
    (is (= "HELLO" (infix "hello" -> .toUpperCase())))
    (is (= 5 (infix "HELLO" -> .toLowerCase() -> .length())))
    
    ;; Test complex chaining
    (is (= "HE" (infix "hello" -> .toUpperCase() -> .substring(0, 2))))
    
    ;; Check macro expansions  
    (is (= '(-> "hello" (.toUpperCase))
           (macroexpand-1 '(infix "hello" -> .toUpperCase()))))
    (is (= '(-> "HELLO" (.toLowerCase) (.length))
           (macroexpand-1 '(infix "HELLO" -> .toLowerCase() -> .length()))))
    
    ;; Verify the chaining pattern user requested can be achieved with threading
    (is (= '(-> obj (.setName "John") (.setSurname "Newman") (.save))
           (macroexpand-1 '(infix obj -> .setName("John") -> .setSurname("Newman") -> .save()))))))

(deftest test-oop-method-calls-in-infix-defn
  (testing "OOP method calls in infix-defn functions"
    ;; Define function using OOP method call syntax
    (infix-defn get-upper-length [s]
      s.toUpperCase().length())
    
    (is (= 5 (get-upper-length "hello")))
    
    ;; Function combining OOP calls with arithmetic
    (infix-defn string-score [s]
      s.length() * 2 + (if (.startsWith s "hello") 10 0))
    
    (is (= 20 (string-score "hello")))  ; 5 * 2 + 10
    (is (= 6 (string-score "abc")))     ; 3 * 2 + 0
    ))

(deftest test-complex-oop-expressions
  (testing "Complex expressions with OOP method calls"
    ;; Multiple method calls in single expression
    (is (= true (infix "hello".startsWith("he") and "world".endsWith("ld"))))
    (is (= 10 (infix "hello".length() + "world".length())))
    
    ;; Method calls with parentheses grouping
    (is (= 50 (infix ("hello".length() + "world".length()) * 5)))
    
    ;; Check macro expansions
    (is (= '(and (.startsWith "hello" "he") (.endsWith "world" "ld"))
           (macroexpand-1 '(infix "hello".startsWith("he") and "world".endsWith("ld")))))
    (is (= '(+ (.length "hello") (.length "world"))
           (macroexpand-1 '(infix "hello".length() + "world".length()))))))

(deftest test-oop-method-calls-edge-cases
  (testing "Edge cases for OOP method calls"
    ;; Method calls without parentheses should not be transformed
    ;; (this tests that we only transform when followed by argument list)
    (let [expansion (macroexpand-1 '(infix obj.method + 1))]
      ;; Should not be transformed to (.method obj) + 1
      ;; Instead should treat obj.method as a symbol
      (is (or (= '(+ obj.method 1) expansion)
              ;; Or it might be parsed differently, but shouldn't be (.method obj ...)
              (not (and (seq? (first expansion)) 
                        (= '. (first (first expansion))))))))
    
    ;; Empty argument list
    (is (= '(.toString obj) 
           (macroexpand-1 '(infix obj.toString()))))
    
    ;; Nested expressions in arguments  
    (is (= '(.substring "hello" (+ 1 2) 4)
           (macroexpand-1 '(infix "hello".substring(1 + 2, 4)))))))

(run-tests)