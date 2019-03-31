(ns recurrent.drivers.rum
  (:require
    ulmus.time
    [dommy.core :as dommy :include-macros true]
    [rum.core :as rum :include-macros true]
    [sablono.core :as sablono :include-macros true]
    [ulmus.signal :as ulmus]))

(def recurrent-mixin
  {:did-mount (fn [state]
                (let [component (:rum/react-component state)
                      dom-$ (first (:rum/args state))]
                  (ulmus/subscribe!
                    dom-$
                    #(rum/request-render component))))})

(rum/defc render [dom] dom)
(rum/defc portal [dom] (rum/portal dom (.-body js/document)))
(rum/defc embed < recurrent-mixin [dom-$] @dom-$)

(def blocked-events* (atom {}))


(defn create!
  [parent-or-id]
  (let [parent (if (string? parent-or-id)
                 (.getElementById js/document parent-or-id)
                 parent-or-id)
        elem-$ (ulmus/signal)
        elem-delay-$ (ulmus.time/frame elem-$)]
    (rum/unmount parent)
    (set! (.-innerHTML parent) "")

    ; stopPropagation on document to stop Reacts event system
    (doseq [e ["keyup"]]
      (.addEventListener js/document e (fn [e] (.stopPropagation e))))

    (with-meta 
      (fn [vdom-$]
        (ulmus/subscribe! vdom-$
          (fn [vdom]
            (rum/mount (render vdom) parent)

            ; attach some metadata to parent about what properties have changed!
            ; selectively unlisten/listen!
            (ulmus/>! elem-$ parent)))
        (fn [selector event]

          (when (not (get @blocked-events* event))
            (swap! blocked-events*
                   assoc
                   event (.addEventListener parent event (fn [e] 
                                                           (.stopPropagation e)))))

          (let [events-$ (ulmus/signal)
                handler #(ulmus/>! events-$ %)]
            (ulmus/subscribe!
              elem-delay-$
              (fn [elem]
                (doseq [e (dommy/sel elem selector)]
                  (dommy/unlisten! e event handler)
                  (dommy/listen! e event handler))))
            events-$)))
      {:recurrent/driver? true})))

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
             :recurrent/dom-$
             (ulmus/map (fn [dom]
                          (sablono/html
                            (-> dom
                                (update-in [1 :class] (fn [class-string] (str "recurrent-component " scope " " class-string)))
                                (assoc-in [1 :key] (str scope)))))
                        (ulmus/distinct (:recurrent/dom-$ component-sinks)))))))
