(ns infix.compiler-test
  (:require [clojure.test :refer [deftest is testing]]
            [infix.compiler :as compiler]))

;; Step 2.3: Postfix to Clojure Form Compiler Tests (TDD - RED phase)
(deftest compile-postfix
  (testing "compiling postfix to Clojure expressions"
    (is (= '(+ a b) (compiler/compile-postfix ['a 'b '+])))
    (is (= '(+ a (* b c)) (compiler/compile-postfix ['a 'b 'c '* '+])))
    (is (= '(* (+ a b) c) (compiler/compile-postfix [['a 'b '+] 'c '*])))))