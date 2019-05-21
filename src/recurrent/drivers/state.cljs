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
            (ulmus/distinct
              (ulmus/reduce apply-reduction
                            initial-value reductions-$))]
        (constantly state-$)))
    {:recurrent/driver? true}))

(defn isolate
  ([scope Component] (isolate scope :recurrent/state-$ Component))
  ([scope k Component]
   (fn [sources & args]
     (let [isolated-state-source-$ (constantly (ulmus/map (fn [state] (get-in state scope)) ((get sources k))))
           sinks (apply Component (assoc sources k isolated-state-source-$) args)
           isolated-state-sink-$
           (ulmus/map (fn [reducer]
                        (fn [state] 
                          (assoc-in 
                            state
                            scope
                            (reducer (get-in state scope)))))
                      (get sinks k (ulmus/signal)))]
       (assoc sinks k isolated-state-sink-$)))))

(defn- index-for-key
  [state key-fn k]
  (get
    (into
      {}
      (map-indexed
        (fn [idx item] [(key-fn item) idx])
        state))
    k))

(defn collection
  ([Component] (collection Component identity))
  ([Component key-fn]
   (fn [sources & args]
     (let [items-$
           (ulmus/map
             (fn [state]
               (into 
                 {}
                 (map (fn [item] [(key-fn item) item]) state)))
             ((:recurrent/state-$ sources)))

           elements-$
           (ulmus/reduce
             (fn [elements [gained lost]]
               (-> 
                 (apply disj elements (keys lost))
                 (merge
                   (into
                     {}
                     (map
                       (fn [[k]]
                         (let [component
                               (apply
                                 Component
                                 (assoc
                                   sources
                                   :recurrent/state-$
                                   (constantly (ulmus/map k items-$)))
                                 args)]
                           [k (assoc
                                component
                                :recurrent/state-$
                                (ulmus/map
                                  (fn [reducer]
                                    (fn [state]
                                      (let [idx (index-for-key
                                                  state
                                                  key-fn
                                                  k)]
                                        (assoc
                                          state
                                          idx
                                          (reducer
                                            (get
                                              state
                                              idx))))))
                                  (or
                                    (:recurrent/state-$ component)
                                    (ulmus/signal))))]))
                           gained)))))
             {}
             (ulmus/changed-keys items-$))
           values-$ (ulmus/map vals elements-$)]
       {:recurrent/state-$
        (ulmus/pickmerge :recurrent/state-$ values-$)
        :recurrent/dom-$
        (ulmus/pickzip :recurrent/dom-$ values-$)}))))



