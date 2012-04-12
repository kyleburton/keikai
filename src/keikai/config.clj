;; vim: set ts=2 sw=2 et ai ft=clojure:
(ns keikai.config)

(def ^:dynamic *default-config* "/etc/keikai.conf")

(defn include
  "Load a Clojure file, typically for configuration purposes. The file is
   evaluated in the same namespace as this file."
  [file]
  (let [file (or file (first *command-line-args*) *default-config*)]
    (binding [*ns* (find-ns 'keikai.config)]
      (load-string (slurp file)))))
