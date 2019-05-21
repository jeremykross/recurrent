(ns recurrent.drivers.state
  (:require
    [ulmus.signal :as ulmus]))

(defn apply-reduction
  [state reducer]
  (reducer state))

(defn create-store!
  [initial-value]
  (with-meta
    (fn [reductions-$]
      (let [state-$
            (ulmus/reduce apply-reduction
                          initial-value reductions-$)]
        state-$))
    {:recurrent/driver? true}))

(defn isolate
  ([Component scope] (isolate Component scope :recurrent/state-$))
  ([Component scope k]
   (fn [sources & args]
     (let [isolated-state-source-$ (ulmus/map (fn [state] (get-in state scope)) (get sources k))
           sinks (apply Component (assoc sources k isolated-state-source-$) args)
           isolated-state-sink-$ (ulmus/map (fn [reducer] (fn [state] 
                                                            (assoc-in 
                                                              state
                                                              scope
                                                              (reducer (get-in state scope))))) (get sinks k))]
       (assoc sinks k isolated-state-sink-$)))))
