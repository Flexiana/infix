(ns infix.core
  "Main public API for the Infix syntax library."
  (:require [infix.parser :as parser]
            [infix.compiler :as compiler]))

(defn- operator? [token]
  (contains? #{'+ '- '* '/ '< '<= '> '>= '= 'not= 'and 'or 'not '-> '->> 'some-> 'some->>} token))

(defn- known-function? [sym]
  (contains? #{'apply 'reduce 'map 'filter 'partial 'comp 'max 'min 'count 'empty? 'str} sym))

(defn- is-infix-expression? 
  "Check if a list looks like an infix expression (not a function call)."
  [lst]
  (and (seq? lst) 
       (not (empty? lst))
       ;; NOT starting with an operator (that would be a function call like (+ 1 2))
       (not (operator? (first lst)))
       ;; Has infix operators between operands (not just as first element)
       (let [operators-in-middle (some operator? (rest lst))]
         (and operators-in-middle
              ;; And it's not a known function that might take operators as arguments
              (not (known-function? (first lst)))))))

(defn- process-nested-infix
  "Recursively process nested infix expressions."
  [form]
  (cond
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
                       
        processed-body (-> body-tokens
                           (as-> processed (map process-nested-infix processed))
                           parser/parse-infix
                           compiler/compile-postfix)]
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
        (as-> processed (map process-nested-infix processed))
        parser/parse-infix
        compiler/compile-postfix)))

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
                           (if (sequential? expr)
                             (-> expr
                                 (as-> processed (map process-nested-infix processed))
                                 parser/parse-infix
                                 compiler/compile-postfix)
                             expr))
                         ;; Multiple expressions - treat as do block
                         `(do ~@(map (fn [expr]
                                       (if (sequential? expr)
                                         (-> expr
                                             (as-> processed (map process-nested-infix processed))
                                             parser/parse-infix
                                             compiler/compile-postfix)
                                         expr))
                                     body)))]
    `(let ~processed-bindings ~processed-body)))