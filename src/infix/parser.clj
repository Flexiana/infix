(ns infix.parser
  "Infix expression parser using Shunting Yard algorithm."
  (:require [infix.precedence :as prec]))

(defn tokenize
  "Convert infix expression into sequence of tokens."
  [expr]
  (vec expr))

(defn- operator?
  "Check if token is an operator."
  [token]
  (contains? #{'+ '- '* '/} token))

(defn- flatten-tokens
  "Flatten nested expressions while marking parentheses groups."
  [expr]
  (reduce
   (fn [acc token]
     (if (and (seq? token) (not (empty? token)))
       (-> acc
           (conj :lparen)
           (into (flatten-tokens token))
           (conj :rparen))
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