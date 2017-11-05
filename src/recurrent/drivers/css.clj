(ns recurrent.drivers.css)

(defmacro collect [css-signal-key & components]
  `(elmalike.signal/map identity
                 (elmalike.signal/latest 
                   ~@(map (fn [component-name] `(~(keyword css-signal-key) ~component-name)) components))))
