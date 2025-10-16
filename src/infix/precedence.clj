(ns infix.precedence
  "Operator precedence definitions and utilities.")

(def ^:private precedence-map
  "Operator precedence table. Higher numbers = higher precedence."
  {'+ 1
   '- 1
   '* 2
   '/ 2})

(def ^:private associativity-map
  "Operator associativity. true = left-associative, false = right-associative."
  {'+ true
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