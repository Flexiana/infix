(ns infix.compiler
  "Compile postfix notation to Clojure forms."
  (:require [infix.parser :as parser]))

(defn- operator?
  "Check if token is an operator."
  [token]
  (contains? #{'+ '- '* '/ '< '<= '> '>= '= 'not= 'and 'or 'not '-> '->> 'some-> 'some->>} token))

(defn- unary-operator?
  "Check if token is a unary operator."
  [token]
  (contains? #{'not} token))

(defn- threading-operator?
  "Check if token is a threading operator."
  [token]
  (contains? #{'-> '->> 'some-> 'some->>} token))

(defn- compile-threading
  "Convert threading operation to direct threading macro usage."
  [data operation threading-op]
  (let [threaded-form (cond
                        ;; Thread-first operators (-> some->)
                        (contains? #{'-> 'some->} threading-op)
                        (if (seq? operation)
                          ;; Function call: thread data as first argument
                          (list* (first operation) data (rest operation))
                          ;; Simple function: create function call
                          (list operation data))
                        
                        ;; Thread-last operators (->> some->>)  
                        (contains? #{'->> 'some->>} threading-op)
                        (if (seq? operation)
                          ;; Function call: thread data as last argument
                          (concat operation [data])
                          ;; Simple function: create function call
                          (list operation data))
                        
                        :else
                        (throw (ex-info "Unknown threading operator" {:operator threading-op})))]
    ;; Wrap with some-> or some->> if nil-safe
    (if (contains? #{'some-> 'some->>} threading-op)
      (list threading-op data operation)
      threaded-form)))

(defn- process-nested-expressions
  "Recursively process nested expressions for infix compilation."
  [form]
  (cond
    ;; If it's a list (function call), recursively process arguments
    (seq? form)
    (map process-nested-expressions form)
    
    ;; Otherwise return as-is
    :else
    form))

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
              (swap! stack #(-> % pop (conj (list token (process-nested-expressions a))))))
            
            ;; Threading operators - direct compilation
            (threading-operator? token)
            (let [b (peek @stack)
                  a (peek (pop @stack))]
              (swap! stack #(-> % pop pop (conj (compile-threading (process-nested-expressions a) (process-nested-expressions b) token)))))
            
            ;; Binary operator - needs two operands  
            :else
            (let [b (peek @stack)
                  a (peek (pop @stack))]
              (swap! stack #(-> % pop pop (conj (list token (process-nested-expressions a) (process-nested-expressions b)))))))
          (swap! stack conj (if (vector? token)
                             (compile-postfix token)  ; Recursively compile nested vectors
                             (process-nested-expressions token)))))
      (let [result @stack]
        (if (= 1 (count result))
          (first result)
          result)))
    ;; Handle already compiled forms
    postfix-tokens))