;; vim: set ts=2 sw=2 et ai ft=clojure:
(ns keikai.collectd
  (:import (java.io EOFException)
           (java.nio ByteBuffer
                     ByteOrder))
  (:require [clojure.string :as str]
            [keikai.util :as u]))

; collectd packet field header length
;   2 bytes -> field type (unsigned)
;   2 bytes -> field length (unsigned)
(def header-len 4)
; values for field type in header
(def field-types {0 :host
                  1 :time
                  2 :plugin
                  3 :plugin-instance
                  4 :type
                  5 :type-instance
                  6 :values
                  7 :interval
                  8 :time-hires
                  9 :interval-hires
                  256 :message
                  257 :severity
                  512 :signature
                  513 :encryption})
; values to disambiguate types in list of values within a Values list
(def value-types {0 :counter
                  1 :gauge
                  2 :derive
                  3 :absolute})
; message severity values
(def severities {1 :failure
                 2 :warning
                 4 :okay})
; label fields to put in stack to keep track of packet parsing depth
(def pos-fields '(:plugin
                  :plugin-instance
                  :type
                  :type-instance))

(defn- read-severity [ios len]
  (or (severities (int (.readLong ios)))
      :unknown))

(defn- read-str [ios len]
  (let [buf (byte-array len)]
    (.read ios buf 0 len)
    ; len-1 to skip the NUL sentinel; collectd sends C strings
    (String. buf 0 (- len 1))))

(defn- read-int [ios len]
  (.readLong ios))

; collectd sends doubles in little-endian order, thus the dance below
(defn- read-double [ios len]
  (let [buf (byte-array len)]
    (.read ios buf)
    (let [bb (ByteBuffer/wrap buf)]
      (.order bb ByteOrder/LITTLE_ENDIAN)
      (Double. (.getDouble bb)))))

(defn- read-values [ios len]
  (let [nvalues (.readUnsignedShort ios)
        types (map (fn [x] (.readByte ios)) (range nvalues))]
    (vec
     (map #(let [valtype (value-types %1)]
             (if (= :gauge valtype)
               [valtype (read-double ios 8)]
               [valtype (read-int ios 8)]))
          types))))

(defn- discard-bytes [ios len]
  (dotimes [_ len]
    (.readByte ios))
  true)

; type -> handler function map for easy dispatch on field type
(def field-fns {:host read-str
                :time read-int
                :time-hires read-int
                :interval read-int
                :interval-hires read-int
                :plugin read-str
                :plugin-instance read-str
                :type read-str
                :type-instance read-str
                :message read-str
                :severity read-severity
                :values read-values})

(defn- read-field [ios]
  (let [type (field-types (.readUnsignedShort ios))
        len (.readUnsignedShort ios)]
    (if (>= len header-len)
      (let [size (- len header-len)
            f (or (field-fns type) discard-bytes)]
        [len [type (f ios size)]]))))

(defn- parse-fields
  "Parse a collectd packet from the given input stream."
  [ios len]
  (try
   (loop [size len
          fields ()]
     (let [[nr data] (read-field ios)]
       (if (not (nil? nr))
         (let [rem (- size nr)]
           (if (= rem 0)
             (reverse (cons data fields))
             (recur rem (cons data fields)))))))
   (catch EOFException e
     nil)))

(defn- new-pos []
  {:plugin nil
   :plugin-instance nil
   :type nil
   :type-instance nil})

(defn- mark-pos [pos type data]
  (cond
   (= type :plugin) (do
                      (assoc pos :plugin data)
                      (assoc pos :plugin-instance nil)
                      (assoc pos :type nil)
                      (assoc pos :type-instance nil))
   (= type :plugin-instance) (do
                               (assoc pos :plugin-instance data)
                               (assoc pos :type nil)
                               (assoc pos :type-instance nil))
   (= type :type) (do
                    (assoc pos :type data)
                    (assoc pos :type-instance nil))
   (= type :type-instance) (assoc pos :type-instance data))
  pos)

(defn- format-key [pos]
  (let [a (:plugin pos)
        b (:plugin-instance pos)
        c (:type pos)
        d (:type-instance pos)]
    (str/join "." (keep identity '(a b c d)))))

(defn- format-field [pkt pos type data]
  (let [key (format-key pos)]
    (if (not (contains? pos key))
      (assoc pkt key {}))
    (if (= "" key)
      (assoc pkt type data)
      (assoc (get pkt key) type data))))

(defn decode
  "Parse a collectd packet into a canonical metric information record."
  [ios len]
  (let [fields (parse-fields ios len)
        pkt {}
        pos (new-pos)]
    (for [f fields]
      (let [[type data] f]
        (if (u/in? type pos-fields)
          (mark-pos pos type data)
          (format-field pkt pos type data))))
    pkt))

(defn handle
  "Process an individual collectd packet."
  [pkt]
  (println "----packet----")
  (doseq [x pkt]
    (prn x)))
