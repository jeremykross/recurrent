(ns recurrent.helpers)

(defmacro defcomponent
  [named prop-keys source-keys initializer 
   css dom-sink-key dom-sink-fn & other-sinks]
  (let [other-sinks (into {} (map vec (partition 2 other-sinks)))
        dom-key (first source-keys)]
    `(do
       (when-let [existing# (dommy.core/sel1 ~(str "." named))]
         (dommy.core/remove! (dommy.core/sel1 "#style")
                             existing#))
       (dommy.core/append!
         (dommy.core/sel1 "#style")
         (hipo.core/create
           [:style {:class ~(str named)}
            (garden.core/css ~css)]))
       (defn ~named
         [props# sources#]
         (let [scope# (gensym)
               dom-$# (recurrent.drivers.dom/isolate-source scope# (~dom-key sources#))
               props# (select-keys props# ~prop-keys)
               sources# (merge (select-keys sources# ~source-keys)
                               {~(keyword (str "global-" (name dom-key)))
                                (~dom-key sources#)}
                               {~dom-key dom-$#})
               this# (~initializer props# sources#)
               sink-placeholders# (into {~dom-sink-key (elmalike/signal)}
                                        (map (fn [[k# _#]]
                                               [k# (elmalike/signal)]) ~other-sinks))
               sinks# (merge
                        {~dom-sink-key (recurrent.drivers.dom/isolate-sink 
                                         scope#
                                         (~dom-sink-fn this# props# sources# sink-placeholders#))}
                        (into {} (map (fn [[sink-k# sink-fn#]]
                                        [sink-k# (sink-fn# this# props# sources# sink-placeholders#)]) ~other-sinks)))]

           (doseq [[k# sink#] sink-placeholders#]
             (elmalike.signal/pipe (k# sinks#) (k# sink-placeholders#)))

           sinks#)))))
