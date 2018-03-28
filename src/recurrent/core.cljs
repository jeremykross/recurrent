(ns recurrent.core
  (:refer-clojure :exclude [run!])
  (:require 
    [cljs.core.async :as async]
    [ulmus.core :as e-sig]
    [recurrent.drivers.dom :as dom-driver])
  (:require-macros
    [cljs.core.async.macros :as async-mac]))


(defn- make-sink-proxies
  [drivers]
  (reduce (fn [sink-proxies [k v]]
            (assoc sink-proxies
                   k (e-sig/signal)))
          {} drivers))

(defn- call-drivers
  [drivers sink-proxies]
  (let [sources (reduce (fn [sources [k sink]]
                          (assoc sources
                                 k ((drivers k) sink)))
                      {} sink-proxies)]
    sources))

(defn- replicate-many!
  [signals proxies]
  (doseq [[k signal] signals]
    (when (proxies k)
      (async/pipe (async/tap 
                    (:mult signal) 
                    (async/chan (async/sliding-buffer 1) nil))
                  (:ch (proxies k)))
      (when @signal
        (e-sig/next! (proxies k) @signal)))))

(defn run!
  "Take a function to be run `main`, `props` (which is a map where the values are non-signals), `sources` (which is a map where the values are signals), and drivers (a map where the values are recurrent drivers).  Calls the function provided as main passing the props and the merging of the sources and drivers.  Will match the returned sinks from `main` with the provided drivers to perform associated mutations."
  ([main drivers] (run! main {} {} drivers))
  ([main props other-sources drivers]
    (let [sink-proxies (make-sink-proxies drivers)
          sources (call-drivers drivers sink-proxies)
          sinks (main props (merge sources other-sources))]
      (replicate-many! sinks sink-proxies)
      {:sources sources :sinks sinks})))
