(ns recurrent.core
  (:require [clojure.walk :as walk]))

(defmacro defcomponent-1
  [metadata named args & body]
  (let [sources-symbol (gensym)
        expanded-source-body (walk/prewalk (fn [f]
                                             (cond
                                               (and (list? f) (= (first f) 'recurrent/new))
                                               `(~(second f) ~sources-symbol
                                                          ~@(rest (rest f)))
                                               (and (list? f) (= (first f) '$))
                                               `((get ~sources-symbol ~(second f)) ~@(rest (rest f)))
                                               :else f))
                                           body)]
    `(do
      (declare ~named)
      (defn- ~(symbol (str "_" named))
         ~(into [] (concat [sources-symbol] args))
         (with-meta
           (do
             ~@expanded-source-body)
           {:recurrent/component ~(keyword (str (:name (:ns &env)) "/" named))}))
      (def ~named (recurrent.drivers.dom/isolate 
                    (with-meta 
                      ~(symbol (str "_" named))
                      ~metadata))))))

(defmacro defcomponent
  [named args & body]
  `(defcomponent-1 {} ~named ~args ~@body))
