(ns infix.realistic-simple-test
  "Simplified realistic tests that demonstrate readability and functionality"
  (:require [clojure.test :refer :all]
            [infix.core :refer [infix infix-defn]]))

;; =============================================================================
;; BUSINESS LOGIC EXAMPLES - READABLE AND FUNCTIONAL
;; =============================================================================

(deftest business-logic-readability-test
  (testing "Business logic is more readable with infix"
    
    ;; Traditional Clojure discount calculation
    (defn discount-traditional [subtotal tier quantity]
      (let [base-rate (cond (= tier :premium) 0.15
                           (>= quantity 10) 0.10
                           :else 0.05)]
        (min (* subtotal base-rate) 100)))
    
    ;; Infix version - more readable mathematical expressions
    (infix-defn discount-infix [subtotal tier quantity]
      (let [base-rate (cond (tier = :premium) 0.15
                           (quantity >= 10) 0.10
                           :else 0.05)]
        (min (subtotal * base-rate) 100)))
    
    ;; Both produce identical results
    (is (= 100.0 (discount-traditional 1000 :premium 5)))
    (is (= 100.0 (discount-infix 1000 :premium 5)))
    (is (= 50.0 (discount-traditional 500 :regular 15)))
    (is (= 50.0 (discount-infix 500 :regular 15)))
    
    ;; Pricing calculation with complex formula
    (infix-defn calculate-price [base-cost markup labor-hours hourly-rate tax-rate]
      (((base-cost * (1 + markup)) + (labor-hours * hourly-rate)) * (1 + tax-rate)))
    
    (let [price (calculate-price 100 0.20 5 25 0.08)]
      (is (> price 260))  ; (100 * 1.2 + 5 * 25) * 1.08 = 270
      (is (< price 280)))))

;; =============================================================================
;; DATA PROCESSING PIPELINES - THREADING AND TRANSFORMATIONS
;; =============================================================================

