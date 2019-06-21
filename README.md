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

Recurrent is a UI library for the web.  It's influenced by the likes of React and other v-dom based libraries.  Whereas React has one point of reactivity (state -> UI), Recurrent is deeply reactive throughout.

### Usage

```clojure
[recurrent "0.2.1"]
```

or

```clojure
{recurrent {:mvn/version "0.2.1"}}
```

### Components

Components in Recurrent are functions of data.  They're built using a special data type, called a signal, which represents a value that changes over time.  These are composed using the standard functional tooling (`map`, `filter`, `reduce`, et al.).  These functions yield derived signals that will update when their constituent signals change.  Many readers will recognize this as the functional reactive style of programming (FRP).  The FRP layer in Recurrent is currently handled by [Ulmus](https://github.com/jeremykross/ulmus).

The simplest component looks something like this:

```clojure
(require [recurrent.core :as recurrent :include-macros true])
(require [ulmus.signal :as ulmus])

(recurrent/defcomponent Hello
  [] ; No arguments
  {:recurrent/dom-$
   (ulmus/signal-of
     [:h1 {} "Hello World"])})
```

`defcomponent` is conceptually similar to `defn`.  It takes a vector of arbitary arguments. The return value must be a map with a signal at `recurrent/dom-$` and potentially other data.  The `:recurrent/dom-$` signal represents the component's [Hiccup](https://github.com/weavejester/hiccup) formatted DOM.  In this case the signal isn't reactive.  It's displays "hello world" in a header.

But becuase `:recurrent/dom-$` is a signal, it needn't be static.  Here we take a signal as an argument which drives the value displayed.

```clojure
(recurrent/defcomponent Hello
  [the-name-$]
  {:recurrent/dom-$
   (ulmus/map
     (fn [the-name]
       [:h1 {} (str "Hello " the-name)])
     the-name-$)})
```

We map over a signal, `the-name-$`, which provides the name to be printed.  Any time `the-name-$` changes a new dom object will be emitted and the component will rerender. 

### Sources

Components are provided with "sources" that allow the user to generate new signals based on external data.  The DOM source is used to generate signals from events on the component's DOM.  Let's see how this works.  In the following example, we're going to display a button and a label indicating how many times the button has been clicked.

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

Here you can see we create a signal called `count-$`.  `count-$` is a reduction against `inc` starting at 0.

```clojure
($ :recurrent/dom-$ "button" "click")
```

generates a signal of events that occur when the button in the component is clicked.

`$` is a special symbol used within the context of a `defcomponent` to access sources.  The first argument is the name of the source, in this case `:recurrent/dom-$`.  The addititional arguments are provided to the source for generating the signal.

The `:recurrent/dom-$` source takes a CSS selector (`"button"` here), and an event, in this case `"click"`.  Components can only general signals for events sourced from inside their own DOM.

We'll see where sources come from shortly.

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

The `Input` component returns a signal `value-$`.  This is built from it's `oninput` event looking at `target.value` in the event object.  We may want to use this component from within others.  There's another special symbol, `!`, used for instantiation.  To create an instance of the input we do something like this

```clojure
(let [input (! Input "FooBar")])
```

`input` is a map with the `:recurrent/dom-$` and `:value-$` keys defined in the componet.  The component to be instantiated is the first argument to `!`.  Additional arguments will be passed to the component.  Above, `initial-value` is set to `"FooBar"`.

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

So far we've seen how sources allow for the creation of new signals from external sources (like DOM events).  We've also seen how components can mutate external sources by returning signals (i.e., the way the `:recurrent/dom-$` signal changes the actual DOM).  These changes are mediated by drivers.  The aforementioned facilities come from the dom driver included with Recurrent.

To start a recurrent reconciliation loop, and provide drivers to our components, we use the function `recurrent.core/start!`.  `start!`'s arguments are the Component to instantiate, a map of drivers, followed by any additional arguments to the component.  Here's an example:

```clojure
(defn main!
  []
  (recurrent/start!
    Main
    {:recurrent/dom-$
     (recurrent.drivers.dom/create! "app")}))
```

This will instantiate the `Main` component.  The dom emitted on the signal at `:recurrent/dom-$` will be rendered into the div with id of "app".

Drivers' sources can be accessed at the key at which they are instantiated.  i.e. we now have access to `($ :recurrent/dom-$)` within `Main` and any other components instantiated from `Main`.  The dom driver should always be instantiated at the `:recurrent/dom-$` key, although that limitation may be removed in the future.

Recurrent ships with three drivers by default, `dom`, `state`, and `http`.

### Examples

Recurrent was used to build [Konstellate](https://containership.github.io/konstellate).  More information on Konstellate can be found at [konstellate.io](https://konstellate.io) or it's [repo](https://github.com/containership/konstellate).

A handful of examples can be found in the [recurrent-examples repo](https://github.com/jeremykross/recurrent-examples) and running [here](https://jeremykross.github.io/recurrent-examples).


### Status

Recurrent is beta quality and shouldn't be relied upon yet for mission critical applications.

### Roadmap

#### Short Term

* Documentation
  * Docstring and API
  * HTTP Driver
  * State Driver
  * Creating new drivers
  * More examples
* Spec
* Performance & bug fixes

#### Longer Term
* Potentially move drivers into seperate repos
* Investigate alternative v-dom implementations
* Abstract the FRP implementation
* Add helper funcs/macros for common patterns

### Thanks

Recurrent was largely inspired by [Cycle.js](https://cycle.js.org/).

### License

Copyright 2019 Jeremy Kross

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
