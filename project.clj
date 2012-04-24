;; vim: set ts=2 sw=2 et ai ft=clojure:

(defproject keikai "0.1.0-SNAPSHOT"
  :description "Monitoring and metrics framework for distributed systems."
  :url "http://github.com/codeslinger/keikai"
  :local-repo-classpath true
  :dependencies [
    [clojure "1.3.0"]
    [io.netty/netty "3.3.1.Final"]
    [org.clojure/tools.logging "0.2.3"]
  ]
  :dev-dependencies [
    [swank-clojure "1.4.2"]
  ]
  :main keikai.cli
)

