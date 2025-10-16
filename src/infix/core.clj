(ns infix.core
  "Main public API for the Infix syntax library."
  (:require [infix.parser :as parser]
            [infix.compiler :as compiler]))

(defmacro infix
  "Transform infix expressions into Clojure forms.
  
  Example:
    (infix a + b * c) => (+ a (* b c))"
  [& expr]
  (-> expr
      parser/parse-infix
      compiler/compile-postfix))