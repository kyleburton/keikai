;; vim: set ts=2 sw=2 et ai ft=clojure:
(ns keikai.collectd
  (:import (java.io EOFException)))

(def header-len 4)
(def field-types {0 :host
                  1 :time
                  2 :plugin
                  3 :plugin-instance
                  4 :type
                  5 :type-instance
                  6 :values
                  7 :interval
                  256 :message
                  257 :severity})
(def value-types {0 :counter
                  1 :gauge
                  2 :derive
                  3 :absolute})

(defn- read-str [ios len]
  (let [buf (byte-array len)]
    (.read ios buf 0 len)
    (String. buf 0 (- len 1))))  ; len-1 to skip the NUL sentinel; collectd sends C strings

(defn- read-values [ios pkt len]
  nil)

(defn- read-obj [ios pkt]
  (try
   (let [type (.readUnsignedShort ios)
         len (.readUnsignedShort ios)]
     (if (< len header-len)
       nil
       (let [size (- len header-len)
             field (type field-types)]
         (cond (= field :host) (assoc pkt field (read-str ios size))
               (= field :time) (assoc pkt field (* (.readLong ios) 1000))
               (= field :plugin) (assoc pkt field (read-str ios size))
               (= field :plugin-instance) (assoc pkt field (read-str ios size))
               (= field :type) (assoc pkt field (read-str ios size))
               (= field :type-instance) (assoc pkt field (read-str ios size))
               (= field :values) (do
                                   (if (nil? (:values pkt))
                                     (assoc pkt :values {}))
                                   (assoc (:values pkt) field (read-values ios pkt size)))
               (= field :interval) (do
                                     (if (nil? (:values pkt))
                                       (assoc pkt :values {}))
                                     (assoc (:values pkt) field (.readLong ios)))
               (= field :message) (do
                                    (if (nil? (:notification pkt))
                                      (assoc pkt :notification {}))
                                    (assoc (:notification pkt) field (read-str ios size)))
               (= field :severity) (do
                                     (if (nil? (:notification pkt))
                                       (assoc pkt :notification {}))
                                     (assoc pkt field (int (.readLong ios)))))
         true)))
   (catch EOFException e
     nil)))

(defn decode
  "Parse a collectd packet from the given input stream."
  [ios]
  (let [pkt {}]
    (while (read-obj ios pkt)
           nil)
    pkt))

(defn handle
  "Process an individual collectd packet."
  [pkt]
  nil)
