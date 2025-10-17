(ns infix.compiler
  "Compile postfix notation to Clojure forms."
  (:require [infix.parser :as parser]))

;; Constants
(def ^:private operators 
  "Set of all supported infix operators."
  #{'+ '- '* '/ '< '<= '> '>= '= 'not= 'and 'or 'not '-> '->> 'some-> 'some->>})

(def ^:private unary-operators 
  "Set of unary operators."
  #{'not})

(def ^:private threading-operators 
  "Set of threading operators."
  #{'-> '->> 'some-> 'some->>})

(def ^:private thread-first-operators 
  "Thread-first operators."
  #{'-> 'some->})

(def ^:private thread-last-operators 
  "Thread-last operators."
  #{'->> 'some->>})

(def ^:private nil-safe-operators 
  "Nil-safe threading operators."
  #{'some-> 'some->>})

(defn- operator?
  "Check if token is an operator."
  [token]
  (contains? operators token))

(defn- unary-operator?
  "Check if token is a unary operator."
  [token]
  (contains? unary-operators token))

(defn- threading-operator?
  "Check if token is a threading operator."
  [token]
  (contains? threading-operators token))


(defn- compile-threading
  "Convert threading operation to direct threading macro usage."
  [data operation threading-op]
  (let [threaded-form (cond
                        ;; Thread-first operators (-> some->)
                        (contains? thread-first-operators threading-op)
                        (if (seq? operation)
                          ;; Function call: thread data as first argument
                          (list* (first operation) data (rest operation))
                          ;; Simple function: create function call
                          (list operation data))
                        
                        ;; Thread-last operators (->> some->>)  
                        (contains? thread-last-operators threading-op)
                        (if (seq? operation)
                          ;; Function call: thread data as last argument
                          (concat operation [data])
                          ;; Simple function: create function call
                          (list operation data))
                        
                        :else
                        (throw (ex-info "Unknown threading operator" {:operator threading-op})))]
    ;; Wrap with some-> or some->> if nil-safe
    (if (contains? nil-safe-operators threading-op)
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