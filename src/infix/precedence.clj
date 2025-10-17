(ns infix.precedence
  "Operator precedence definitions and utilities.
   
   This namespace defines the precedence and associativity rules for all
   infix operators supported by the library. Higher precedence numbers
   indicate higher precedence (bind more tightly).")

(def ^:private precedence-map
  "Operator precedence table. Higher numbers = higher precedence."
  {;; Threading operators (very low precedence, lower than boolean)
   '-> 0.05
   '->> 0.05  
   'some-> 0.05
   'some->> 0.05
   ;; Boolean operators (lowest precedence)
   'or 0.1
   'and 0.2
   'not 0.8  ; not has higher precedence, almost like unary
   ;; Comparison operators 
   '< 0.5
   '<= 0.5
   '> 0.5
   '>= 0.5
   '= 0.5
   'not= 0.5
   ;; Arithmetic operators (higher precedence)
   '+ 1
   '- 1
   '* 2
   '/ 2})

(def ^:private associativity-map
  "Operator associativity. true = left-associative, false = right-associative."
  {;; Threading operators (left-associative for chaining)
   '-> true
   '->> true
   'some-> true
   'some->> true
   ;; Boolean operators
   'or true
   'and true
   'not false  ; not is right-associative (prefix-like)
   ;; Comparison operators
   '< true
   '<= true
   '> true
   '>= true
   '= true
   'not= true
   ;; Arithmetic operators  
   '+ true
   '- true
   '* true
   '/ true})

(defn precedence
  "Get precedence value for an operator."
  [op]
  (get precedence-map op 0))

(defn left-associative?
  "Check if operator is left-associative."
  [op]
  (get associativity-map op true))