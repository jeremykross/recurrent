(ns recurrent.drivers.dom
  (:require
    hipo.interceptor
    [clojure.string :as string]
    [dommy.core :as dommy :include-macros true]
    [ulmus.core :as e-sig]
    [hipo.core :as hipo]))

(defrecord ^{:private true} DirtyInterceptor [updated]
  hipo.interceptor/Interceptor
  (-intercept [_ t m f]
    (when 
      (not= (:hipo/key (meta (:new-value m)))
            (:hipo/key (meta (:old-value m))))
      (let [dommy-listeners (or (.-dommyEventListeners (:target m)) {})
            listeners-sans-selector (or (dommy-listeners nil) {})]
        (doseq [[evt fn-mapping] listeners-sans-selector]
          (doseq [[f _] fn-mapping]
            (dommy/unlisten! (:target m) evt f)))))

    (when (not= (:new-value m) (:old-value m))
      (swap! updated conj (:target m)))
    (f)))

(defn from-element
  [parent]
  (fn [vtree$]
    (let [elem$ (e-sig/signal)]
      (e-sig/subscribe!
        (e-sig/latest
          (e-sig/sliding-slice 2 vtree$)
          (e-sig/count vtree$))
        (fn [[[curr next] update-count]]
          (when (not= curr next)
            (when (and curr (not next))
              (let [elem (hipo/create curr)]
                (set! (.-innerHTML parent) "")
                (.appendChild parent elem)
                (when (not (e-sig/is-completed? elem$))
                  (e-sig/next! elem$ [elem #{elem}]))))
            (when (and curr next)
              (let [interceptor (DirtyInterceptor. (atom #{}))
                    elem (hipo/reconciliate! (first @elem$) next 
                                             {:interceptors [interceptor]})]
                  (e-sig/next! elem$ [(first @elem$) @(:updated interceptor) update-count])))))
        (fn []
          (.removeChild parent (first @elem$)))
        identity)

      (fn [selector]
        (fn [event]
          (let [event$ (e-sig/signal)
                elem-tap-$ (e-sig/map identity elem$)
                callback (fn [e] 
                           (.stopPropagation e)
                           (e-sig/next! event$ e))]
            (e-sig/subscribe-next!
              elem-tap-$
              (fn [[elem updated update-count]]
                (let [selection 
                      (if (= "root" (name selector))
                        [elem]
                        (dommy/sel (name selector)))]

                  (.forEach selection
                            (fn [s]
                              (when (or (contains? updated s) (= update-count 1))
                                (dommy/unlisten! s (name event) callback)
                                (dommy/listen! s (name event) callback)))))))
            event$))))))

(defn from-id
  [id]
  (from-element (.getElementById js/document id)))

(defn- isolate-source
  [scope source]
  (fn [selector]
    (source 
      (if (not= (name selector) "root")
        (str "." scope " " selector)
        (str "." scope)))))

(defn- isolate-sink
  [scope dom-$]
  (e-sig/map
    (fn [dom]
      (if (and dom (>= (count dom) 2) (map? (nth dom 1)))
        (update-in dom [1 :class] (fn [classNames]
                                    (str classNames " " scope)))
        (let [[before after] (split-at 1 dom)]
          (into [] (concat (conj (into [] before) {:class scope}) after)))))
    dom-$))

