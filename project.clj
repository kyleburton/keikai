;; vim: set ts=2 sw=2 et ai ft=clojure:
(defproject keikai "0.1.0-SNAPSHOT"
  :description "Monitoring and metrics framework for distributed systems."
  :url "http://github.com/codeslinger/keikai"
  :dependencies [
    [clojure "1.3.0"]
    [clj-time "0.3.4"]
    [com.draines/postal "1.7-SNAPSHOT"]
  ]
  :dev-dependencies [
    [swank-clojure "1.4.0-SNAPSHOT"]
  ]
  :main keikai.cli)

