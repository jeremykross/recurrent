(ns recurrent.drivers.vdom
  (:require 
    ulmus.time
    [clojure.walk :as walk]
    [dommy.core :as dommy :include-macros true]
    [vdom.core :as vdom]
    [ulmus.signal :as ulmus]))

(defn- class->className 
  [x]
  (walk/prewalk 
    (fn [form]
      (if (and (vector? form)
               (map? (second form))
               (:class (second form)))
        (update form 1 (fn [attr] (-> attr
                                      (dissoc :class)
                                      (assoc :className (:class attr)))))
        form))
    x))

(defn create!
  [parent]
  (set! (.-innerHTML parent) "")
  (set! (.-requestAnimationFrame js/window) nil)

  (with-meta
    (fn [vdom-$]
      (let [elem-$ (ulmus/signal)
            elem-delay-$ (ulmus.time/delay 0 elem-$)
            render! (vdom/renderer parent)]

        (ulmus/subscribe! vdom-$
          (fn [vdom]
            (let [sanitized (class->className vdom)]
              (render! sanitized)
              (ulmus/>! elem-$ parent))))

        (fn [selector event]
          (let [events-$ (ulmus/signal)
                handler (fn [e]
                          (ulmus/>! events-$ e))
                sub
                (ulmus/subscribe! 
                  elem-delay-$
                  (fn [elem]
                    (doseq [e (dommy/sel elem selector)]
                      (dommy/unlisten! e event handler)
                      (dommy/listen! e event handler))))]

            (ulmus/subscribe-closed!
              events-$
              (fn []
                (ulmus/unsubscribe!
                  elem-delay-$ 
                  sub)))

            events-$))))
    {:recurrent/driver? true}))

(defn for-id!
  [id]
  (create!
    (.getElementById js/document id)))

(defn isolate
  [Component]
  (fn [props sources]
    (let [scope (gensym)
          scoped-dom (fn [selector event] 
                       ((:recurrent/dom-$ sources)
                        (str "." scope " " (if (not= selector :root) selector) " ")
                        event))
          component-sinks (Component props (assoc sources
                                                  :recurrent/dom-$ scoped-dom))]
      (assoc component-sinks
             :recurrent/dom-$ (ulmus/map (fn [dom]
                                           (-> dom
                                               (update-in [1 :class] (fn [class-string] (str "recurrent-component " scope " " class-string)))
                                               (assoc-in [1 :key] (str scope))))
                                         (:recurrent/dom-$ component-sinks))))))
