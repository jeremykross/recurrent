(ns recurrent.drivers.dom
  (:require
    [recurrent.core :as core]))

(defmacro defcomponent-1
  [metadata named args & body]
  `(core/defcomponent
     recurrent.drivers.dom/isolate
     ~metadata
     ~named
     ~args
     ~@body))

(defmacro defcomponent
  [named args & body]
  `(defcomponent-1
     {}
     ~named
     ~args
     ~@body))

