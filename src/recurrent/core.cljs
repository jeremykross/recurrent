(ns recurrent.core
  (:require [ulmus.signal :as ulmus]))

(defn make-sink-proxies
  [drivers]
  (into {} (map (fn [[k v]] [k (ulmus/signal)]) drivers)))

(defn call-drivers!
  [drivers sink-proxies]
  (into {} (map (fn [[k driver]] [k (driver (sink-proxies k))]) drivers)))
    
(defn replicate-many!
  [sinks sink-proxies]
  (into {} (map (fn [[k sink-proxy]] [k (ulmus/splice! sink-proxy (sinks k))]) sink-proxies)))

(defn start!
  [main sources & args]
  (let [drivers (into {} (filter (fn [[_ v]] (:recurrent/driver? (meta v))) sources))]
    (let [sink-proxies (make-sink-proxies drivers)
          running-drivers (call-drivers! drivers sink-proxies)
          sinks (apply main (merge sources running-drivers) args)]
      (replicate-many! sinks sink-proxies)
      sinks)))

(defn close!
  [c]
  (doseq [[k sink-$] c]
    (println "Closing: " k)
    (ulmus/close! sink-$ :transitive? true)))
