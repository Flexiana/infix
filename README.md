# Infix — Readable Math and Data Processing for Clojure

> "Making Clojure more readable for data science, business logic, and mathematical expressions."

**Infix** is a Clojure library that adds optional infix notation for mathematical expressions, comparisons, boolean logic, and data processing pipelines — while compiling to standard Clojure code.

## Key Features

- **🧮 Mathematical Expressions**: Write `a * b + c / d` instead of `(+ (* a b) (/ c d))`
- **🔄 Data Pipelines**: Use `->` and `->>` as infix operators for readable transformations
- **⚡ Arrow Lambdas**: Clean `x => x * 2` syntax for anonymous functions  
- **🔧 Function Definitions**: `infix-defn` for functions with infix bodies
- **🎯 Early Returns**: Guard clause patterns with `return` statement
- **📞 Function Calls**: Familiar `fn(args)` syntax within expressions
- **🏗️ OOP Interop**: Java method chaining with `obj -> .method()` syntax
- **💯 Zero Runtime Overhead**: Everything compiles to standard Clojure forms

Transform readable code like this:

```clojure
(infix-defn calculate-discount [subtotal tier quantity]
  (let [rate (cond (tier = :premium) 0.15
                   (quantity >= 10) 0.10  
                   :else 0.05)]
    (min (subtotal * rate) 100)))
```

Into efficient Clojure that runs at full speed.

---

## ✨ Goals

- Keep **Clojure semantics** — compile to normal Clojure forms.
- Make **math and pipelines readable** without prefix clutter.
- Allow **gradual adoption**: you can mix infix and regular Clojure.
- Serve as **domain language** for data transformations, analytics, pricing, and ETL pipelines.

---

## ⚙️ Installation

Once published:

```clojure
;; deps.edn
com.github.jiriknesl/infix {:mvn/version "1.0-rc1"}
```

or via Leiningen:

```clojure
[com.github.jiriknesl/infix "1.0-rc1"]
```

---

## 🧠 Core Concepts

### 1. Infix expressions

Use `(infix …)` to write math, comparisons, and logic in natural order.

```clojure
(infix a * b + c / d)
;; => (+ (* a b) (/ c d))

(infix x < y and y <= z)
;; => (and (< x y) (<= y z))

(infix not done or a = b)
;; => (or (not done) (= a b))
```

Supported operators:

| Category | Operators | Precedence |
|-----------|------------|------------|
| Threading | `-> ->> some-> some->>` | Lowest (0.05) |
| Boolean | `and or not` | Low (0.1-0.8) |
| Comparison | `= not= < <= > >=` | Medium (0.5) |
| Arithmetic | `+ - * /` | High (1-2) |

Grouping with normal parentheses works:

```clojure
(infix (a * (b + c)) / d)
```

---

### 2. Threading Macros as Infix Operators

The infix library treats Clojure's threading macros (`->`, `->>`, `some->`, `some->>`) as **infix operators** with the lowest precedence, enabling natural data transformation pipelines.

#### Understanding Threading Operator Precedence

Threading operators have **lowest precedence (0.05)**, meaning they bind **last**:

```clojure
;; This expression:
(infix data -> :key + 5 ->> (map inc))

;; Is parsed as:
(infix (data -> :key + 5) ->> (map inc))

;; Not as:
(infix data -> (:key + 5) ->> (map inc))
```

This design allows mathematical expressions to be computed **before** threading:

```clojure
;; Calculate first, then thread
(infix [1 2 3] ->> count * 2 + 1)
;; => (+ (* (count [1 2 3]) 2) 1) => 7

;; Comparison result gets threaded  
(infix 5 > 3 -> (if true "yes" "no"))
;; => (if (> 5 3) "yes" "no") => "yes"
```

#### Thread-First (`->`) vs Thread-Last (`->>`)

**Thread-First (`->`)**: Insert result as **first argument**
```clojure
(infix {:a 1 :b 2} -> (assoc :c 3) -> (get :c))
;; => (-> {:a 1 :b 2} (assoc :c 3) (get :c))
;; => (get (assoc {:a 1 :b 2} :c 3) :c) => 3

;; Perfect for data access chains
(infix user -> :profile -> :address -> :city)
;; => (-> user :profile :address :city)
```

**Thread-Last (`->>`)**: Insert result as **last argument**
```clojure
(infix [1 2 3 4 5] ->> (filter even?) ->> (map #(* % 2)))
;; => (->> [1 2 3 4 5] (filter even?) (map #(* % 2)))
;; => (map #(* % 2) (filter even? [1 2 3 4 5])) => (4 8)

;; Perfect for collection transformations
(infix data ->> (map transform) ->> (filter valid?) ->> (take 10))
```

