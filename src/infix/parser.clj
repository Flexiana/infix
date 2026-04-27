(ns infix.parser
  "Infix expression parser using Shunting Yard algorithm."
  (:require [infix.precedence :as prec]
            [clojure.string :as str]))

;; Constants
(def ^:private operators 
  "Set of all supported infix operators."
  #{'+ '- '* '/ '< '<= '> '>= '= 'not= 'and 'or 'not '-> '->> 'some-> 'some->>})

(def ^:private known-functions
  "Set of functions that commonly take operators as arguments."
  #{'apply 'reduce 'map 'filter 'partial 'comp})

(def ^:private special-forms
  "Set of Clojure special forms to avoid in lambda parameters."
  #{'fn 'defn 'let 'if 'when 'cond})

(def ^:private binding-forms
  "Clojure forms whose first argument is a let-style binding vector
   alternating [name value name value ...]. Recognised so that fn(args)
   transformation does not confuse a binding name with a function head."
  #{'let 'loop 'when-let 'if-let 'when-some 'if-some 'binding 'with-open})

(defn- function-call-pattern?
  "Check if a symbol could be followed by function call syntax."
  [sym]
  (and (symbol? sym)
       (not (contains? operators sym))
       (let [s (str sym)]
         ;; Valid function name: starts with letter or special chars, contains valid chars
         (re-matches #"^[a-zA-Z_\-\+\*\/\<\>\=\!\?\$\%\&\.][a-zA-Z0-9_\-\+\*\/\<\>\=\!\?\$\%\&\.]*$" s))))

(defn- oop-method-call-pattern?
  "Check if a symbol looks like obj.method for OOP method calls."
  [sym]
  (and (symbol? sym)
       (let [s (str sym)]
         ;; Contains a dot and looks like obj.method
         (and (re-find #"\." s)
              (not (re-find #"^\." s))  ; Not starting with dot (that's .method syntax)
              (re-matches #"^[a-zA-Z_][a-zA-Z0-9_]*\.[a-zA-Z_][a-zA-Z0-9_]*$" s)))))

(defn- split-oop-method-call
  "Split obj.method into [obj method]."
  [sym]
  (let [s (str sym)
        [obj-str method-str] (str/split s #"\." 2)]
    [(symbol obj-str) (symbol method-str)]))

(defn- object-creation-pattern?
  "Check if a symbol looks like ClassName.new for object creation."
  [sym]
  (and (symbol? sym)
       (let [s (str sym)]
         ;; Looks like ClassName.new where ClassName starts with uppercase
         (and (re-find #"\." s)
              (not (re-find #"^\." s))
              (re-matches #"^[A-Z][a-zA-Z0-9_]*\.new$" s)))))

(defn- split-object-creation
  "Split ClassName.new into [ClassName new]."
  [sym]
  (let [s (str sym)
        [class-str _] (str/split s #"\." 2)]
    [(symbol class-str) 'new]))

(defn- constructor-call-pattern?
  "Check if a symbol looks like a constructor call (ClassName where ClassName starts with uppercase)."
  [sym]
  (and (symbol? sym)
       (let [s (str sym)]
         ;; Constructor: starts with uppercase letter
         (re-matches #"^[A-Z][a-zA-Z0-9_]*$" s))))


(declare transform-function-calls)

(defn- transform-binding-vector
  "Transform a let-style binding vector. Even indices are binding targets
   (kept as-is or recursed for destructuring); odd indices are RHS
   expressions processed as flat infix fragments. If the vector has an
   odd count, Clojure reader probably split an fn(args) pair, so treat
   the whole vector as a flat expression to glue it back together."
  [bvec]
  (let [items (vec bvec)]
    (if (odd? (count items))
      (transform-function-calls bvec :expression)
      ;; Even count — binding names and RHSs are already aligned. Each
      ;; sequential item (destructuring LHS or expression RHS) is a Clojure
      ;; form, so recurse as :call. Non-sequential items stay as-is.
      (mapv (fn [item]
              (if (sequential? item)
                (transform-function-calls item :call)
                item))
            items))))

(defn transform-function-calls
  "Transform function call syntax by detecting symbol followed by list pattern.

   `context` tells us how to interpret the first element of `expr`:
     :expression — a flat infix/arg-list sequence; every position is eligible
                   for fn(args) pattern matching.
     :call       — the first element is the fn/macro/special-form head of a
                   Clojure call form; skip pattern matching at position 0 so
                   `cond` in `(cond (x = y) 1)` isn't treated as fn(args).
                   Additionally, non-head positions are treated as independent
                   arguments: adjacent `symbol` + `(list)` pairs are NOT
                   merged into fn-calls there (that would mis-read e.g.
                   `(.addAll list (seq items))` as `(.addAll (list seq items))`)."
  ([expr] (transform-function-calls expr :expression))
  ([expr context]
   (cond
     (sequential? expr)
     (let [items (vec expr)
           vec-input? (vector? expr)
           head (first items)
           binding-call? (and (= context :call)
                              (symbol? head)
                              (contains? binding-forms head)
                              (vector? (second items)))
           ;; When the head is an operator (e.g. `(/ count (numbers))`), the
           ;; body is a flat operand list — treat it as :expression so nested
           ;; fn(args) pairs get glued together.
           operator-headed-call? (and (= context :call)
                                      (symbol? head)
                                      (contains? operators head))
           ;; Are we currently in a position where fn-call patterns may match?
           match-patterns? (or (= context :expression) operator-headed-call?)]
       (cond
         ;; (let [binding-vec] body...) and friends: protect binding-vec layout.
         binding-call?
         (let [transformed-bvec (transform-binding-vector (second items))
               body (drop 2 items)
               transformed-body (map #(if (sequential? %)
                                        (transform-function-calls % :call)
                                        %)
                                     body)]
           (apply list head transformed-bvec transformed-body))

         :else
         (loop [i 0
                result []]
           (if (>= i (count items))
             (if vec-input? result (seq result))  ; Preserve original container type
             (let [current (items i)
                   next-item (get items (inc i))
                   head-pos? (and (= context :call) (zero? i))]
               (cond
                 ;; Head of a call form: preserve the head, recurse into it as :call.
                 head-pos?
                 (recur (inc i)
                        (conj result
                              (if (sequential? current)
                                (transform-function-calls current :call)
                                current)))

                 ;; In a context where pattern matching is allowed, try each
                 ;; fn(args) pattern. Otherwise skip directly to :else (keep
                 ;; tokens as independent arguments).
                 (and match-patterns?
                      (object-creation-pattern? current)
                      (seq? next-item))
                 (let [[class-name _] (split-object-creation current)
                       transformed-args (transform-function-calls next-item :expression)
                       constructor-call (cons 'new (cons class-name transformed-args))]
                   (recur (+ i 2) (conj result constructor-call)))

                 (and match-patterns?
                      (constructor-call-pattern? current)
                      (seq? next-item))
                 (let [transformed-args (transform-function-calls next-item :expression)
                       constructor-call (cons 'new (cons current transformed-args))]
                   (recur (+ i 2) (conj result constructor-call)))

                 (and match-patterns?
                      (oop-method-call-pattern? current)
                      (seq? next-item))
                 (let [[obj method] (split-oop-method-call current)
                       transformed-args (transform-function-calls next-item :expression)
                       method-call (cons (symbol (str "." method)) (cons obj transformed-args))]
                   (recur (+ i 2) (conj result method-call)))

                 (and match-patterns?
                      (function-call-pattern? current)
                      (seq? next-item))
                 (let [transformed-args (transform-function-calls next-item :expression)
                       fn-call (cons current transformed-args)]
                   (recur (+ i 2) (conj result fn-call)))

                 ;; Plain token: keep as-is, recursing into sub-sequences as
                 ;; :call so nested fn(args) is still detected where safe.
                 :else
                 (recur (inc i)
                        (conj result
                              (if (sequential? current)
                                (transform-function-calls current :call)
                                current)))))))))

     :else
     expr)))

(defn tokenize
  "Convert infix expression into sequence of tokens."
  [expr]
  (vec (transform-function-calls expr)))

(defn- operator?
  "Check if token is an operator."
  [token]
  (contains? operators token))

(defn- lambda-parameter?
  "Check if a token looks like a lambda parameter."
  [token]
  (and (symbol? token)
       (not (operator? token))
       (not (contains? special-forms token))))

(defn- lambda-parameters?
  "Check if a token looks like lambda parameters (symbol or parameter list)."
  [token]
  (or (lambda-parameter? token)
      ;; Parameter list like (x, y) or (x y)  
      (and (sequential? token)
           (every? lambda-parameter? token))))

(defn- is-arrow-lambda?
  "Check if expression looks like an arrow lambda: param(s) -> body"
  [tokens]
  (and (>= (count tokens) 3)
       ;; First token(s) are parameters
       (lambda-parameters? (first tokens))
       ;; Second token is ->
       (= '-> (second tokens))
       ;; Has a body after ->
       (>= (count tokens) 3)))

(defn- contains-infix-pattern?
  "Check if a sequence contains infix operator patterns (operator between two operands)."
  [tokens]
  (let [token-vec (vec tokens)]
    (loop [i 1]  ; Start from index 1 since we need to check operand-operator-operand pattern
      (if (>= i (dec (count token-vec)))
        false
        (if (and (operator? (nth token-vec i))  ; Current token is operator
                 (< (dec i) (count token-vec))  ; Has left operand
                 (< (inc i) (count token-vec))) ; Has right operand
          true  ; Found infix pattern
          (recur (inc i)))))))

(defn- known-function?
  "Check if symbol is a known function that's likely to be called with operators as arguments."
  [sym]
  (contains? known-functions sym))

(defn- is-function-call?
  "Check if a list looks like a function call.
   Uses heuristics: if it starts with a known function and contains operators,
   those operators are likely arguments, not infix. Otherwise, if it contains
   infix patterns, it's probably a grouped infix expression."
  [lst]
  (and (seq? lst) 
       (not (empty? lst))
       (symbol? (first lst))  ; Must start with a symbol
       (not (operator? (first lst)))  ; Not an operator
       (or (known-function? (first lst))  ; Known function with operators as args
           (not (contains-infix-pattern? lst)))))  ; Or no infix patterns

(defn- flatten-tokens
  "Flatten nested expressions while preserving function calls as single tokens."
  [expr]
  (reduce
   (fn [acc token]
     (cond
       ;; Function call - keep as single token
       (is-function-call? token)
       (conj acc token)
       
       ;; Infix grouping with operators - flatten with parens
       (and (seq? token) (not (empty? token)) (contains-infix-pattern? token))
       (-> acc
           (conj :lparen)
           (into (flatten-tokens token))
           (conj :rparen))
       
       ;; Simple list without operators - keep as single token (like vector literals)
       (and (seq? token) (not (empty? token)))
       (conj acc token)
       
       ;; Regular token
       :else
       (conj acc token)))
   []
   expr))

(defn- pop-until-lparen
  "Pop operators from stack until left parenthesis, returning [output stack]."
  [output stack]
  (loop [out output stk stack]
    (cond
      (empty? stk) [out stk]
      (= (peek stk) :lparen) [out (pop stk)]
      :else (recur (conj out (peek stk)) (pop stk)))))

(defn- pop-higher-precedence
  "Pop operators with higher or equal precedence, returning [output stack]."
  [output stack current-op]
  (loop [out output stk stack]
    (cond
      (empty? stk) [out stk]
      (= (peek stk) :lparen) [out stk]
      (not (operator? (peek stk))) [out stk]
      (or (> (prec/precedence (peek stk)) (prec/precedence current-op))
          (and (= (prec/precedence (peek stk)) (prec/precedence current-op))
               (prec/left-associative? current-op)))
      (recur (conj out (peek stk)) (pop stk))
      :else [out stk])))

(defn parse-infix
  "Parse infix expression into postfix notation using Shunting Yard algorithm."
  [expr]
  (let [tokens (flatten-tokens expr)]
    (loop [input tokens
           output []
           op-stack []]
      (if (empty? input)
        ;; End of input: pop remaining operators
        (into output (filter operator? (reverse op-stack)))
        (let [token (first input)]
          (cond
            ;; Left parenthesis
            (= token :lparen)
            (recur (rest input) output (conj op-stack token))
            
            ;; Right parenthesis
            (= token :rparen)
            (let [[new-output new-stack] (pop-until-lparen output op-stack)]
              (recur (rest input) new-output new-stack))
            
            ;; Operator
            (operator? token)
            (let [[new-output new-stack] (pop-higher-precedence output op-stack token)]
              (recur (rest input) new-output (conj new-stack token)))
            
            ;; Operand
            :else
            (recur (rest input) (conj output token) op-stack)))))))