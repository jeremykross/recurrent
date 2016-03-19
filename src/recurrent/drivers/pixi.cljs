(ns recurrent.drivers.pixi
  (:require
    [elmalike.signal :as e-sig]
    (cljsjs.pixi)))

(defonce -instances- (atom {}))

(comment 
{:id "Player"
 :texture "path/to/texture"
 :position []
 :scale []
 :anchor []
 :rot 0 
 :width 0
 :height 0
 :children []
 }
  )

(defn copy-rect-to-pixi!
  [sprite pixi-sprite & ks]
  (doseq [k ks]
    (let [property (aget pixi-sprite (name k))
          [x y width height] (k sprite)]
      (set! (.-x property) x)
      (set! (.-y property) y)
      (set! (.-width property) width)
      (set! (.-height property) height))))


(defn copy-vec-to-pixi!
  [sprite pixi-sprite & ks]
  (doseq [k ks]
    (let [[x y] (k sprite)]
      (set! (.-x (aget pixi-sprite (name k))) x)
      (set! (.-y (aget pixi-sprite (name k))) y))))

(defn copy-scalar-to-pixi!
  [sprite pixi-sprite & ks]
  (doseq [k ks] (aset pixi-sprite (name k) (k sprite))))

(defn copy-to-pixi!
  [sprite pixi-sprite]
  (when (not (.-hasLoaded (.-baseTexture (.-texture pixi-sprite))))
    (let [texture (js/PIXI.Texture. (js/PIXI.Texture.fromImage (get-in sprite [:texture :path])))]
      (set! (.-frame texture) (js/PIXI.Rectangle.))
      (set! (.-scaleMode (.-baseTexture texture)) js/PIXI.SCALE_MODES.NEAREST)
      (set! (.-texture pixi-sprite)  texture)))
  (copy-vec-to-pixi! sprite pixi-sprite :position :anchor)
  (copy-scalar-to-pixi! sprite pixi-sprite :rotation :width :height)
  (copy-rect-to-pixi! (:texture sprite) (.-texture pixi-sprite) :frame)
  (set! (.-frame (.-texture pixi-sprite)) (.-frame (.-texture pixi-sprite))))

(defn make-pixi-driver 
  ([element-id] (make-pixi-driver element-id nil))
  ([element-id props]
  (fn [sprite$]
    (when (not (contains? @-instances- element-id))
      (let [{:keys [bg-color width height] :or {bg-color 0x6495ed width 640 height 360}} props]
        (let [element (.getElementById js/document element-id)
              stage (js/PIXI.Container.)
              renderer (js/PIXI.autoDetectRenderer width height #js {:backgroundColor bg-color})]
          ((fn render []
             (js/requestAnimationFrame render)
             (.render renderer stage)))
          (.appendChild element (.-view renderer) )
          (swap! -instances- assoc element-id stage))))
    (let [stage (@-instances- element-id)
          pixi-sprite (js/PIXI.Sprite.)]
      (.addChild stage pixi-sprite)
      (e-sig/subscribe-next sprite$
        (fn [sprite] 
          (copy-to-pixi! sprite pixi-sprite)))))))

