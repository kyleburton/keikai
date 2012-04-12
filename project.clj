;; vim: set ts=2 sw=2 et ai ft=clojure:

(defproject keikai "0.1.0-SNAPSHOT"
  :description "Monitoring and metrics framework for distributed systems."
  :url "http://github.com/codeslinger/keikai"
  :dependencies [
    [clojure "1.3.0"]
    [clojure-contrib "1.1.0"]
    [log4j/log4j "1.2.16" :exclusions [javax.mail/mail
                                       javax.jms/jms
                                       com.sun.jdmk/jmxtools
                                       com.sun.jmx/jmxri]]
  ]
  :dev-dependencies [
    [swank-clojure "1.4.0-SNAPSHOT"]
  ]
  :main keikai.cli
)

