(ns recurrent.state
  (:require
    recurrent.drivers.dom
    [clojure.set :as sets]
    [ulmus.signal :as ulmus]))

(defn isolate
  [Component scope]
  (fn [props sources]
    (let [state-source-$ (:recurrent/state-$ sources)
          sinks (Component props (assoc sources
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

(defn index-for-key
  [keyed-by k state]
  (let [state-keys (map-indexed keyed-by state)]
    (get
      (into {}
            (map-indexed (fn [idx k] [k idx]) state-keys))
      k)))

; need to attach child state to sources
; and take child state out as a sink mapping it to it's correct index.
(defn- make-child
  [Component keyed-by k]
  (fn [props sources]
    (let [component-sinks
          (Component props
                     (assoc sources
                            :recurrent/state-$
                            (ulmus/map (fn [state]
                                         (let [index (index-for-key keyed-by k state)]
                                           (get state index)))
                                       (:recurrent/state-$ sources))))]
      (assoc component-sinks
             :recurrent/state-$
             (ulmus/map
               (fn [reducer]
                 (fn [state]
                   (let [index (index-for-key keyed-by k state)]
                     (assoc state index (reducer (get state index))))))
               (:recurrent/state-$ component-sinks))))))



    ; state-$ is the full array

(defn collection-of
  [Component & opts] 
  (let [{:keys [collect keyed-by]
         :or {keyed-by (fn [index child-state] (str index))
              collect (fn [_ _ children-$] 
                        {:recurrent/dom-$
                         (ulmus/map
                           (fn [children]
                             `[:div {:class "array"} 
                               ~@children])
                           (ulmus/pickzip :recurrent/dom-$ children-$))})}}
        (apply hash-map opts)]
    (recurrent.drivers.dom/isolate
      (fn [props sources]
        (let [children-$ (ulmus/distinct 
                           (ulmus/reduce
                             (fn [children state]
                               (let [state-keys (set (map-indexed keyed-by state))
                                     existing-keys (set (keys children))
                                     new-keys (sets/difference state-keys existing-keys)
                                     lost-keys (sets/difference existing-keys state-keys)]
                                 (with-meta 
                                   (merge (apply dissoc children lost-keys)
                                          (into {} (map (fn [k] [k ((make-child
                                                                      Component
                                                                      keyed-by
                                                                      k) 
                                                                    (assoc props
                                                                           :recurrent/key k)
                                                                    sources)])
                                                        new-keys)))
                                   {:state state})))
                             {}
                             (:recurrent/state-$ sources)))
              ordered-children-$ (ulmus/map
                                   (fn [children]
                                     (let [state (:state (meta children))
                                           child-keys (map-indexed keyed-by state)]
                                       (mapv #(get children %) child-keys)))
                                   children-$)]
          (collect props sources ordered-children-$))))))
                       
