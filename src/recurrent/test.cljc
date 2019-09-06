(ns recurrent.test
  (:require
    recurrent.drivers.string-log
    [ulmus.core :as ulmus]
    [recurrent.core :as recurrent]))

(defn Main
  [sources]
  (ulmus/subscribe! (:log-$ sources) #(println "LOG:" %))
  {:log-$ (ulmus/input-signal)})

(defn start!
  []
  (recurrent/start!
    Main
    {:log-$ (recurrent.drivers.string-log/make-log)}))