#### Nil-Safe Threading (`some->`, `some->>`)

Handle `nil` values gracefully in pipelines:

```clojure
;; Returns nil if any step returns nil
(infix {:user {:name "john"}} some-> :user some-> :name some-> .toUpperCase)
;; => "JOHN"

(infix {:user nil} some-> :user some-> :name some-> .toUpperCase)  
;; => nil (stops at :user step)

;; Useful for deep data access
(infix request some-> :params some-> :user-id some-> parse-int)
```

#### Mixed Threading Patterns

Combine `->` and `->>` in the same expression:

```clojure
;; Access data first, then transform collection
(infix {:data [1 2 3 4 5]} -> :data ->> (filter odd?) ->> (reduce +))
;; => (->> (-> {:data [1 2 3 4 5]} :data) (filter odd?) (reduce +)) => 9

;; Build data structure, then process
(infix (range 10) ->> (map #(* % %)) -> vec -> (nth 3))
;; => (nth (vec (map #(* % %) (range 10))) 3) => 9
```

#### Threading with All Other Operators

Threading works seamlessly with arithmetic, comparisons, and boolean logic:

```clojure
;; Threading with arithmetic (threading happens first due to precedence)
(infix ([1 2 3] ->> count) + 5)
;; => (+ (count [1 2 3]) 5) => 8

;; Threading with comparisons  
(infix ([1 2 3 4 5] ->> count) > 3)
;; => (> (count [1 2 3 4 5]) 3) => true

;; Threading with boolean logic
(infix ([1 2] ->> empty?) or ([3 4] ->> empty?))
;; => (or (empty? [1 2]) (empty? [3 4])) => false

;; Complex business logic with threading
(infix {:transactions txns}
       -> :transactions
       ->> (filter #((:type %) = :debit))
       ->> (map :amount)
       ->> (reduce +)
       -> (> 1000))
;; Check if total debits exceed $1000
```

#### Advanced Threading Examples

```clojure
;; ETL pipeline with multiple threading operators
(infix raw-data
       -> :payload               ; Extract payload
       ->> (map parse-record)    ; Transform each record  
       ->> (filter valid?)       ; Filter valid records
       -> (group-by :category)   ; Group by category
       -> vals                   ; Get category groups
       ->> (map count)           ; Count each group  
       ->> (reduce max))         ; Find largest group

;; Error-safe data processing
(infix user-input
       some-> .trim()            ; Safely trim if not nil
       some-> parse-json         ; Parse JSON if string exists
       some-> :data              ; Extract data if parse succeeded
       some->> (map process)     ; Process items if data is collection
       some->> (take 10))        ; Take first 10 if processing succeeded
```

**Key Insight**: Threading operators in infix notation make data transformation pipelines read **left-to-right** like natural language, while maintaining all the power of Clojure's threading macros.

---

### 3. Operator Precedence Rules

Understanding precedence ensures expressions work as expected:

**Precedence Order (High to Low):**
1. **Arithmetic** (`* /`) - Highest precedence (2)
2. **Arithmetic** (`+ -`) - High precedence (1)  
3. **Comparison** (`< <= > >= = not=`) - Medium precedence (0.5)
4. **Boolean** (`and`) - Low precedence (0.2)
5. **Boolean** (`or`) - Lower precedence (0.1)
6. **Threading** (`-> ->> some-> some->>`) - Lowest precedence (0.05)

**Precedence Examples:**
```clojure
;; Arithmetic first, then comparison, then boolean
(infix a + b * c > d and e < f)
;; => (and (> (+ a (* b c)) d) (< e f))

;; Threading happens last
(infix [1 2 3] ->> count + 5 > 10)  
;; => (> (+ (count [1 2 3]) 5) 10)

;; Use parentheses to override precedence
(infix (a + b) * (c + d))
;; => (* (+ a b) (+ c d))
```

**Left-to-Right Evaluation:**
All operators are **left-associative**, so they evaluate left-to-right:

```clojure
(infix a - b - c)      ; => (- (- a b) c)
(infix a -> f -> g)    ; => (-> (-> a f) g)  
(infix a and b and c)  ; => (and (and a b) c)
```

---

### 4. Arrow Lambdas

Use `=>` syntax for clean, readable anonymous functions:

