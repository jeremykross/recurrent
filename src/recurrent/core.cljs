(ns recurrent.core
  (:require [ulmus.signal :as ulmus]))

(defn- make-sink-proxies
  [drivers]
  (into {} (map (fn [[k v]] [k (ulmus/signal)]) drivers)))

(defn- call-drivers!
  [drivers sink-proxies]
  (into {} (map (fn [[k driver]] [k (driver (sink-proxies k))]) drivers)))
    
(defn- replicate-many!
  [sinks sink-proxies]
  (into {} (map (fn [[k sink-proxy]] [k (if (sinks k) (ulmus/splice! sink-proxy (sinks k)) (ulmus/signal))]) sink-proxies)))

(defn start!
  [main sources & args]
  (let [drivers (into {} (filter (fn [[_ v]] (:recurrent/driver? (meta v))) sources))]
    (let [sink-proxies (make-sink-proxies drivers)
          running-drivers (call-drivers! drivers sink-proxies)
          all-sources (merge sources running-drivers)
          sinks (apply main all-sources args)]
      (replicate-many! sinks sink-proxies)
      (with-meta
        sinks
        {:recurrent/sources all-sources}))))

(defn close!
  [c]
  (doseq [[k sink-$] c]
    (ulmus/close! sink-$ :transitive? true)))
