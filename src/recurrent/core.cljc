(ns recurrent.core
  (:require
    [ulmus.transaction :as transaction]
    [ulmus.signal :as signal]))

(defn- call-drivers
  [drivers]
  (map (fn [[k d]]
         (let [placeholder-$ (signal/forward)]
           [k {:driver (d placeholder-$)
               :placeholder-$ placeholder-$}]))
       drivers))

(defn- pipe-sinks!
  [placeholders sinks]
  (doseq [[k d] placeholders]
    (let [sink-$ (get sinks k)]
      (when sink-$
        (transaction/pipe!
          sink-$
          (:placeholder-$ d))))))

(defn start!
  [Component drivers & others]
  (let [placeholders
        (call-drivers drivers)

        initialized-drivers
        (into {} (map (fn [[k d]]
                        [k (:driver d)])
                      placeholders))
        sinks
        (apply Component
               initialized-drivers
               others)]

    (pipe-sinks! placeholders sinks)

    (transaction/propogate!)

    sinks))


