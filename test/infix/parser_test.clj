(ns infix.parser-test
  (:require [clojure.test :refer [deftest is testing]]
            [infix.parser :as parser]))

;; Step 1.2: Basic Tokenization Tests (TDD - RED phase)
(deftest tokenize-simple-arithmetic
  (testing "tokenizing simple arithmetic expressions"
    (is (= ['a '+ 'b] (parser/tokenize '(a + b))))
    (is (= [1 '* 2] (parser/tokenize '(1 * 2))))
    (is (= ['x '- 'y '+ 'z] (parser/tokenize '(x - y + z))))))

;; Step 2.2: Shunting Yard Parser Tests (TDD - RED phase)  
(deftest parse-to-postfix
  (testing "parsing infix to postfix notation"
    (is (= ['a 'b '+] (parser/parse-infix '(a + b))))
    (is (= ['a 'b 'c '* '+] (parser/parse-infix '(a + b * c))))
    (is (= ['a 'b '+ 'c '*] (parser/parse-infix '((a + b) * c))))))