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

(defn lens 
  ([from Component] (lens from identity Component))
  ([from to Component] (lens from to :recurrent/state-$ Component))
  ([from to k Component]
   (fn [sources & args]
     (let [lensed-state-source-$
           (constantly
             (ulmus/map
               from
               ((get sources k))))
           sinks (apply Component (assoc sources k lensed-state-source-$) args)
           lensed-state-sink-$
           (ulmus/map (fn [reducer]
                        (fn [state] 
                          (to
                            state
                            (reducer (from state)))))
                      (get sinks k (ulmus/signal)))]
       (assoc sinks k lensed-state-sink-$)))))


(defn isolate
  ([scope Component] (isolate scope :recurrent/state-$ Component))
  ([scope k Component]
   (lens #(get-in % scope) #(assoc-in %1 scope %2) k Component)))

(defn- key->index
  [state key-fn k]
  (get
    (into
      {}
      (map-indexed
        (fn [idx item] [(key-fn idx item) idx])
        state))
    k))

(defn collection
  ([Component] (collection Component (fn [index _] index)))
  ([Component key-fn]
   (fn [sources & args]
     (let [items-$
           (ulmus/map
             (fn [state]
               (into 
                 {}
                 (map-indexed (fn [index item] [(key-fn index item) item]) state)))
             ((:recurrent/state-$ sources)))

           elements-$
           (ulmus/distinct
             (ulmus/reduce
               (fn [elements [gained lost]]
                 (-> 
                   (apply dissoc elements (keys lost))
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
                                     (constantly (ulmus/map #(get % k) items-$)))
                                   args)]
                             [k (assoc
                                  component
                                  :recurrent/state-$
                                  (ulmus/map
                                    (fn [reducer]
                                      (fn [state]
                                        (let [idx (key->index
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
               (ulmus/changed-keys items-$)))
           values-$ (ulmus/map vals elements-$)]
       {:elements-$
        values-$

        :recurrent/state-$
        (ulmus/pickmerge :recurrent/state-$ values-$ {:replay? false})

        :recurrent/dom-$
        (ulmus/pickzip :recurrent/dom-$ values-$)}))))



