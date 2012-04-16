;; vim: set ts=2 sw=2 et ai ft=clojure:
(ns keikai.collectd
  (:import (java.io EOFException)
           (java.nio ByteBuffer
                     ByteOrder)))

(def header-len 4)
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
(def field-fns {:host 'read-str
                :time 'read-int
                :time-hires 'read-int
                :interval 'read-int
                :interval-hires 'read-int
                :plugin 'read-str
                :plugin-instance 'read-str
                :type 'read-str
                :type-instance 'read-str
                :message 'read-str
                :severity 'read-severity
                :values 'read-values})
(def value-types {0 :counter
                  1 :gauge
                  2 :derive
                  3 :absolute})
(def severities {1 :failure
                 2 :warning
                 4 :okay})

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
        types (map (.readByte ios) (range nvalues))]
    (map #(let [valtype ((int %1) value-types)]
            (if (= :gauge valtype)
              [valtype (read-double ios 8)]
              [valtype (read-int ios 8)]))
         types)))

(defn- discard-bytes [ios len]
  (dotimes [_ len]
    (.readByte ios))
  nil)

(defn- parse-field [ios pkt]
  (let [type (field-types (.readUnsignedShort ios))
        len (.readUnsignedShort ios)]
    (if (< len header-len)
      nil
      (try
       (let [size (- len header-len)
             f (or (field-fns type) 'discard-bytes)
             data (apply f [ios size])]
         (if (not (nil? data))
           (do
             (println size " " f " " type " => " data)))
         true)
       (catch EOFException e
         nil)))))

(defn decode
  "Parse a collectd packet from the given input stream."
  [ios]
  (let [pkt {}]
    (while (parse-field ios pkt)
           nil)
    pkt))

(defn handle
  "Process an individual collectd packet."
  [pkt]
  (println pkt))

;; (defn- xread-obj [ios pkt]
;;   (try
;;    (let [type (.readUnsignedShort ios)
;;          len (.readUnsignedShort ios)]
;;      (println "found obj of type" type "size" len)
;;      (if (< len header-len)
;;        nil
;;        (let [size (- len header-len)
;;              field (get field-types type)]
;;          (println "got field" field "type" type "len" size)
;;          (cond (= field :host) (assoc pkt field (read-str ios size))
;;                (= field :time) (assoc pkt field (* (.readLong ios) 1000))
;;                (= field :time-hires) (assoc pkt :time (* (.readLong ios) 1000))
;;                (= field :plugin) (assoc pkt field (read-str ios size))
;;                (= field :plugin-instance) (assoc pkt field (read-str ios size))
;;                (= field :type) (assoc pkt field (read-str ios size))
;;                (= field :type-instance) (assoc pkt field (read-str ios size))
;;                (= field :values) (do
;;                                    (if (nil? (:values pkt))
;;                                      (assoc pkt :values {}))
;;                                    (assoc (:values pkt) field (read-values ios pkt size)))
;;                (= field :interval) (do
;;                                      (if (nil? (:values pkt))
;;                                        (assoc pkt :values {}))
;;                                      (assoc (:values pkt) field (.readLong ios)))
;;                (= field :interval-hires) (do
;;                                            (if (nil? (:values pkt))
;;                                              (assoc pkt :values {}))
;;                                            (assoc (:values pkt) :interval (.readLong ios)))
;;                (= field :message) (do
;;                                     (if (nil? (:notification pkt))
;;                                       (assoc pkt :notification {}))
;;                                     (assoc (:notification pkt) field (read-str ios size)))
;;                (= field :severity) (do
;;                                      (if (nil? (:notification pkt))
;;                                        (assoc pkt :notification {}))
;;                                      (assoc pkt field
;;                ; skip unknown fields
;;                :else (dotimes [size] (.readByte ios)))
;;          true)))
;;    (catch EOFException e
;;      nil)))