#### Single Parameter
```clojure
(map (infix x => x * x) [1 2 3])
;; => [1 4 9]

(filter (infix x => x > 5) [1 5 10 15])
;; => (10 15)
```

#### Multi-Parameter
```clojure
(reduce (infix (acc x) => acc + x) 0 [1 2 3 4])
;; => 10

((infix (x y) => (x + y) * 2) 3 4)
;; => 14
```

#### Complex Expressions
```clojure
(map (infix x => x * x + 2 * x + 1) [1 2 3])
;; => [4 9 16]  ; (x+1)²

;; Mixed with threading
(map (infix x => x -> str -> clojure.string/upper-case) [1 2 3])
;; => ["1" "2" "3"]
```

#### Perfect Disambiguation
Arrow lambdas (`=>`) are completely separate from threading (`->`):

```clojure
;; Arrow lambda
(map (infix x => x * 2) [1 2 3])  ; => [2 4 6]

;; Threading
(infix data -> :key ->> (map inc))
```

---

### 5. Function Definitions with `infix-defn`

Define functions with infix expressions in their bodies:

#### Basic Functions
```clojure
(infix-defn square [x] x * x)
(square 4)  ; => 16

(infix-defn add-multiply [x y z] x + y * z)
(add-multiply 1 2 5)  ; => 11  (1 + (2 * 5))
```

#### Functions with Comparisons and Logic
```clojure
(infix-defn in-range? [x min-val max-val] 
  x >= min-val and x <= max-val)
(in-range? 5 1 10)  ; => true

(infix-defn greater-than-ten? [x] x > 10)
(greater-than-ten? 15)  ; => true
```

#### Functions with Threading Operations
```clojure
(infix-defn process-users [users]
  users 
  ->> (filter :active?)
  ->> (map :name)
  ->> (take 10)
  ->> sort)

(infix-defn calculate-discount [order]
  (:total order)
  -> (* 0.1)        ; 10% discount
  -> (max 5)        ; minimum $5 discount  
  -> (min 100))     ; maximum $100 discount
```

#### Functions with Docstrings
```clojure
(infix-defn circle-area
  "Calculate the area of a circle given radius"
  [radius]
  3.14159 * radius * radius)
```

#### Complex Mathematical Functions
```clojure
(infix-defn quadratic [a b c x]
  a * x * x + b * x + c)

(quadratic 1 2 3 1)  ; => 6  (1*1*1 + 2*1 + 3)
```

#### Early Returns with `return`

Use the `return` statement for early exits from functions:

```clojure
(infix-defn safe-divide [x y]
  (when (= y 0) (return nil))
  (/ x y))

(safe-divide 10 0)   ; => nil
(safe-divide 10 2)   ; => 5

;; Guard clauses pattern
(infix-defn validate-age [age]
  (when (age < 0) (return "Invalid: negative age"))
  (when (age > 150) (return "Invalid: too old")) 
  (when (age < 18) (return "Minor"))
  "Adult")

;; Multiple return points
(infix-defn categorize-number [n]
  (when (n < 0) (return "negative"))
  (when (n = 0) (return "zero"))
  (when (n <= 10) (return "small positive"))
  "large positive")
```

**Note:** Early returns work by throwing and catching special exceptions internally, providing clean non-local exit semantics without affecting normal exception handling.

---

### 6. Function Call Syntax

Use familiar `fn(args)` syntax within infix expressions:

#### Basic Function Calls
```clojure
(infix max(3, 5))           ; => 5
(infix min(1, 2))           ; => 1
(infix count("hello"))      ; => 5
(infix Math/sqrt(9))        ; => 3.0
```

#### Function Calls with Infix Operators
```clojure
(infix max(3, 5) + min(1, 2))        ; => 6  (5 + 1)
(infix Math/sqrt(9) * 2)             ; => 6.0
(infix count("hello") > 3)           ; => true
```

#### Method Calls
```clojure
(infix .toUpperCase("hello"))        ; => "HELLO"
(infix .length("hello"))             ; => 5
(infix .substring("hello", 0, 2))    ; => "he"
```

#### Function Calls with Threading
```clojure
(infix "hello" -> .toUpperCase() -> .length())  ; => 5
(infix [1 2 3] ->> (map(#(* % 2))) ->> vec())   ; => [2 4 6]
```

#### In Function Definitions
```clojure
(infix-defn distance [x1 y1 x2 y2]
  (infix Math/sqrt(Math/pow(x2 - x1, 2) + Math/pow(y2 - y1, 2))))

(infix-defn string-processor [s]
  (infix .toUpperCase(.trim(s))))
```