(deftest data-pipeline-readability-test
  (testing "Data processing pipelines are more readable"
    
    ;; Process user data with readable pipeline
    (infix-defn process-active-users [users]
      (users 
       ->> (filter #(:active? %))
       ->> (filter #((get % :last-login-days 999) <= 30))
       ->> (map #(assoc % :score ((get % :page-views 0) * 0.1 + (get % :purchases 0) * 2.0)))
       ->> (filter #((get % :score 0) > 5))
       ->> (sort-by :score)
       ->> reverse
       ->> (take 10)))
    
    ;; Clean product data with readable transformations  
    (infix-defn clean-products [products]
      (products
       ->> (filter #(and (:name %) (:price %)))
       ->> (map #(update % :price (fn [p] (max 0.01 p))))
       ->> (map #(assoc % :price-category
                   (cond ((get % :price 0) < 10) :budget
                         ((get % :price 0) < 50) :mid-range
                         :else :premium)))))
    
    (let [sample-users [{:active? true :last-login-days 5 :page-views 100 :purchases 3}
                        {:active? false :last-login-days 2 :page-views 200 :purchases 5}
                        {:active? true :last-login-days 45 :page-views 50 :purchases 1}
                        {:active? true :last-login-days 2 :page-views 200 :purchases 5}]
          
          processed (process-active-users sample-users)]
      
      (is (= 2 (count processed)))  ; Should filter to active users with score > 5
      (is (every? #(:active? %) processed))  ; All should be active
      (is (every? #(> (:score %) 5) processed)))  ; All should have score > 5
    
    (let [sample-products [{:name "Widget A" :price 5.99}
                          {:name "Tool B" :price 49.99}  
                          {:name "Premium C" :price 199.99}
                          {:name "Invalid" :price -10}]
          
          cleaned (clean-products sample-products)]
      
      (is (= 4 (count cleaned)))  ; All products should be kept (price fixed)
      (is (every? #(> (:price %) 0) cleaned))  ; All prices should be positive
      (is (= :budget (:price-category (first cleaned))))))) ; First item should be budget

;; =============================================================================
;; MATHEMATICAL COMPUTATIONS - READABLE FORMULAS
;; =============================================================================

(deftest mathematical-readability-test
  (testing "Mathematical formulas are more readable with infix"
    
    ;; Distance formula - compare traditional vs infix
    (defn distance-traditional [x1 y1 x2 y2]
      (Math/sqrt (+ (Math/pow (- x2 x1) 2) (Math/pow (- y2 y1) 2))))
    
    (infix-defn distance-infix [x1 y1 x2 y2]
      (Math/sqrt ((x2 - x1) -> (Math/pow 2) + (y2 - y1) -> (Math/pow 2))))
    
    ;; Both produce same results
    (is (= 5.0 (distance-traditional 0 0 3 4)))
    (is (= 5.0 (distance-infix 0 0 3 4)))
    
    ;; Quadratic formula - much more readable in infix
    (infix-defn quadratic-formula [a b c]
      (let [discriminant (b * b - 4 * a * c)]
        (when (discriminant < 0) (return []))  ; No real solutions
        (let [sqrt-disc (Math/sqrt discriminant)]
          [((- b + sqrt-disc) / (2 * a)) 
           ((- b - sqrt-disc) / (2 * a))])))
    
    (let [solutions (quadratic-formula 1 -5 6)]  ; x^2 - 5x + 6 = 0
      (is (= 2 (count solutions)))
      (is (some #(< (Math/abs (- % 2.0)) 0.001) solutions))  ; x = 2
      (is (some #(< (Math/abs (- % 3.0)) 0.001) solutions))) ; x = 3
    
    ;; Compound interest with readable formula
    (infix-defn compound-interest [principal rate years compounds-per-year]
      (principal * (Math/pow (1 + rate / compounds-per-year) (compounds-per-year * years))))
    
    (let [result (compound-interest 10000 0.05 10 12)]  ; $10k at 5% for 10 years, monthly compounding
      (is (> result 16000))  ; Should be over $16k
      (is (< result 17000)))))  ; But under $17k

;; =============================================================================
;; WEB API PROCESSING - PRACTICAL EXAMPLES
;; =============================================================================

(deftest web-api-readability-test
  (testing "Web API processing with readable conditions"
    
    ;; User authentication with readable conditions
    (infix-defn authenticate-user [username password users]
      (when (or (not username) (not password)) (return {:success false :error "Missing credentials"}))
      
      (let [user (first (filter #((get % :username) = username) users))]
        (when (not user) (return {:success false :error "User not found"}))
        (when (not ((get user :password) = password)) (return {:success false :error "Invalid password"}))
        (when (not (get user :active true)) (return {:success false :error "Account disabled"}))
        
        {:success true :user (dissoc user :password)}))
    
    ;; Rate limiting with clear logic
    (infix-defn check-rate-limit [user-id rate-limits requests-per-minute]
      (let [current-requests (get rate-limits user-id 0)
            limit-exceeded? (current-requests >= requests-per-minute)]
        {:allowed (not limit-exceeded?)
         :requests-remaining (max 0 (requests-per-minute - current-requests))
         :reset-in-seconds 60}))
    
    ;; Test authentication
    (let [users [{:username "john" :password "secret123" :active true}
                 {:username "jane" :password "pass456" :active false}]
          
          valid-auth (authenticate-user "john" "secret123" users)
          invalid-pass (authenticate-user "john" "wrong" users)
          inactive-user (authenticate-user "jane" "pass456" users)]
      
      (is (true? (:success valid-auth)))
      (is (= "john" (get-in valid-auth [:user :username])))
      (is (false? (:success invalid-pass)))
      (is (= "Invalid password" (:error invalid-pass)))
      (is (false? (:success inactive-user)))
      (is (= "Account disabled" (:error inactive-user))))
    
    ;; Test rate limiting
    (let [rate-limit (check-rate-limit "user123" {"user123" 45} 50)]
      (is (true? (:allowed rate-limit)))
      (is (= 5 (:requests-remaining rate-limit))))
    
    (let [rate-limit (check-rate-limit "user456" {"user456" 55} 50)]
      (is (false? (:allowed rate-limit)))
      (is (= 0 (:requests-remaining rate-limit))))))

;; =============================================================================
;; OBJECT-ORIENTED INTEROP - JAVA INTEGRATION  
;; =============================================================================

(deftest oop-interop-readability-test
  (testing "Object-oriented interop is clean and readable"
    
    ;; String processing with readable method chains
    (infix-defn process-text [text]
      (text -> .trim() -> .toUpperCase() -> (.replaceAll "\\s+" " ")))
    
    (is (= "HELLO WORLD" (process-text "  hello    world  ")))
    
    ;; StringBuilder operations - compare traditional vs infix
    (defn build-message-traditional [name items]
      (-> (StringBuilder. "Hello ")
          (.append name)
          (.append ", you have ")
          (.append (count items))
          (.append " items")
          (.toString)))
    
    (infix-defn build-message-infix [name items]
      (StringBuilder("Hello ") 
       -> .append(name) 
       -> .append(", you have ") 
       -> .append(count(items)) 
       -> .append(" items")
       -> .toString()))
    
    ;; Both produce identical results
    (let [items ["a" "b" "c"]]
      (is (= "Hello John, you have 3 items" (build-message-traditional "John" items)))
      (is (= "Hello John, you have 3 items" (build-message-infix "John" items))))
    
    ;; Collection operations with object creation
    (infix-defn create-and-populate-list [& items]
      (ArrayList() -> (.addAll (seq items)) -> .size()))
    
    ;; This doesn't work as expected due to -> semantics, so let's fix it
    (infix-defn create-list-size [& items]
      (let [list (ArrayList())]
        (.addAll list (seq items))
        (.size list)))
    
    (is (= 3 (create-list-size "a" "b" "c")))))

(run-tests)