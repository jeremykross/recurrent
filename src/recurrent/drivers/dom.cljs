(ns recurrent.drivers.dom
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

        (let [driver
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
                  events-$))]
          (with-meta driver
                     {:recurrent/root-source driver
                      :recurrent/root-element parent})))
      {:recurrent/driver? true})))

(defn isolate
  [Component]
  (fn [sources & args]
    (let [scope (gensym)
          scoped-dom 
          (if (:recurrent/portal (meta Component))
            (:recurrent/root-source (meta (:recurrent/dom-$ sources)))
            (with-meta 
              (fn [selector event] 
                ((:recurrent/dom-$ sources)
                 (str "." scope " " (if (not= selector :root) selector) " ")
                 event))
              (meta (:recurrent/dom-$ sources))))


          component-sinks (apply Component
                                 (assoc
                                   sources
                                   :recurrent/dom-$ scoped-dom) args)]


      (assoc component-sinks
             :recurrent/dom-$
             (ulmus/map (fn [dom]
                          (let [scoped-dom-sink
                                (if (:recurrent/portal (meta Component))
                                  (rum/portal (sablono/html dom)
                                              (:recurrent/root-element (meta (:recurrent/dom-$ sources))))
                                  (sablono/html
                                    (-> dom
                                        (update-in [1 :class] (fn [class-string] (str "recurrent-component " scope " " class-string)))
                                        (assoc-in [1 :key] (str scope)))))]
                            scoped-dom-sink))
                        (ulmus/distinct (:recurrent/dom-$ component-sinks)))))))