**Note:** Function call syntax transforms `fn(args)` to standard Clojure `(fn args)` during parsing, maintaining full compatibility while providing familiar syntax.

It is required not to use any whitespace between the function name and the opening parenthesis. For example, `Math/sqrt(9)` is valid, but `Math/sqrt (9)` is not.

---

### 7. OOP Interop and Method Calls

Seamless Java interop with familiar object-oriented syntax:

#### Method Chaining with Threading
```clojure
;; Java-style method chaining using -> operator
(infix "hello" -> .toUpperCase() -> (.substring 0 3))  ; => "HEL"

(infix obj -> (.setName "John") -> (.setSurname "Newman") -> .save())
;; => (-> obj (.setName "John") (.setSurname "Newman") (.save))

;; StringBuilder example
(infix (StringBuilder. "Hello") 
       -> (.append " ") 
       -> (.append "World") 
       -> .toString())  ; => "Hello World"
```

#### OOP in Function Definitions
```clojure
(infix-defn clean-text [text]
  (text -> .trim() -> .toLowerCase()))

(infix-defn build-greeting [name]
  (StringBuilder. "Hello, ") 
  -> (.append name) 
  -> (.append "!") 
  -> .toString()))

(infix-defn string-length-plus [s n]
  ((.length s) + n))
```

#### OOP with Infix Operators
```clojure
;; Method results in mathematical expressions
(infix (.length "hello") + 5)           ; => 10
(infix (.getAge user) > 18)             ; => true/false  
(infix (.isValid obj) and (.isActive obj))  ; => boolean result
```

**Note:** The infix library supports Java interop through Clojure's standard method call syntax (`.method`) combined with threading operators for elegant method chaining.

---

## 🧪 Complete Examples

### 1. Mathematical Expressions

```clojure
;; Simple arithmetic with proper precedence
(infix a * b + c / d)
;; => (+ (* a b) (/ c d))

;; Complex mathematical formulas
(infix (x + y) * (z - w))
;; => (* (+ x y) (- z w))

;; Function calls in math expressions
(infix Math/sqrt(a * a + b * b) + c)
;; => (+ (Math/sqrt (+ (* a a) (* b b))) c)
```

### 2. Data Processing Pipelines

```clojure
;; Thread-last for collection transformations
(infix users
       ->> (filter #((:age %) > 18))
       ->> (map :email)  
       ->> (take 10)
       ->> set)
;; => (set (take 10 (map :email (filter #(> (:age %) 18) users))))

;; Thread-first for data access and manipulation
(infix {:data [1 2 3 4 5]} 
       -> :data 
       ->> (filter odd?) 
       ->> (reduce +))
;; => (reduce + (filter odd? (:data {:data [1 2 3 4 5]})))
```

### 3. Comprehensive Business Logic

Combining all features in realistic business functions:

```clojure
;; Complete order processing with all infix features
(infix-defn process-order [order customer-tier]
  ;; Early validation with guard clauses
  (when (not (:items order)) (return {:error "No items"}))
  (when ((:total order) <= 0) (return {:error "Invalid total"}))
  
  ;; Calculate discount using comparisons and math
  (let [base-discount (cond (customer-tier = :premium) 0.15
                           ((:total order) >= 100) 0.10
                           :else 0.05)
        
        ;; Function calls with arithmetic  
        discount-amount (min((:total order) * base-discount, 50))
        
        ;; String processing with method chaining
        customer-name ((:customer-name order) 
                      -> .trim() 
                      -> .toLowerCase())
        
        ;; Complex pipeline with threading
        processed-items ((:items order)
                        ->> (filter #((:price %) > 0))
                        ->> (map #(assoc % :discounted-price 
                                    ((:price %) * (1 - base-discount))))
                        ->> (sort-by :discounted-price)
                        ->> reverse)]
    
    ;; Return comprehensive result
    {:customer customer-name
     :discount-rate base-discount
     :discount-amount discount-amount  
     :final-total ((:total order) - discount-amount)
     :items processed-items
     :valid true}))

;; Usage example
(let [order {:total 120
             :customer-name "  JOHN DOE  "
             :items [{:name "Widget A" :price 50}
                     {:name "Widget B" :price 70}]}
      result (process-order order :premium)]
  (:final-total result))  ; => 102.0 (120 - 18 discount)
```

### 4. Advanced Data Analytics  

