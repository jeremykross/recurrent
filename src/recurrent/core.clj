(ns recurrent.core)

(defmacro defcomponent
  [named args & body]
  `(do
    (declare ~named)
    (defn- ~(symbol (str "_" named))
       ~args
       (with-meta
         (do
           ~@body)
         {:recurrent/component ~(keyword (str (:name (:ns &env)) "/" named))}))
    (def ~named (recurrent.drivers.rum/isolate ~(symbol (str "_" named))))))
