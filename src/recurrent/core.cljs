(ns recurrent.core
  (:refer-clojure :exclude [run!])
  (:require 
    [cljs.core.async :as async]
    [elmalike.signal :as e-sig]
    [elmalike.time :as e-time]
    [elmalike.mouse :as e-mouse]
    [recurrent.drivers.dom :as dom-driver]
    [recurrent.drivers.pixi :as pixi-driver]
    cljsjs.virtual-dom)
  (:require-macros
    [cljs.core.async.macros :as async-mac]))


(defn make-sink-proxies
  [drivers]
  (reduce (fn [sink-proxies [k v]]
            (assoc sink-proxies
                   k (e-sig/signal)))
          {} drivers))

(defn call-drivers
  [drivers sink-proxies]
  (let [sources (reduce (fn [sources [k sink]]
                          (assoc sources
                                 k ((drivers k) sink)))
                      {} sink-proxies)]
    sources))

(defn replicate-many!
  [signals proxies]
  (doseq [[k signal] signals]
    (async/pipe (async/tap 
                  (:mult signal) 
                  (async/chan (async/sliding-buffer 1) nil))
                (:ch (proxies k)))
    (when @signal
      (e-sig/on-next (proxies k) @signal))))

(defn run!
  [main drivers]
  (let [sink-proxies (make-sink-proxies drivers)
        sources (call-drivers drivers sink-proxies)
        sinks (main sources)]
    (replicate-many! sinks sink-proxies)
    {:sources sources :sinks sinks}))

(defn test-dom
  []
  (run!
    (fn [sources]
      (let [selector-fn (get-in sources [:DOM :selector-fn])
            mouse-pos$ ((selector-fn ":root") "mousemove")
            sinks {:DOM 
                   (->> (e-sig/map
                          (fn [e] [(.-clientX e) 
                                   (.-clientY e)]) mouse-pos$)
                     (e-sig/start-with [0 0])
                     (e-sig/map  
                       (fn [x]
                         [:div {:className "foo"
                                :style {:height "100px"
                                        :backgroundColor "cornflowerblue"}}
                          (str "Mouse Pos: " x)]))
                     )}]
        sinks))
    {:DOM (dom-driver/make-dom-driver "app")}))

(defn test-pixi
  []
  (run!
    (fn []
      {:PIXI
       (e-sig/foldp
         (fn [entity t]
           (js/console.log (str (get-in entity [:texture :frame])))
           (update-in entity [:texture :frame 0] inc))
         {:position [100 100]
          :scale [1 1]
          :width 32
          :height 32
          :anchor [0.5 0.5]
          :texture {:path "images/citytiles.png"
                    :frame [0 32 32 32]}
          :rotation 0}
         (e-time/fps 1))})
    {:PIXI (pixi-driver/make-pixi-driver "app")}))

(defn on-js-reload []
  (test-pixi))
