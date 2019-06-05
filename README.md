# Recurrent

## Table of Contents
- [Introduction](#introduction)
- [Components](#components)
- [Sources](#sources)
- [Instantation](#instantiation)
- [Drivers and the Main Loop](#drivers-and-the-main-loop)
- [Examples](#examples)
- [Status](#status)
- [Roadmap](#roadmap)
- [License](#license)


### Introduction

Recurrent is a UI library for the web.  It's highly influenced by the likes of React and other v-dom based libraries.  Whereas React has one point of reactivity (state -> UI), Recurrent is deeply reactive throughout.

### Usage

`[recurrent "0.2.1"]` in `project.clj`

or

`{:deps {recurrent {:mvn/version "0.2.1"}}}` in `deps.edn`

### Components

Components in Recurrent are functions of data.  A special data type is defined, called a signal, which represents a value that changes over time.  Signals are composed using the standard functional tooling (`map`, `filter`, `reduce`, et al.).  These functions yield derived signals that will update when their constituent signals change.  Many readers will recognize this as the functional reactive style of programming (FRP).  The FRP layer in Recurrent is currently handled by [Ulmus](https://github.com/jeremykross/ulmus).

Components return maps with, at minimum, a signal keyed under `:recurrent/dom-$`.  This signal represents the component's hiccup formatted DOM.

The simplest component looks something like this:

```clojure
(require [recurrent.core :as recurrent :include-macros true])
(require [ulmus.signal :as ulmus])

(recurrent/defcomponent Hello
  [] ; No arguments
  {:recurrent/dom-$ (ulmus/signal-of [:h1 {:class "hello" :style {:color "green"}} "Hello World"])})
```

`defcomponent` is conceptually similar to `defn`.  It takes a vector of arbitary arguments, and must return an object with a signal at `recurrent/dom-$` and potentially other data.  In this case the signal isn't reactive.  It's displays "hello world" in a header.

But becuase `:recurrent/dom-$` is a signal, it needn't be static.  Here we take a signal as an argument which drives the value displayed.

```clojure
(recurrent/defcomponent Hello
  [the-name-$]
  {:recurrent/dom-$ (ulmus/map (fn [the-name] [:h1 {} (str "Hello " the-name)]) the-name-$)})
```

We map over a signal, `the-name-$`, which provides the name to be printed.  Any time `the-name-$` changes a new dom object will be emitted and the component will rerender. 

### Sources

Components are provided with "sources" that allow the user to generate new signals, usually from some external data.  One of the most fundamental sources is used to generate signals from events on the components DOM.  Let's see how this works.  In this example we're going to display a button along with a label indicating how many times the button has been clicked.

```clojure
(recurrent/defcomponent ButtonClickCount
  []
  (let [count-$ (ulmus/reduce inc 0 ($ :recurrent/dom-$ "button" "click"))]
    {:recurrent/dom-$
      (ulmus/map
        (fn [count]
          [:div {:class "button-click-count"}
            [:button {} "Click Me!"]
            [:p {} (str "You've clicked " count " times.")]]) count-$)}))
```

Here you can see we create a signal called `count-$`.  `count-$` is a reduction over `inc` starting at 0.

```clojure
($ :recurrent/dom-$ "button" "click")
```

generates a signal of events that occur when the button in the component is clicked.

`$` is a special symbol used within the context of a `defcomponent` to access sources.  The first argument is the name of the source, in this case `:recurrent/dom-$`.  The addititional arguments are provided to the source for generating the signal.  The `:recurrent/dom-$` source takes a CSS selector (`"button"` here), and an event, in this case `"click"`.  Interestingly, we can generate this signal even before the dom is rendered.  Additionally, components can only general signals for events sourced from inside their own DOM.  We'll see where sources come from shortly.

### Instantiation

Let's imagine we have a text input component.

```clojure
(recurrent/defcomponent Input
  [initial-value]
  (let [value-$ 
        (ulmus/start-with!
          initial-value
          (ulmus/map
            (fn [e] 
              (.-value (.-target e)))
            ($ :recurrent/dom-$ ".my-input" "input")))]

    {:value-$ value-$

     :recurrent/dom-$
     (ulmus/map
       (fn [v] 
         [:div {}
          [:input {:class "my-input" :type "text" :defaultValue initial-value}]])
       value-$)}))
```

Here, we've defined a component `Input` that generates a signal `value-$` from it's `oninput` event looking at the `target.value` of the event object.  We may want to use this component within other components.  There's another special symbol (in addition to `$`) for instantiation.  To create an instance of the input we do something like this

```clojure
(let [input (! Input "FooBar")])
```

Now `input` is a map with the `:recurrent/dom-$` and `:value-$` keys defined in the componet.  This is essentially a function invocation.  The first argument to `!` is the component to be instantiated, followed by the arguments the component accepts.  Above, `initial-value` is set to `"FooBar"`.

Here's a more complete example.

```clojure
(recurrent/defcomponent Main
  [message]
  (let [input (! Input "Partner")]
    {:recurrent/dom-$
     (ulmus/map
       (fn [[input-dom input-value]]
         [:div {}
          input-dom
          [:br]
          (str message " " input-value)])
       (ulmus/zip
         (:recurrent/dom-$ input)
         (:value-$ input)))}))
```

### Drivers and the Main Loop

So far we've seen how sources allow for the creation of signals within components from external sources (like DOM events).  We've also seen how components can return signals which are used to mutate some external source (i.e., the way the `:recurrent/dom-$` signal changes the actual DOM).  These changes are mediated by drivers.  The aforementioned facilities come from the dom driver included with Recurrent.

To start a recurrent reconciliation loop, and provide drivers to our components, we use the function `recurrent.core/start!`.  `start!` take the Component to instantiate, a map of drivers, and any additional arguments to the component.  Here's an example:

```clojure
(defn main!
  []
  (recurrent/start!
    Main
    {:recurrent/dom-$
     (recurrent.drivers.dom/create! "app")}))
```

This will instantiate the `Main` component (defined above actually), and render the dom returned on `:recurrent/dom-$` into the div with the id "app". 

Drivers' sources can be accessed at the key at which they are instantiated here, i.e. we now have access to `($ :recurrent/dom-$)` within `Main` and any other components instantiated from `Main`.  The dom driver should always be instantiated at the `:recurrent/dom-$` key, although that limitation maybe removed in the future.

Recurrent ships with three drivers by default, `dom`, `state`, and `http`.

### Examples

Recurrent was used to build [Konstellate](https://containership.github.io/konstellate).  More information on Konstellate can be found at [konstellate.io](https://konstellate.io) or it's [repo](https://github.com/containership/konstellate).

A handful of examples can be found in the [recurrent-examples repo](https://github.com/jeremykross/recurrent-examples) and running [here](https://jeremykross.github.io/recurrent-examples).


### Status

Recurrent is beta quality and shouldn't be relied upon yet for mission critical applications.

### Roadmap

#### Short Term

* Documentation
  * HTTP Driver
  * State Driver
  * More examples
* Spec
* Potentially move drivers into seperate repos
* Add helper funcs/macros for common patterns
* Performance & bug fixes

#### Longer Term
* Investigate alternative v-dom implementations
* Abstract the FRP implementation

### Thanks

Recurrent was largely inspired by [Cycle.js](https://cycle.js.org/).

### License

Copyright 2019 Jeremy Kross

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
