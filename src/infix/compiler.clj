(ns infix.compiler
  "Compile postfix notation to Clojure forms.")

(defn- operator?
  "Check if token is an operator."
  [token]
  (contains? #{'+ '- '* '/ '< '<= '> '>= '= 'not= 'and 'or 'not '|>} token))

(defn- unary-operator?
  "Check if token is a unary operator."
  [token]
  (contains? #{'not} token))

(defn- pipeline-operator?
  "Check if token is a pipeline operator."
  [token]
  (= token '|>))

(defn- compile-pipeline
  "Convert pipeline operation to threading macro."
  [data operation]
  (if (seq? operation)
    ;; If operation is a function call, thread data as last argument (like ->>)
    (concat operation [data])
    ;; If operation is a simple function, create function call with data
    (list operation data)))

(defn compile-postfix
  "Convert postfix notation to Clojure expression."
  [postfix-tokens]
  (if (vector? postfix-tokens)
    ;; Handle nested vectors (partial compilation results)
    (let [stack (atom [])]
      (doseq [token postfix-tokens]
        (if (operator? token)
          (cond
            ;; Unary operator - only needs one operand
            (unary-operator? token)
            (let [a (peek @stack)]
              (swap! stack #(-> % pop (conj (list token a)))))
            
            ;; Pipeline operator - special handling
            (pipeline-operator? token)
            (let [b (peek @stack)
                  a (peek (pop @stack))]
              (swap! stack #(-> % pop pop (conj (compile-pipeline a b)))))
            
            ;; Binary operator - needs two operands  
            :else
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