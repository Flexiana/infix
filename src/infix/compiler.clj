(ns infix.compiler
  "Compile postfix notation to Clojure forms.")

(defn- operator?
  "Check if token is an operator."
  [token]
  (contains? #{'+ '- '* '/ '< '<= '> '>= '= 'not= 'and 'or 'not} token))

(defn- unary-operator?
  "Check if token is a unary operator."
  [token]
  (contains? #{'not} token))

(defn compile-postfix
  "Convert postfix notation to Clojure expression."
  [postfix-tokens]
  (if (vector? postfix-tokens)
    ;; Handle nested vectors (partial compilation results)
    (let [stack (atom [])]
      (doseq [token postfix-tokens]
        (if (operator? token)
          (if (unary-operator? token)
            ;; Unary operator - only needs one operand
            (let [a (peek @stack)]
              (swap! stack #(-> % pop (conj (list token a)))))
            ;; Binary operator - needs two operands  
            (let [b (peek @stack)
                  a (peek (pop @stack))]
              (swap! stack #(-> % pop pop (conj (list token a b))))))
          (swap! stack conj (if (vector? token)
                             (compile-postfix token)  ; Recursively compile nested vectors
                             token))))
      (let [result @stack]
        (if (= 1 (count result))
          (first result)
          result)))
    ;; Handle already compiled forms
    postfix-tokens))