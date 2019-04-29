(ns recurrent.core
  (:require
    [clojure.string :as string]
    [clojure.walk :as walk]))

(defmacro legacy
  [& body]
  (let [alt-body (walk/prewalk (fn [form]
                                 (cond
                                   (and
                                     (list? form)
                                     (symbol? (first form))
                                     (re-find #"^[A-Z]" (name (first form))))
                                   `(~'recurrent/new ~(first form) ~@(rest form))

                                   (and
                                     (list? form)
                                     (list? (first form))
                                     (= 'sources (second (first form))))
                                   (let [driver-name (first (first form))
                                         args (subvec (vec form) 1)]
                                     `(~'$ ~driver-name ~@args))
                                   :else form))
                               body)]
    alt-body))

(defmacro defcomponent-1
  [metadata named args & body]
  (let [sources-symbol (gensym)
        expanded-source-body (walk/prewalk (fn [f]
                                             (cond
                                               (and (list? f) (= (first f) 'recurrent/new))
                                               `(~(second f) ~sources-symbol
                                                          ~@(rest (rest f)))
                                               (and 
                                                 (list? f)
                                                 (= (first f) '$))
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
