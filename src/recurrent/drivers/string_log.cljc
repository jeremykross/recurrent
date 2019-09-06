(ns recurrent.drivers.string-log
  (:require [ulmus.core :as ulmus]))

(defn make-log
  []
  (fn [sink-$]
    (let [log (atom "")
          source-$ (ulmus/input-signal)]
      (ulmus/subscribe!
        sink-$
        (fn [v]
          (swap! log str v "\n")
          (ulmus/>! source-$ @log)))
      source-$)))

