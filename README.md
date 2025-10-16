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

**🔄 Future Roadmap**
- `v0.3`: Enhanced function syntax, comma-separated parameters
- `v0.3`: Collection operators (`in`, `not-in`), helper functions
- `v0.4`: Advanced features, better error messages

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
