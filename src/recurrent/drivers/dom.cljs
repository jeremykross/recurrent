(ns recurrent.drivers.dom
  (:require
    [elmalike.signal :as e-sig]
    [dommy.core :as dommy :include-macros true]
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
                callback (fn [e] (when e (e-sig/on-next event$ e)))]
            (e-sig/subscribe-next 
              elem-tap-$
              (fn [elem]
                (let [selection 
                      (if (= "root" (name selector))
                        [elem]
                        (dommy/sel [elem (name selector)]))]
                  (doseq [s selection]
                    (.removeEventListener s (name event) callback)
                    (.addEventListener s (name event) callback)))))
            event$))))))

(defn from-id
  [id]
  (from-element (.getElementById js/document id)))
