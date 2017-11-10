(ns recurrent.helpers)

(defmacro collect-markup-and-style
  [dom-key css-key & components]
  `{~css-key (recurrent.drivers.css/collect ~css-key
                                            ~@components)
    ~dom-key (recurrent.drivers.dom/collect ~dom-key
                                            ~@components)})


(defmacro defcomponent
  [named source-keys initializer dom-sink-key dom-sink-fn & other-sinks]
  (let [other-sinks (into {} (map vec (partition 2 other-sinks)))
        dom-key (first source-keys)]
    `(defn ~named
       [props# sources#]
       (let [scope# (gensym)
             this# (~initializer props# sources#)
             dom-$# (recurrent.drivers.dom/isolate-source scope# (~dom-key sources#))
             sources# (merge (select-keys sources# ~source-keys)
                             {~dom-key dom-$#})
             sink-placeholders# (into {} (map (fn [[k# _#]]
                                    [k# (elmalike/signal)]) ~other-sinks))
             sinks# (merge
                      {~dom-sink-key (recurrent.drivers.dom/isolate-sink 
                                       scope#
                                       (~dom-sink-fn this# props# sources# sink-placeholders#))}
                      (into {} (map (fn [[sink-k# sink-fn#]]
                                      [sink-k# (sink-fn# this# props# sources# sink-placeholders#)]) ~other-sinks)))]

         (doseq [[k# sink#] sink-placeholders#]
           (elmalike.signal/pipe (k# sinks#) (k# sink-placeholders#)))

         sinks#))))
