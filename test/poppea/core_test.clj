(ns poppea.core-test
  (:use clojure.test
        poppea))

(def a 4)
(def b 5)

(deftest quick-map-test
  (is (= {:a 4 :b 5 :c 3}
         (coffee-map a b :c 3))))

(defn-curried add [x y] (+ x y))

(deftest currying
  (is (= 7
         ((add 3) 4))))
