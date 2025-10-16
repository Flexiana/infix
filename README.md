# Infix — Readable Math and Dataflow Syntax for Clojure

> “A massive heresy that makes Clojure more readable for normal humans.”

**Infix** adds optional infix-style expressions, `|>` pipelines, arrow lambdas, and early-returning functions to Clojure — without leaving Lisp semantics.  
It lets you write this:

```clojure
(infix-let
  [raw     (slurp "file.txt")
   lines   (split raw #"
")
   words   (lines |> map(fn(s) split(s, " ")) |> mapcat identity)
   words   (words |> map(trim))
   unique  (distinct words)
   sorted  (sort unique)
   avg     ((map count unique |> reduce + 0) / (count unique))]
  {:unique unique
   :sorted sorted
   :avg    avg})
```

and get this:

```clojure
(let [raw (slurp "file.txt")
      lines (clojure.string/split raw #"
")
      words (->> lines
                 (map (fn [s] (clojure.string/split s " ")))
                 (mapcat identity)
                 (map clojure.string/trim))
      unique (distinct words)
      sorted (sort unique)
      avg (/ (reduce + 0 (map count unique)) (count unique))]
  {:unique unique :sorted sorted :avg avg})
```

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
| Pipeline | `|>` → expands to `->>` |

Grouping with normal parentheses works:

```clojure
(infix (a * (b + c)) / d)
```

---

### 2. Pipelines (`|>`)

`|>` threads its left-hand value as the **last argument** (`->>`).  
It reads left-to-right like a shell or F# pipeline.

```clojure
(infix xs |> map(f) |> filter(p) |> take 10)
;; => (->> xs (map f) (filter p) (take 10))
```

You can mix with lambdas:

```clojure
(infix data |> filter(x -> x.age > 30) |> map(x -> x.name))
```

---

### 3. `infix-let`

Bindings written with `=` and right-hand infix expressions.

```clojure
(infix-let
  [a (1 + 2*3)
   b (max(a, 5))
   c (a + b)]
  (+ a b c))
```

Equivalent to:

```clojure
(let [a (+ 1 (* 2 3))
      b (max a 5)
      c (+ a b)]
  (+ a b c))
```

---

### 4. Arrow lambdas and `fn(...)`

Two readable ways to define inline functions.

```clojure
(map(x -> x*x, xs))
;; => (map (fn [x] (* x x)) xs)

(filter(fn(o) o.amount > 0, orders))
;; => (filter (fn [o] (> (:amount o) 0)) orders)
```

---

### 5. Early-returning functions (`infix-defn`)

`(return expr)` jumps out of the current `infix-defn` immediately.  
The macro wraps the body in a non-local exit (using an internal `Return` type).

```clojure
(infix-defn score [a b]
  (if a <= 0 then (return b) else 0) + (a * b))
```

Equivalent to:

```clojure
(defn score [a b]
  (try
    (if (<= a 0) (throw (Return. b)) 0)
    (+ (* a b))
    (catch Return r (.v r))))
```

---

### 6. Function calls and commas

Calls read like normal programming languages:

```clojure
(infix max(a, b))
(infix split(str, #"\s+"))
(infix reduce(+, 0, xs))
```

---

### 7. “By” helpers

Read naturally for ranking and sorting.

```clojure
(infix max-by(count, words))
;; => (apply max-key count words)

(infix sort-by(score, desc, items))
;; => (sort-by :score > items)
```

---

### 8. Membership tests

Readable containment checks.

```clojure
(infix :vip in user.tags and user.id not-in blocked)
;; => (and (contains? (:tags user) :vip)
;;         (not (contains? blocked (:id user))))
```

---

### 9. Indexing and slices *(optional sugar)*

```clojure
(infix xs[2])
;; => (nth xs 2)

(infix xs[1..4])
;; => (subvec xs 1 4)
```

---

### 10. Piecewise / guards *(planned)*

Future syntax for tariff-like formulas:

```clojure
(infix case(
  x < 10     -> 1.0*x,
  x < 100    -> 10 + 0.8*(x - 10),
  otherwise  -> 82 + 0.5*(x - 100)
))
```

→ expands to a `cond`.

---

## 🧩 Macros Summary

| Macro | Purpose |
|--------|----------|
| `infix` | One expression, list-based infix form. |
| `infix-let` | Sequential bindings with infix RHS and `=`. |
| `infix-defn` | Function with infix body and `return`. |
| `return` | Non-local early exit inside `infix-defn`. |

---

## 🧪 Examples

### Math

```clojure
(infix-let [(r = 2)
            (area = PI * r * r)]
  (println "Area:" area))
```

### Data pipeline

```clojure
(infix-defn top-names [path n]
  data = slurp(path)
  lines = split(data, #"\n")
  names = lines |> map(fn(s) first(split(s, ",")))
  freq  = frequencies(names)
  sorted = sort-by(val, desc, freq)
  take(n, sorted))
```

### Early return

```clojure
(infix-defn safe-div [x y]
  when y = 0 then (return nil)
  x / y)
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

## 🧰 Roadmap

| Version | Features |
|----------|-----------|
| `v0.1` | `infix`, `infix-let`, precedence parser, `|>`, `and/or/not`, arithmetic, comparisons. |
| `v0.2` | Arrow lambdas (`x -> ...`), `fn(...) ...`, `return`, `infix-defn`. |
| `v0.3` | `max-by`, `sort-by`, `in`, `not-in`, slices, `case(...)`. |
| `v0.4` | Pretty error messages, source maps, macroexpansion debugging. |
| `v1.0` | Stable DSL + optional static checking for formulas. |

---

## ⚡️ Example Session

```clojure
user=> (require '[infix.core :refer :all])
user=> (macroexpand-1 '(infix 1 + 2*3))
(+ 1 (* 2 3))

user=> (infix-let [(a = 10) (b = 5) (c = a*b + a/2)] (+ a b c))
70.0

user=> (infix-defn f [x]
         when x < 0 then (return :negative)
         x * x)
user=> (map f [-3 0 2])
(:negative 0 4)
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
