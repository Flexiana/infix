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
       ;; Check for operator between operands (infix pattern) 
       (and (> (count lst) 2)
            (not (known-function? (first lst)))
            (some operator? (rest lst)))))

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
      (map process-nested-infix form))
    
    ;; If it's a vector, recursively process elements
    (vector? form)
    (vec (map process-nested-infix form))
    
    ;; Otherwise return as-is
    :else
    form))

(defmacro infix
  "Transform infix expressions into Clojure forms.
  
  Example:
    (infix a + b * c) => (+ a (* b c))"
  [& expr]
  (-> expr
      (as-> processed (map process-nested-infix processed))
      parser/parse-infix
      compiler/compile-postfix))