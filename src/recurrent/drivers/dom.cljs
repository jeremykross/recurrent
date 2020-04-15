(ns recurrent.drivers.dom
  (:require
    [dumdom.core :as dumdom]
    [ulmus.core :as ulmus]
    [ulmus.transaction :as ulmus-tx]))

(defn render-into!
  [id-or-node]
  (let [parent (if (string? id-or-node)
                 (.getElementById js/document id-or-node)
                 id-or-node)]
  (fn [dom-$]
    ; Render the dom
    (ulmus-tx/subscribe!
      dom-$
      (fn [dom]
        (when dom
          (dumdom/render dom parent))))

    ; return a source creator)
    (fn [selector event]))))
