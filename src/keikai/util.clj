;; vim: set ts=2 sw=2 et ai ft=clojure:
(ns keikai.util)

(defn in? [elem lst]
  (some #(= elem %) lst))
