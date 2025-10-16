(ns infix.parser
  "Infix expression parser using Shunting Yard algorithm."
  (:require [infix.precedence :as prec]))

(defn- function-call-pattern?
  "Check if a symbol could be followed by function call syntax."
  [sym]
  (and (symbol? sym)
       (let [s (str sym)]
         ;; Valid function name: starts with letter or special chars, contains valid chars
         (re-matches #"^[a-zA-Z_\-\+\*\/\<\>\=\!\?\$\%\&\.][a-zA-Z0-9_\-\+\*\/\<\>\=\!\?\$\%\&\.]*$" s))))

(defn- comma-separated-args
  "Split comma-separated arguments into individual arguments."
  [args-list]
  (if (empty? args-list)
    []
    ;; For now, just return the arguments as-is since commas are whitespace in Clojure
    ;; We'll handle this when we get actual comma-separated syntax working
    args-list))

(defn transform-function-calls
  "Transform function call syntax by detecting symbol followed by list pattern."
  [expr]
  (cond
    (sequential? expr)
    (let [items (vec expr)]
      (loop [i 0
             result []]
        (if (>= i (count items))
          (seq result)  ; Convert back to list/seq
          (let [current (items i)
                next-item (get items (inc i))]
            (if (and (function-call-pattern? current)
                     (sequential? next-item))
              ;; Found function call pattern
              (let [transformed-args (map transform-function-calls next-item)
                    fn-call (cons current transformed-args)]
                (recur (+ i 2) (conj result fn-call)))
              ;; Not a function call pattern
              (recur (inc i) (conj result (transform-function-calls current))))))))
    
    :else
    expr))

(defn tokenize
  "Convert infix expression into sequence of tokens."
  [expr]
  (vec (transform-function-calls expr)))

(defn- operator?
  "Check if token is an operator."
  [token]
  (contains? #{'+ '- '* '/ '< '<= '> '>= '= 'not= 'and 'or 'not '-> '->> 'some-> 'some->>} token))

(defn- lambda-parameter?
  "Check if a token looks like a lambda parameter."
  [token]
  (and (symbol? token)
       (not (operator? token))
       (not (contains? #{'fn 'defn 'let 'if 'when 'cond} token))))

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
  (contains? #{'apply 'reduce 'map 'filter 'partial 'comp} sym))

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