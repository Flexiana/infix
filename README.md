# Infix — Readable Math and Dataflow Syntax for Clojure

> "A massive heresy that makes Clojure more readable for normal humans."

**Infix** adds optional infix-style expressions with native Clojure threading macros, arithmetic, comparisons, and boolean logic — without leaving Lisp semantics.  

Transform this readable syntax:

```clojure
(infix {:users users} 
       -> :users 
       ->> (filter #(> (:age %) 25))
       ->> (map :name)
       ->> (take 10))
```

Into standard Clojure:

```clojure
(->> (-> {:users users} :users)
     (filter #(> (:age %) 25)) 
     (map :name)
     (take 10))
```

Perfect for data transformations, business logic, and mathematical expressions.

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
com.yourorg/infix {:mvn/version "0.1.0"}
```

or via Leiningen:

```clojure
[com.yourorg/infix "0.1.0"]
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

| Category | Operators |
|-----------|------------|
| Arithmetic | `+ - * /` |
| Comparison | `= not= < <= > >=` |
| Boolean | `and or not` |
| Threading | `-> ->> some-> some->>` |

Grouping with normal parentheses works:

```clojure
(infix (a * (b + c)) / d)
```

---

### 2. Native Threading Macros

Use Clojure's threading macros directly as infix operators for powerful data transformations:

#### Thread-First (`->`)
```clojure
(infix {:a 1 :b 2} -> (assoc :c 3) -> (get :c))
;; => (-> {:a 1 :b 2} (assoc :c 3) (get :c))
;; => 3
```

#### Thread-Last (`->>`)
```clojure
(infix [1 2 3 4 5] ->> (filter even?) ->> (map #(* % 2)))
;; => (->> [1 2 3 4 5] (filter even?) (map #(* % 2)))
;; => (4 8)
```

#### Nil-Safe Threading (`some->`, `some->>`)
```clojure
(infix {:user {:name "john"}} some-> :user some-> :name some-> .toUpperCase)
;; => "JOHN"

(infix {:user nil} some-> :user some-> :name)  
;; => nil
```

#### Mixed Threading
```clojure
(infix {:data [1 2 3 4 5]} -> :data ->> (filter odd?) ->> (reduce +))
;; => (->> (-> {:data [1 2 3 4 5]} :data) (filter odd?) (reduce +))
;; => 9
```

---

### 3. Combining Threading with Other Operators

Threading operators work seamlessly with arithmetic, comparisons, and boolean logic:

```clojure
;; Threading with arithmetic
(infix ([1 2 3] ->> count) + 5)
;; => (+ (count [1 2 3]) 5) => 8

;; Threading with comparisons  
(infix ([1 2 3 4 5] ->> count) > 3)
;; => (> (count [1 2 3 4 5]) 3) => true

;; Threading with boolean logic
(infix ([1 2] ->> empty?) or ([3 4] ->> empty?))
;; => (or (empty? [1 2]) (empty? [3 4])) => false
```

Complex business logic becomes readable:

```clojure
(infix {:transactions txns}
       -> :transactions
       ->> (filter #(= :debit (:type %)))
       ->> (map :amount)
       ->> (reduce +)
       -> (> 1000))
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

;; Threading (unchanged)
(infix data -> :key ->> (map inc))  ; Threading pipeline
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
  (when (< age 0) (return "Invalid: negative age"))
  (when (> age 150) (return "Invalid: too old")) 
  (when (< age 18) (return "Minor"))
  "Adult")

;; Multiple return points
(infix-defn categorize-number [n]
  (when (< n 0) (return "negative"))
  (when (= n 0) (return "zero"))
  (when (<= n 10) (return "small positive"))
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

---

## 🧪 Examples

### Mathematical Expressions

```clojure
(infix a * b + c / d)
;; => (+ (* a b) (/ c d))

(infix (x + y) * (z - w))
;; => (* (+ x y) (- z w))
```

### Data Transformation Pipeline

```clojure
(infix users
       ->> (filter #(> (:age %) 18))
       ->> (map :email)  
       ->> (take 10)
       ->> set)
```

### Business Logic

```clojure
(defn calculate-discount [order]
  (infix (:total order)
         -> (* 0.1)  ; 10% discount
         -> (max 5)  ; minimum $5 discount
         -> (min 100))) ; maximum $100 discount

(defn active-premium-users [users]
  (infix users
         ->> (filter :active?)
         ->> (filter :premium?)
         ->> (map :name)
         ->> sort))
```

### Complex Conditional Logic

```clojure
(defn categorize-transaction [txn]
  (let [amount (:amount txn)
        category (:category txn)]
    (infix amount > 1000 and category = :business)))
```

---

## ⚖️ Design Philosophy

1. **Opt-in readability:** Clojure stays pure; infix is a macro layer.
2. **No string parsing:** all syntax is real data (lists and symbols).
3. **Functional parity:** everything compiles to normal Clojure forms.
4. **Composable:** `infix`, `infix-let`, and `infix-defn` can nest freely.
5. **Safe early returns:** no hidden mutations or dynamic vars.
6. **Gradual expansion:** new tokens can be added as tables, not syntax hacks.

---

## 🧰 Current Status

**✅ v0.1 - Core Infix Operations**
- ✅ Arithmetic operators: `+ - * /`
- ✅ Comparison operators: `= not= < <= > >=`  
- ✅ Boolean operators: `and or not`
- ✅ Direct threading macros: `-> ->> some-> some->>`
- ✅ Proper operator precedence with Shunting Yard algorithm
- ✅ Function call integration
- ✅ Comprehensive test suite (100+ passing tests)

**✅ v0.2 - Arrow Lambdas**
- ✅ Arrow lambda syntax: `x => x * 2`
- ✅ Multi-parameter lambdas: `(x y) => x + y`
- ✅ Complex expressions: `x => x * x + 1`
- ✅ Smart disambiguation from threading operators

**✅ v0.3 - Function Definitions**
- ✅ `infix-defn` macro for function definitions with infix bodies
- ✅ Support for docstrings: `(infix-defn name "doc" [params] body)`
- ✅ Complex expressions with threading, comparisons, and arithmetic
- ✅ Early return mechanism with `return` statement
- ✅ Multiple return points and guard clause patterns
- ✅ Full integration with all existing infix features

**✅ v0.4 - Function Call Syntax**
- ✅ Function call syntax: `fn(args)` transforms to `(fn args)`
- ✅ Method calls: `.method("arg")` and `.method(obj, arg)`
- ✅ Integration with infix operators: `max(3, 5) + min(1, 2)`
- ✅ Threading support: `obj -> .method() -> .otherMethod()`
- ✅ Full compatibility with existing infix features

**🔄 Future Roadmap**
- `v0.5`: Collection operators (`in`, `not-in`), helper functions
- `v0.6`: Enhanced comma handling, nested function calls
- `v0.7`: Advanced features, better error messages

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

user=> (infix .toUpperCase("hello"))
"HELLO"
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
