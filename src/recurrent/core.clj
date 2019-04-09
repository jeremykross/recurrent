(ns recurrent.core)

(defmacro defcomponent-1
  [metadata named args & body]
  `(do
    (declare ~named)
    (defn- ~(symbol (str "_" named))
       ~args
       (with-meta
         (do
           ~@body)
         {:recurrent/component ~(keyword (str (:name (:ns &env)) "/" named))}))
    (def ~named (recurrent.drivers.rum/isolate 
                  (with-meta 
                    ~(symbol (str "_" named))
                    ~metadata)))))

(defmacro defcomponent
  [named args & body]
  `(defcomponent-1 {} ~named ~args ~@body))
