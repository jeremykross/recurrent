(ns recurrent.drivers.dom
  (:require
    [elmalike.signal :as e-sig]
    (cljsjs.virtual-dom)))

[:div {}
 [:h1 {} "Hello World"]
 [:p {} "quick brown fox"]]

(def VirtualDom js/virtualDom)

(defn vtree-for
  [[tag attrs & children :as element]]
  (if (and (vector? element) (keyword? tag))
    (do 
      (VirtualDom.h (name tag)
                    (clj->js attrs)
                    (clj->js (map vtree-for children))))
    (if (vector? element)
      (map vtree-for element)
      element)))

(defn from-element
  [parent]
  (fn [vtree$]
    (let [elem$ (e-sig/signal)]
      (e-sig/subscribe
        (e-sig/sliding-slice 2 vtree$)
        (fn [[curr next]]
          (when (not= curr next)
            (let [curr-vtree (vtree-for curr)
                  next-vtree (vtree-for next)]
              (when (and curr (not next))
                (let [elem (js/virtualDom.create curr-vtree)]
                  (.appendChild parent elem)
                  (when (not (e-sig/is-completed elem$))
                    (e-sig/on-next elem$ elem))))
              (when (and curr next)
                (let [patches (js/virtualDom.diff curr-vtree next-vtree)
                      elem (js/virtualDom.patch @elem$ patches)]
                  (e-sig/on-next elem$ elem))))))
        (fn []
          (js/alert "Removing!")
          (.removeChild parent @elem$))
        identity)

      (fn [selector]
        (fn [event]
          (let [event$ (e-sig/signal)
                elem-tap-$ (e-sig/tap elem$)]
            (e-sig/subscribe-next 
              elem-tap-$
              (fn [elem]
                (e-sig/on-completed elem-tap-$)
                (let [selection 
                      (if (= "root" (name selector))
                        elem
                        (.querySelector elem selector))]
                  (.addEventListener selection
                                     (name event)
                                     (fn [e]
                                       (e-sig/on-next event$ e))))))
            event$))))))

(defn from-id
  [id]
  (from-element (.getElementById js/document id)))
