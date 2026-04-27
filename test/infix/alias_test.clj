(ns infix.alias-test
  (:require [clojure.test :refer [deftest is testing]]
            [infix.core :refer [_+_]]))

(deftest underscore-plus-alias
  (testing "_+_ behaves identically to infix for arithmetic"
    (is (= 7 (_+_ 3 + 4)))
    (is (= 14 (_+_ 2 * 3 + 4 * 2)))
    (is (= 25 (_+_ 5 * 5))))

  (testing "_+_ supports comparison and boolean operators"
    (is (= true (_+_ 5 > 3)))
    (is (= true (_+_ 1 < 2 and 2 < 3))))

  (testing "_+_ supports arrow-lambda syntax"
    (is (= 25 ((_+_ x => x * x) 5)))
    (is (= 7 ((_+_ (x y) => x + y) 3 4))))

  (testing "_+_ supports nested fn(args) calls"
    (is (= 3 (_+_ count([1 2 3]))))
    (is (= 4 (_+_ count([1 2 3]) + 1))))

  (testing "_+_ supports redundant grouping"
    (is (= 7 (_+_ ((((3 + 4))))))))

  (testing "macroexpansion produces equivalent form to infix"
    (is (= (macroexpand '(infix.core/infix 3 + 4))
           (macroexpand '(infix.core/_+_ 3 + 4))))))
