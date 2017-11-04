(ns recurrent.drivers.css
  (:require 
    recurrent.core
    recurrent.drivers.dom
    [garden.core :as garden]
    [elmalike.signal :as elmalike]))

(defn from-element
  [parent]
  (let [style-placeholder-$ (elmalike/signal)]

    (recurrent.core/run!
      (fn []
        {:dom-$
         (elmalike/map
           (fn [style]
             `[:style ~(garden/css style)])
           style-placeholder-$)})
      {:dom-$ (recurrent.drivers.dom/from-element parent)})

    (fn [style-$]
      (elmalike/pipe style-$ style-placeholder-$)
      (fn [style]
        (elmalike/signal-of style)))))

(defn from-id
  [id]
  (from-element (.getElementById js/document id)))

