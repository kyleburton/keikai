;; vim: set ts=2 sw=2 et ai ft=clojure:
(ns keikai.cli)

(def- main
  "Start Keikai service. Loads configuration from first command-line
   argument."
  [& m]
    (try
      (keikai.config/include (first argv))
      (keikai.core/start)
      (catch Exception e
        (error e "Aborting"))))

