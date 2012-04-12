;; vim: set ts=2 sw=2 et ai ft=clojure:
(ns keikai.cli
  (:require [keikai.config :as config]
            [keikai.core :as core])
  (:use clojure.contrib.logging))

(defn -main
  "Start Keikai service. Loads configuration file path via first
   command-line argument, or the default if none specified."
  [& argv]
  (try
   (config/include (first argv))
   (core/start)
   (catch Exception e
     (error e "Aborting"))))