```clojure
;; Statistical calculations with arrow lambdas
(infix-defn analyze-sales-data [sales]
  (let [;; Filter and transform with threading
        valid-sales (sales 
                    ->> (filter #((:amount %) > 0))
                    ->> (filter #((:date %) != nil)))
        
        ;; Mathematical aggregations with function calls
        total-revenue (valid-sales 
                      ->> (map :amount) 
                      ->> (reduce +))
        
        average-sale (total-revenue / count(valid-sales))
        
        ;; Complex filtering with lambda functions
        high-value-sales (valid-sales 
                         ->> (filter (infix sale => (:amount sale) > average-sale))
                         ->> count)
        
        ;; String manipulation and formatting
        summary-text (StringBuilder("Sales Analysis: ")
                     -> (.append count(valid-sales))
                     -> (.append " sales, $")  
                     -> (.append total-revenue)
                     -> (.append " revenue")
                     -> .toString())]
    
    {:total-sales count(valid-sales)
     :total-revenue total-revenue
     :average-sale average-sale
     :high-value-count high-value-sales
     :high-value-percentage (high-value-sales * 100.0 / count(valid-sales))
     :summary summary-text}))
```

### 5. Complex Conditional Logic with All Features

```clojure
;; Risk assessment combining everything
(infix-defn assess-transaction-risk [transaction user]
  ;; Guard clauses with early returns
  (when (not transaction) (return {:risk :high :reason "Missing transaction"}))
  (when (not user) (return {:risk :high :reason "Missing user"}))
  
  (let [;; Mathematical risk scoring
        amount-risk (cond ((:amount transaction) > 10000) 0.8
                         ((:amount transaction) > 1000) 0.4  
                         :else 0.1)
        
        ;; Time-based calculations with method chaining
        hours-since ((:timestamp transaction)
                    -> .getTime()
                    -> (- (System/currentTimeMillis))
                    -> (/ 3600000))  ; Convert to hours
        
        time-risk (cond (hours-since < 1) 0.6    ; Very recent
                       (hours-since > 24) 0.3    ; Older transaction  
                       :else 0.2)
        
        ;; User history analysis with pipelines
        user-score ((:transaction-history user)
                   ->> (filter #((:status %) = :completed))
                   ->> (map :amount)
                   ->> (filter (infix amt => amt > 0))
                   ->> count
                   -> (max 1)              ; Avoid division by zero
                   -> (min 100)            ; Cap the score
                   -> (/ 100.0))           ; Normalize to 0-1
        
        ;; Geographic risk with string operations
        location-risk (let [country ((:location user) -> .toLowerCase())]
                       (cond (country = "us") 0.1
                             (country = "ca") 0.1  
                             :else 0.3))
        
        ;; Combined risk calculation
        total-risk (amount-risk + time-risk + location-risk - user-score)
        normalized-risk (max(0, min(1, total-risk)))
        
        ;; Risk categorization
        risk-level (cond (normalized-risk >= 0.7) :high
                        (normalized-risk >= 0.4) :medium
                        :else :low)]
    
    {:risk risk-level
     :score normalized-risk
     :factors {:amount amount-risk
               :timing time-risk
               :location location-risk
               :user-history user-score}
     :recommendation (cond (risk-level = :high) "Block transaction"
                          (risk-level = :medium) "Require additional verification"
                          :else "Approve transaction")}))
```

This example demonstrates:
- **Mathematical expressions**: `amount-risk` calculations with proper precedence  
- **Function calls**: `count(valid-sales)`, `min(1, total-risk)`
- **Threading pipelines**: `->` for data access, `->>` for collection processing
- **Method chaining**: `.getTime()` and `.toLowerCase()` on Java objects
- **Arrow lambdas**: `(infix amt => amt > 0)` for filtering
- **Early returns**: Guard clauses with `return` statements
- **Complex comparisons**: Multi-condition `cond` statements with infix operators
- **String building**: `StringBuilder` with method chaining

---

## ⚖️ Design Philosophy

1. **Opt-in readability:** Clojure stays pure; infix is a macro layer.
2. **No string parsing:** all syntax is real data (lists and symbols).
3. **Functional parity:** everything compiles to normal Clojure forms.
4. **Composable:** `infix`, `infix-let`, and `infix-defn` can nest freely.
5. **Safe early returns:** no hidden mutations or dynamic vars.
6. **Gradual expansion:** new tokens can be added as tables, not syntax hacks.

---

## 🎯 Current Status: Version 1.0-rc1

The infix library provides comprehensive infix notation support for Clojure!

