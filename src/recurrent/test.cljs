(ns recurrent.test
  (:require
    [recurrent.core :as recurrent :include-macros true]
    [recurrent.drivers.dom :as dom]
    [recurrent.drivers.state :as state]
    [ulmus.signal :as ulmus]))

(defn Main
  [sources]
  (ulmus/subscribe! (:recurrent/state-$ sources) #(println "State is" %))
  {:recurrent/state-$ (ulmus/signal-of (fn [state] (assoc state :hello "World")))})

(defn start!
  []
  (recurrent/start!
    (state/isolate Main [:put :at])
    {:recurrent/state-$ (state/create-store! {})}))

(set! (.-onerror js/window) #(js/alert %))
