(ns recurrent.drivers.dom
  (:require
    [elmalike.signal :as e-sig]
    (cljsjs.virtual-dom)))

[:div {}
 [:h1 {} "Hello World"]
 [:p {} "quick brown fox"]]

(def VirtualDom js/virtualDom)

(defn vtree-for
  [[tag attrs & children]]
  (VirtualDom.h (name tag) 
                (clj->js attrs)
                (if (= (count children) 1)
                  (first children)
                  (clj->js (map vtree-for children)))))

(defn make-dom-driver
  [id]
  (fn [vtree$]
    (let [elem$ (e-sig/signal)
          parent (.getElementById js/document id)]

      (e-sig/subscribe-next 
        (e-sig/sliding-slice 2 (e-sig/map vtree-for vtree$))
        (fn [[curr next]]
          (when (and curr (not next))
            (let [elem (js/virtualDom.create curr)]
              (.appendChild parent elem)
              (when (not (e-sig/is-completed elem$))
                (e-sig/on-next elem$ elem))))
          (when (and curr next) 
            (let [patches (js/virtualDom.diff curr next)
                  elem (js/virtualDom.patch @elem$ patches)]
              (when (not (e-sig/is-completed elem$))
                (e-sig/on-next elem$ elem))))))
              
      {:selector-fn
       (fn [selector]
         (fn [event]
           (let [event$ (e-sig/signal)]
             (e-sig/subscribe-next 
               elem$
               (fn [elem]
                 (e-sig/on-completed elem$)
                 (let [selection 
                       (if (= ":root" selector) 
                         elem
                         (.querySelector elem selector))]
                   (.addEventListener selection
                                      event
                                      (fn [e]
                                        (e-sig/on-next event$ e))))))
             event$)))})))




