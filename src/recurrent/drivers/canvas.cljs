(ns recurrent.drivers.canvas
  (:require [elmalike.signal :as signal]
            [monet.canvas :as canvas]))

(comment
  {:fill-color :black
   :stroke-color :none
   :shape-type :rect
   :data {:x 0
          :y 0
          :w 25
          :h 25}})

(defn render-entity!
  [ctx e]
  (when (= (:shape-type e) :image)
    (let [image-elem (.getElementById js/document (:src e))
          data (:data e)
          img-w (.-width image-elem)
          img-h (.-height image-elem)
          scale-w (/ (:w data) img-w)
          scale-h (/ (:h data) img-h)
          scale (min scale-w scale-h)
          w (* img-w scale)
          h (* img-h scale)
          half-w (/ w 2)
          half-h (/ h 2)
          x (- (:x data) half-w)
          y (- (:y data) half-h)]
      (canvas/draw-image ctx image-elem {:x x
                                         :y y
                                         :w w
                                         :h h}))))
                                        

(defn from-element
  [parent]
  (let [ctx (canvas/get-context parent "2d")]
    (set! (.-imageSmoothingEnabled ctx) true)
    (fn [entities-$]
      (signal/subscribe-next entities-$
                             (fn [entities]
                               (doseq [e entities]
                                 (render-entity! ctx e)))))))

(defn from-id
  [id]
  (from-element (.getElementById js/document id)))