**✅ Core Features**
- **Mathematical Expressions**: Full arithmetic with proper precedence (`+ - * /`)
- **Comparisons & Logic**: All comparison and boolean operators (`= < > and or not`)
- **Threading Integration**: Native support for `-> ->> some-> some->>`  
- **Arrow Lambdas**: Clean `x => expr` syntax with perfect disambiguation
- **Function Definitions**: `infix-defn` with infix expressions in function bodies
- **Early Returns**: Guard clause patterns with `return` statement
- **Function Call Syntax**: Familiar `fn(args)` notation within expressions
- **Java Interop**: Method chaining with `obj -> .method()` syntax
- **Zero Overhead**: Everything compiles to standard Clojure forms

**✅ Production Ready**
- Comprehensive test coverage for all features
- Clean, maintainable codebase with proper separation of concerns
- Proper error handling and edge case management
- Full compatibility with existing Clojure code
- Extensible design for future enhancements

**🔮 Future Enhancements** 
- Enhanced error messages with better syntax hints
- Collection operators (`in`, `not-in`) for membership testing
- Advanced pattern matching integration
- Performance optimizations for complex expressions

---

## ⚡️ Example Session

```clojure
user=> (require '[infix.core :refer :all])

user=> (macroexpand-1 '(infix 1 + 2 * 3))
(+ 1 (* 2 3))

user=> (infix 1 + 2 * 3)
7

user=> (infix [1 2 3 4 5] ->> (filter even?) ->> (map #(* % 2)) ->> vec)
[4 8]

user=> (infix {:user {:name "john"}} some-> :user some-> :name some-> .toUpperCase)
"JOHN"

user=> (infix 5 > 3 and 2 < 4)
true

user=> (map (infix x => x * x) [1 2 3 4])
(1 4 9 16)

user=> ((infix (x y) => x + y * 2) 3 4)
11

user=> (infix-defn square [x] x * x)
#'user/square

user=> (square 5)
25

user=> (infix-defn process-data [items] items ->> (filter :active?) ->> count)
#'user/process-data

user=> (process-data [{:active? true} {:active? false} {:active? true}])
2

user=> (infix-defn safe-divide [x y] (when (= y 0) (return nil)) (/ x y))
#'user/safe-divide

user=> (safe-divide 10 0)
nil

user=> (safe-divide 10 2)
5

user=> (infix max(3, 5) + min(1, 2))
6

user=> (infix count("hello") > 3)
true

user=> (infix (.toUpperCase "hello"))
"HELLO"

user=> (infix "hello" -> .toUpperCase() -> (.substring 0 3))
"HEL"

user=> (infix-defn greet-user [name] 
         (name -> .toUpperCase() -> (.concat "!")))
#'user/greet-user

user=> (greet-user "alice")
"ALICE!"

user=> (infix (StringBuilder. "hello") -> (.append " world") -> .toString())
"hello world"

user=> (infix (.length "hello") + 5)
10

user=> (infix-defn string-processor [text]
         (text -> .trim() -> .toLowerCase()))
#'user/string-processor

user=> (string-processor "  HELLO WORLD  ")
"hello world"

user=> ;; Complex example combining all features
user=> (infix-defn analyze-numbers [numbers]
         (when (numbers ->> empty?) (return {:error "No data"}))
         (let [avg (numbers ->> (reduce +) -> (/ count(numbers)))
               above-avg (numbers ->> (filter (infix n => n > avg)) ->> count)]
           {:average avg
            :above-average-count above-avg  
            :percentage (above-avg * 100.0 / count(numbers))}))
#'user/analyze-numbers

user=> (analyze-numbers [1 2 3 4 5])
{:average 3.0, :above-average-count 2, :percentage 40.0}

user=> ;; Threading with arithmetic and comparisons  
user=> (infix [10 20 30] ->> count * 2 + 1 > 5)
true
```

---

## 🤓 Why

Clojure is beautiful, but for math-heavy or dataflow-heavy code, prefix notation hides intent.  
**Infix** is not a new language — it’s a *lens*. It lets engineers, analysts, or business rule authors read logic naturally while keeping full Clojure semantics under the hood.

You can use it for:
- BI and ETL transformations  
- Pricing and rules engines  
- Numeric and ML prototypes  
- Teaching Clojure to non-Lispers  

---

## 🧩 License

Eclipse Public License 2.0 (same as Clojure)

---

## 🪦 Disclaimer

This project commits Lisp heresy.  
Use it to make your Clojure more readable — or to horrify your local REPL priest.
