# Recurrent

Recurrent is an experiment in writing Reactive GUIs using FRP all the way down.

## Installation

Add `[recurrent "0.1.1-SNAPSHOT"]` to your project.clj dependencies.

## About

Components in Recurrent can be thought of as pure functions.  They take two maps.  One contains traditional data, as might be passed into a React component as `props`.  The other is special.  It contains a series of signals.  A signal is a value that can vary over time.  The value of a signal can't be assigned like a traditional variable.  Instead, the series of values the signal will take is described at the time of it's construction and is thereafter immutable.  These signals are constructed and composed with a series of familiar functional operators.

Components return a series of signals which can be fed into other components.  One of the return signals is treated specially.  It's responsible for generating a series of virtual-dom descriptions which get diffed (one by one) into the actual dom.  Each value it produces can be thought of as `setState` in the React world, triggering a re-render.

Therefore, components take some time-varying data and produce a DOM description + some other time varying data.  Components can also query their dom description using query selectors and attach events which are returned as signals of the given event over time.

## Example
This example will render whatever is input in the text-box into a container on the page.

```clojure
(recurrent/defcomponent TextInput

  ; keys expected in props and sources
  [:initial-value] [:dom-$]

  ; constructor, return value used as the value of this
  #()

  ; some css, garden style
  [:.text-input {:margin "16px"}]

  ; Our first return signal (called a sink)
  ; creates a signal of successive virutal-dom (hiccup style) to get diffed into real dom.
  :dom-$ (fn [this props sources sinks]
           (ulmus/map (fn [value]
                        [:input {:class "text-input"
                                 :type "text"
                                 :value value}])
                      (:value-$ sinks)))

  ; Another sink
  ; Keeps track of the current value
  :value-$ (fn [this props sources sinks]
             (ulmus/start-with! (:initial-value props)
               (ulmus/map
                 (fn [evt] (.-value (.-target evt)))
                 ; a special souce, available under the first key in the source list.
                 ; takes a query-selector and an event
                 ; returns a signal of occurances of the event.
                 ((:dom-$ sources) "input" "keydown")))))

(recurrent/defcomponent Label
  [] [:dom-$ :name-$]

  ; Constructor
  #()

  ; Style
  [:label {:margin-left "42px"}]

  :dom-$ (fn [_ _ sources _]
           (ulmus/map
             (fn [n]
               [:label (str "Hello, " n ".")])
             (:name-$ sources))))


(defn Main
  [props sources]
  (let [input (TextInput {:initial-value "John"} {:dom-$ (:dom-$ sources)})
        label (Label {} {:dom-$ (:dom-$ sources)
                         :name-$ (:value-$ input)})]
    {:dom-$
     (ulmus/map
       (fn [[input-dom label-dom]]
         [:div input-dom label-dom])
       (ulmus/latest (:dom-$ input) (:dom-$ label)))}))

(defn main!
  []
  (recurrent/run!
    Main
    {:dom-$ (recurrent.drivers.dom/from-id "app")}))
```

## Prior Art
Many, many, thanks to the great [cycle.js](https://github.com/cyclejs/cyclejs) which inspired this work.

## License

Copyright 2018 Jeremy Kross

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
