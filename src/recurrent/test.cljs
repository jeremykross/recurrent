(ns recurrent.test
  (:require
    recurrent.drivers.dom
    [recurrent.core :as recurrent :include-macros true]
    [ulmus.signal :as ulmus]))

(recurrent/defcomponent
  Bar
  []
  {:recurrent/dom-$ (ulmus/signal-of [:div {} "Howdy partner"])})

(recurrent/defcomponent
  Main
  [n]
  (let [clicks-$ (ulmus/reduce inc 0 ($ :recurrent/dom-$ :root "click"))]
    {:recurrent/dom-$
     (ulmus/map
       (fn [cnt] (println cnt) [:div {} (str "Click Count: " cnt)]) clicks-$)}))

(recurrent/legacy
(recurrent/defcomponent
  LegacyMain
  [props sources]
  (let [clicks-$ (ulmus/reduce inc 0 ((:recurrent/dom-$ sources) :root "click"))]
    {:recurrent/dom-$
     (ulmus/map
       (fn [cnt] (println cnt) [:div {} (str "Click Count: " cnt)]) clicks-$)})))

(defn start!
  []
  (recurrent/start!
    LegacyMain
    {:recurrent/dom-$
     (recurrent.drivers.dom/create! "app")}
    "Jeremy"))
