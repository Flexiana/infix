(ns infix.precedence
  "Operator precedence definitions and utilities.")

(def ^:private precedence-map
  "Operator precedence table. Higher numbers = higher precedence."
  {;; Pipeline operator (very low precedence, lower than boolean)
   '|> 0.05
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
  {;; Pipeline operator (left-associative for chaining)
   '|> true
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