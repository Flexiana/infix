(ns infix.threading_test
  (:require [clojure.test :refer [deftest is testing]]
            [infix.core :refer [infix]]))

;; Direct Threading Macro Support Tests
(deftest thread-first-operator
  (testing "-> thread-first operator"
    ;; Basic thread-first with simple functions
    (is (= 5 (infix {:a 1 :b 2} -> (get :b) -> (+ 3))))  ; {:a 1 :b 2} -> (get :b) -> (+ 3) = 2 + 3 = 5
    
    ;; Thread-first with map operations
    (is (= {:a 1 :b 2 :c 3} (infix {:a 1 :b 2} -> (assoc :c 3))))
    
    ;; Thread-first with method chaining style
    (is (= "HELLO" (infix "hello" -> (.toUpperCase))))
    
    ;; Multiple thread-first operations
    (is (= 8 (infix 5 -> (+ 2) -> (+ 1))))  ; 5 + 2 + 1 = 8
    
    ;; Thread-first in larger expressions
    (is (= 10 (infix (5 -> (+ 3)) + 2)))))  ; (5 + 3) + 2 = 10

(deftest thread-last-operator  
  (testing "->> thread-last operator"
    ;; Basic thread-last with collection operations
    (is (= [2 4 6] (vec (infix [1 2 3] ->> (map #(* % 2))))))
    
    ;; Thread-last with filtering and mapping
    (is (= [2 6 10] (vec (infix [1 2 3 4 5] ->> (filter odd?) ->> (map #(* % 2))))))
    
    ;; Thread-last with reduction
    (is (= 18 (infix [1 3 5] ->> (map #(* % 2)) ->> (reduce +))))
    
    ;; Thread-last with single function
    (is (= 3 (infix [1 2 3] ->> count)))
    
    ;; Thread-last in expressions
    (is (= 8 (infix ([1 2 3] ->> count) + 5)))))

(deftest some-thread-first-operator
  (testing "some-> nil-safe thread-first"
    ;; some-> with successful chain
    (is (= "JOHN" (infix {:user {:name "john"}} some-> :user some-> :name some-> (.toUpperCase))))
    
    ;; some-> with nil in chain (should return nil)
    (is (= nil (infix {:user nil} some-> :user some-> :name some-> (.toUpperCase))))
    
    ;; some-> with map access
    (is (= 42 (infix {:data {:value 42}} some-> :data some-> :value)))
    (is (= nil (infix {:data nil} some-> :data some-> :value)))
    
    ;; some-> mixed with regular operations
    (is (= true (infix ({:count 5} some-> :count) > 3)))))

(deftest some-thread-last-operator
  (testing "some->> nil-safe thread-last"
    ;; some->> with collection operations
    (is (= [2 3] (vec (infix [1 2] some->> (map inc)))))
    
    ;; some->> with nil (should return nil)
    (is (= nil (infix nil some->> (map inc) some->> (filter even?))))
    
    ;; some->> with filtering
    (is (= [2 4 6] (vec (infix [1 2 3 4 5 6] some->> (filter even?)))))
    (is (= [] (vec (infix nil some->> (filter even?)))))  ; vec of nil is [], not nil
    
    ;; some->> in expressions
    (is (= 5 (infix ([1 2] some->> count) + 3)))))  ; 2 + 3 = 5

(deftest threading-precedence
  (testing "threading operator precedence with other operators"
    ;; Threading has lower precedence than arithmetic
    (is (= 15 (infix (10 + 5) -> (identity))))  ; (10 + 5) -> identity = 15
    
    ;; Threading with boolean operations
    (is (= false (infix ([1 2] ->> empty?) or false)))  ; false or false = false
    
    ;; Threading with comparisons
    (is (= true (infix ([1 2 3] ->> count) > 2)))  ; 3 > 2 = true
    
    ;; Complex precedence with parentheses
    (is (= 16 (infix (5 -> (+ 3)) * ([1 2] ->> count))))))  ; (5 + 3) * 2 = 8 * 2 = 16

(deftest mixed-threading-operators
  (testing "mixing different threading operators"
    ;; Combine -> and ->>
    (is (= [11 12 13] (vec (infix {:data [1 2 3]} -> :data ->> (map #(+ % 10))))))
    
    ;; Combine with some-> and some->>
    (is (= 3 (infix {:items [1 2 3]} some-> :items some->> count)))
    (is (= nil (infix {:items nil} some-> :items some->> count)))
    
    ;; Complex chaining
    (is (= "3" (infix {:users [{:name "alice"} {:name "bob"} {:name "charlie"}]} 
                      -> :users 
                      ->> count 
                      -> str)))))

(deftest threading-edge-cases
  (testing "threading operator edge cases"
    ;; Identity operations
    (is (= 42 (infix 42 -> identity)))
    (is (= [1 2 3] (infix [1 2 3] ->> identity)))
    
    ;; Threading with complex function calls
    (is (= 3 (infix {:a 1 :b {:c 2 :d 3}} -> :b -> (get :d))))
    
    ;; Threading with Java interop
    (is (= 5 (infix "hello" -> .length)))
    (is (= "WORLD" (infix "world" -> .toUpperCase)))
    
    ;; Threading with nested data structures
    (is (= [2 4] (vec (infix [[1 2] [3 4]] ->> (map second)))))
    
    ;; Threading in conditional expressions  
    (is (= "large" (if (infix [1 2 3 4 5] ->> count -> (> 3))
                     "large" 
                     "small")))))

(deftest complex-threading-scenarios
  (testing "complex and nested threading scenarios"
    ;; Business logic example
    (let [users [{:name "Alice" :age 25 :active true}
                 {:name "Bob" :age 30 :active false} 
                 {:name "Charlie" :age 35 :active true}]]
      (is (= ["Alice" "Charlie"] 
             (infix users 
                    ->> (filter :active)
                    ->> (map :name)
                    ->> vec))))
    
    ;; Data transformation pipeline
    (let [data {:transactions [{:amount 100 :type :debit}
                               {:amount 200 :type :credit}
                               {:amount 50 :type :debit}]}]
      (is (= 150 (infix data 
                        -> :transactions
                        ->> (filter #(= :debit (:type %)))
                        ->> (map :amount)  
                        ->> (reduce +)))))
    
    ;; Nested map operations
    (is (= {:user {:profile {:display-name "JOHN DOE"}}}
           (infix {:user {:profile {:name "john doe"}}}
                  -> (update-in [:user :profile] #(assoc % :display-name (.toUpperCase (:name %))))
                  -> (update-in [:user :profile] #(dissoc % :name)))))
    
    ;; Error handling with some->
    (let [maybe-data {:result {:value 42}}
          bad-data {:result nil}]
      (is (= 42 (infix maybe-data some-> :result some-> :value)))
      (is (= nil (infix bad-data some-> :result some-> :value))))))