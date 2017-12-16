(ns recurrent.drivers.dom
  (:require
    elmalike.time
    hipo.interceptor
    [clojure.string :as string]
    [dommy.core :as dommy :include-macros true]
    [elmalike.signal :as e-sig]
    [hipo.core :as hipo]))

(defrecord DirtyInterceptor [updated]
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
      (e-sig/subscribe
        (e-sig/sliding-slice 2 vtree$)
        (fn [[curr next]]
          (when (not= curr next)
            (when (and curr (not next))
              (let [elem (hipo/create curr)]
                (set! (.-innerHTML parent) "")
                (.appendChild parent elem)
                (when (not (e-sig/is-completed elem$))
                  (e-sig/on-next elem$ [elem #{elem}]))))
            (when (and curr next)
              (let [interceptor (DirtyInterceptor. (atom #{}))
                    elem (hipo/reconciliate! (first @elem$) next 
                                             {:interceptors [interceptor]})]
                  (e-sig/on-next elem$ [(first @elem$) @(:updated interceptor)])))))
        (fn []
          (js/alert "Removing!")
          (.removeChild parent (first @elem$)))
        identity)

      (fn [selector]
        (fn [event]
          (let [event$ (e-sig/signal)
                elem-tap-$ (e-sig/tap elem$)
                callback (fn [e] 
                           (println "For selector:" selector "handling" event)
                           (.stopPropagation e)
                           (e-sig/on-next event$ e))]
            (e-sig/subscribe-next 
              elem-tap-$
              (fn [[elem updated]]
                (let [selection 
                      (if (= "root" (name selector))
                        [elem]
                        (dommy/sel (name selector)))]

                  (.forEach selection
                            (fn [s]
                              (when (contains? updated s)
                                (dommy/unlisten! s (name event) callback)
                                (dommy/listen! s (name event) callback)))))))
            event$))))))

(defn isolate-source
  [scope source]
  (fn [selector]
    (source 
      (if (not= (name selector) "root")
        (str "." scope " " selector)
        (str "." scope)))))

(defn isolate-sink
  [scope dom-$]
  (elmalike.signal/map
    (fn [dom]
      (if (map? (nth dom 1))
        (update-in dom [1 :class] (fn [classNames]
                                    (str classNames " " scope)))
        (let [[before after] (split-at 1 dom)]
          (into [] (concat (conj (into [] before) {:class scope}) after)))))
    dom-$))

(defn from-id
  [id]
  (from-element (.getElementById js/document id)))
