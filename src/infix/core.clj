(ns infix.core
  "Main public API for the Infix syntax library."
  (:require [infix.parser :as parser]
            [infix.compiler :as compiler]
            [infix.precedence :as prec]))

(def ^:private operators 
  "Set of all supported infix operators."
  #{'+ '- '* '/ '< '<= '> '>= '= 'not= 'and 'or 'not '-> '->> 'some-> 'some->>})

(def ^:private known-functions
  "Set of functions/macros that commonly take operators as arguments."
  #{'apply 'reduce 'map 'filter 'partial 'comp 'max 'min 'count 'empty? 'str
    'infix 'infix-let 'infix-defn 'return 'when 'if 'cond 'let 'do 'fn 'defn})

(defn- operator? 
  "Check if token is an operator."
  [token]
  (contains? operators token))

(defn- known-function? 
  "Check if symbol is a known function that might take operators as arguments."
  [sym]
  (contains? known-functions sym))

(defn- is-infix-expression? 
  "Check if a list looks like an infix expression (not a function call)."
  [lst]
  (and (seq? lst) 
       (not (empty? lst))
       ;; NOT starting with an operator (that would be a function call like (+ 1 2))
       (not (operator? (first lst)))
       ;; Check if it starts with a literal value (boolean, number, etc.) followed by an operator
       ;; This handles cases like (false or true) which Clojure might try to evaluate
       (let [first-elem (first lst)
             second-elem (second lst)
             starts-with-literal (or (boolean? first-elem)
                                    (number? first-elem)
                                    (string? first-elem)
                                    (keyword? first-elem)
                                    (nil? first-elem))
             has-operator-after-literal (and starts-with-literal
                                            (operator? second-elem))]
         (or has-operator-after-literal
             ;; Has infix operators between operands (not just as first element)
             (let [operators-in-middle (some operator? (rest lst))]
               (and operators-in-middle
                    ;; And it's not a known function that might take operators as arguments
                    (not (known-function? (first lst)))))))))

(defn- grouping-wrap?
  "Detect a single-element seq that wraps a literal, a vector, or an infix
   expression — i.e. redundant parens. We deliberately do NOT peel forms
   like `((fn [] 5))` or `((foo))` because those are thunk/no-arg calls in
   standard Clojure semantics: the outer parens are an invocation, not
   grouping. Recurses through nested single-element wraps so chains like
   `(((3 + 4)))` are still recognised."
  [form]
  (and (seq? form)
       (= 1 (count form))
       (let [inner (first form)]
         (cond
           (or (number? inner) (string? inner) (boolean? inner)
               (nil? inner) (keyword? inner)) true
           (vector? inner) true
           (and (seq? inner) (is-infix-expression? inner)) true
           (and (seq? inner) (= 1 (count inner))) (grouping-wrap? inner)
           :else false))))

(defn- process-nested-infix
  "Recursively process nested infix expressions."
  [form]
  (cond
    ;; Redundant grouping: `((expr))` or `(5)` etc. — peel one layer of parens
    ;; and recurse. This lets users over-parenthesise without producing
    ;; `((+ 3 4))` at runtime, which would try to call 7 as a function.
    (grouping-wrap? form)
    (process-nested-infix (first form))

    ;; If it's a list, check if it's infix or function call
    (seq? form)
    (if (is-infix-expression? form)
      ;; Process as infix expression
      (-> form
          (as-> processed (map process-nested-infix processed))
          parser/parse-infix
          compiler/compile-postfix)
      ;; Process as function call, recursively processing arguments
      (apply list (map process-nested-infix form)))

    ;; If it's a vector, recursively process elements
    (vector? form)
    (vec (map process-nested-infix form))

    ;; Otherwise return as-is
    :else
    form))

(defn- is-arrow-lambda?
  "Check if expression looks like an arrow lambda: param(s) => body"
  [tokens]
  (and (>= (count tokens) 3)
       ;; Check if first token is lambda parameter(s)
       (let [first-token (first tokens)]
         (or (and (symbol? first-token)
                  (not (operator? first-token)))
             ;; Parameter list like (x y)
             (and (sequential? first-token)
                  (every? #(and (symbol? %) (not (operator? %))) first-token))))
       ;; Second token is =>
       (= '=> (second tokens))
       ;; Has a body after =>
       (>= (count tokens) 3)))

(defn- compile-arrow-lambda
  "Compile arrow lambda to fn form."
  [params body-tokens]
  (let [param-vector (cond
                       ;; Single parameter: x -> expr
                       (symbol? params) [params]

                       ;; Parameter list: (x, y) -> expr
                       (sequential? params)
                       (vec params))

        processed-body (if (= 1 (count body-tokens))
                         ;; A body that is a single form is already a complete
                         ;; Clojure expression (e.g. `(max x 5)` or a nested
                         ;; `(infix y => x + y)`). Don't run it through the
                         ;; infix parser, which would treat the macro name and
                         ;; `=>` as operands and emit a broken vector stack.
                         (process-nested-infix (first body-tokens))
                         ;; Multi-token body — parse as an infix expression.
                         (-> body-tokens
                             (as-> processed (map process-nested-infix processed))
                             parser/parse-infix
                             compiler/compile-postfix))]
    `(~'fn ~param-vector ~processed-body)))

(defmacro infix
  "Transform infix expressions into Clojure forms.

  Example:
    (infix a + b * c) => (+ a (* b c))
    (infix x => x * x) => (fn [x] (* x x))"
  [& expr]
  (if (is-arrow-lambda? expr)
    ;; Handle arrow lambda: x => expr
    (compile-arrow-lambda (first expr) (drop 2 expr))
    ;; Handle regular infix expression
    (-> expr
        parser/transform-function-calls
        (as-> processed (map process-nested-infix processed))
        parser/parse-infix
        compiler/compile-postfix)))

(defmacro _+_
  "Terse alias for `infix` — operand-operator-operand notation.

  Example:
    (_+_ a + b * c) => (+ a (* b c))
    (_+_ x => x * x) => (fn [x] (* x x))"
  [& expr]
  `(infix ~@expr))

(defn- process-binding-pairs
  "Process infix-let binding pairs, transforming RHS to infix."
  [bindings]
  (loop [pairs bindings
         result []]
    (if (< (count pairs) 2)
      result
      (let [sym (first pairs)
            expr (second pairs)
            processed-expr (if (and (sequential? expr)
                                    ;; Only process if it contains infix operators or is an infix expression
                                    (or (some operator? expr)
                                        (is-infix-expression? expr)))
                             (-> expr
                                 (as-> processed (map process-nested-infix processed))
                                 parser/parse-infix
                                 compiler/compile-postfix)
                             ;; Otherwise just recursively process nested expressions  
                             (process-nested-infix expr))]
        (recur (drop 2 pairs)
               (conj result sym processed-expr))))))

(defmacro infix-let
  "Sequential bindings with infix expressions on the right-hand side.
  
  Example:
    (infix-let [a (1 + 2) b (a * 4)] b + 10)
    => (let [a (+ 1 2) b (* a 4)] (+ b 10))"
  [bindings & body]
  (let [processed-bindings (process-binding-pairs bindings)
        processed-body (if (= 1 (count body))
                         ;; Single expression body
                         (let [expr (first body)]
                           (if (and (sequential? expr)
                                    (is-infix-expression? expr))
                             (-> expr
                                 (as-> processed (map process-nested-infix processed))
                                 parser/parse-infix
                                 compiler/compile-postfix)
                             (process-nested-infix expr)))
                         ;; Multiple expressions - treat as do block
                         `(do ~@(map (fn [expr]
                                       (if (and (sequential? expr)
                                                (is-infix-expression? expr))
                                         (-> expr
                                             (as-> processed (map process-nested-infix processed))
                                             parser/parse-infix
                                             compiler/compile-postfix)
                                         (process-nested-infix expr)))
                                     body)))]
    `(let ~processed-bindings ~processed-body)))

(defmacro infix-defn
  "Define a function with infix expressions in the body.
  
  Example:
    (infix-defn square [x] x * x)
    => (defn square [x] (* x x))"
  [fn-name & args]
  (let [[docstring params body] (if (string? (first args))
                                  ;; With docstring: (infix-defn name "doc" [params] body)
                                  [(first args) (second args) (drop 2 args)]
                                  ;; Without docstring: (infix-defn name [params] body)
                                  [nil (first args) (rest args)])
        ;; Check if body has bare operator symbols at the top level,
        ;; indicating it's a single infix expression (e.g., x * x, x >= min-val and x <= max-val)
        has-bare-operators? (and (> (count body) 1)
                                (some #(and (symbol? %) (operator? %)) body))
        ;; Process the body - support multiple expressions
        processed-body (if (or (= 1 (count body)) has-bare-operators?)
                         ;; Single infix expression body - process as infix
                         (-> body
                             parser/transform-function-calls
                             (as-> processed (map process-nested-infix processed))
                             parser/parse-infix
                             compiler/compile-postfix)
                         ;; Multiple expressions - apply function call transformation to the entire body first
                         (let [transformed-body (parser/transform-function-calls body)]
                           `(do ~@(map (fn [expr]
                                         (if (and (sequential? expr)
                                                  (is-infix-expression? expr))
                                           (-> expr
                                               (as-> processed (map process-nested-infix processed))
                                               parser/parse-infix
                                               compiler/compile-postfix)
                                           ;; Otherwise just recursively process nested expressions
                                           (process-nested-infix expr)))
                                       transformed-body))))]
    ;; Wrap the processed body in try-catch to handle early returns
    (let [wrapped-body `(try ~processed-body
                             (catch Exception e#
                               (if (and (= "return" (.getMessage e#))
                                        (contains? (ex-data e#) :return-value))
                                 (:return-value (ex-data e#))
                                 (throw e#))))]
      (if docstring
        `(defn ~fn-name ~docstring ~params ~wrapped-body)
        `(defn ~fn-name ~params ~wrapped-body)))))

;; Early return mechanism using exceptions
(def ^:private return-exception-marker "return")

(defn throw-return
  "Throw special exception for early returns. Used by the return macro."
  [value]
  (throw (ex-info return-exception-marker {:return-value value})))

(defn- handle-return-exception 
  "Handle early return exceptions in try-catch blocks."
  [e]
  (when (and (= return-exception-marker (.getMessage e))
             (:return-value (ex-data e)))
    (:return-value (ex-data e))))

(defmacro return
  "Early return from infix-defn functions.
  
  Example:
    (infix-defn safe-div [x y]
      (when y = 0 (return nil))
      x / y)"
  [value]
  `(infix.core/throw-return ~value))