(ns recurrent.drivers.dom
  (:require
    [clojure.string :as string]
    [dommy.core :as dommy :include-macros true]
    [elmalike.signal :as e-sig]
    [hipo.core :as hipo]))

(defn from-element
  [parent]
  (fn [vtree$]
    (let [elem$ (e-sig/signal)]
      (e-sig/subscribe
        (e-sig/sliding-slice 2 vtree$)
        (fn [[curr next]]
          (when (not= curr next)
            (when (and curr (not next))
              (println curr)
              (let [elem (hipo/create curr)]
                (set! (.-innerHTML parent) "")
                (.appendChild parent elem)
                (when (not (e-sig/is-completed elem$))
                  (e-sig/on-next elem$ elem))))
            (when (and curr next)
              (let [elem (hipo/reconciliate! @elem$ next)]
                (e-sig/on-next elem$ @elem$)))))
        (fn []
          (js/alert "Removing!")
          (.removeChild parent @elem$))
        identity)

      (fn [selector]
        (fn [event]
          (let [event$ (e-sig/signal)
                elem-tap-$ (e-sig/tap elem$)
                callback (fn [e] 
                           (when e 
                             (e-sig/on-next event$ e)))]
            (e-sig/subscribe-next 
              elem-tap-$
              (fn [elem]
                (let [selection 
                      (if (= "root" (name selector))
                        elem
                        [elem (name selector)])]
                  (println "Attaching to: " selection " for " event)
                  (dommy/unlisten! selection (name event) callback)
                  (dommy/listen! selection (name event) callback))))
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
