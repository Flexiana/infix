(ns infix.compiler
  "Compile postfix notation to Clojure forms.")

(defn- operator?
  "Check if token is an operator."
  [token]
  (contains? #{'+ '- '* '/} token))

(defn compile-postfix
  "Convert postfix notation to Clojure expression."
  [postfix-tokens]
  (if (vector? postfix-tokens)
    ;; Handle nested vectors (partial compilation results)
    (let [stack (atom [])]
      (doseq [token postfix-tokens]
        (if (operator? token)
          (let [b (peek @stack)
                a (peek (pop @stack))]
            (swap! stack #(-> % pop pop (conj (list token a b)))))
          (swap! stack conj (if (vector? token)
                             (compile-postfix token)  ; Recursively compile nested vectors
                             token))))
      (first @stack))
    ;; Handle already compiled forms
    postfix-tokens))