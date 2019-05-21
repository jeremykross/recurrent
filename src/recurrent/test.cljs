(ns recurrent.test
  (:require
    [recurrent.core :as recurrent :include-macros true]
    [recurrent.drivers.dom :as dom]
    [recurrent.drivers.state :as state]
    [ulmus.signal :as ulmus]))

(recurrent/defcomponent
  Element
  []
  {:recurrent/dom-$
   (ulmus/map 
     (fn [item]
       [:div {:class "item"} (str (name item))])
     ($ :recurrent/state-$))})

(recurrent/defcomponent Main
  []
  {:recurrent/dom-$ 
   (ulmus/map
     (fn [elements]
       `[:div {} ~@elements])
     (:recurrent/dom-$
       (! (state/collection Element))))})

(defn start!
  []
  (recurrent/start!
    (state/isolate [:array] Main)
    {:recurrent/dom-$
     (dom/create! "app")
     :recurrent/state-$
     (state/create-store! {:array [:foo :bar :baz]})}))

;(set! (.-onerror js/window) #(js/alert %))
