(ns recurrent.state
  (:require [ulmus.signal :as ulmus]))

(defn isolate
  [component scope]
  (fn [props sources]
    (let [state-source-$ (:recurrent/state-$ sources)
          sinks (component props (assoc sources
                                        :recurrent/state-$ (ulmus/map
                                                             (fn [state]
                                                               (get-in state scope))
                                                             state-source-$)))
          state-sink-$ (:recurrent/state-$ sinks)]
      (assoc sinks
             :recurrent/state-$ (ulmus/map
                                  (fn [reducer]
                                    (fn [prev-state]
                                      (assoc-in prev-state
                                                scope
                                                (reducer (get-in prev-state scope)))))
                                  state-sink-$)))))


(defn attach-reducer
  ([Component proc] 
   (fn [props state]
     (let [sinks (Component props state)]
       (assoc sinks
              :recurrent/state-$ (proc sinks)))))
  ([Component k proc] (isolate (attach-reducer Component proc) k)))

(defn with-state
  [main]
  (fn [props sources]
    (let [state-placeholder-$ (ulmus/signal)
          main-sinks (main props (assoc sources :recurrent/state-$ state-placeholder-$))
          state-$ (ulmus/distinct (ulmus/reduce (fn [prev-state proc]
                                                  (proc prev-state))
                                                nil (:recurrent/state-$ main-sinks)))]

      (ulmus/splice! state-placeholder-$ (ulmus/distinct state-$))
      main-sinks)))
