(ns recurrent.drivers.dom)

(defmacro collect [dom-signal-key & components]
  `(elmalike.signal/map
     (fn [components#]
       `[:div
         ~@components#])
         (elmalike.signal/latest 
           ~@(map (fn [component-name] `(~(keyword dom-signal-key) ~component-name)) components))))
