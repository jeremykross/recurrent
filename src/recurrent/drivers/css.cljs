(ns recurrent.drivers.css
  (:require 
    [clojure.string :as string]
    [dommy.core :as dommy :include-macros true]
    [garden.core :as garden]
    [hipo.core :as hipo]
    [ulmus.signal :as ulmus]))


(defn style-for-scope
  [styles scope]
  (first (filter (fn [style] (= (:recurrent/scope (meta style)) scope)) styles)))

(defn scopes-by-style
  [styles]
  (->> styles
       (group-by #(:recurrent/component (meta %)))
       (map (fn [[_ component-styles]]
              (let [scopes (map #(:recurrent/scope (meta %)) component-styles)]
                (group-by (partial style-for-scope component-styles) scopes))))
       (apply merge)))
       

(defn apply-scope
  [[style scopes]]
  (let [raw-key (first style)
        new-head (mapv (fn [scope] (if (not scope)
                                    raw-key
                                    (str (name raw-key) "." scope))) scopes)]
    (into new-head (rest style))))

(defn create! 
  ([] (create! (gensym)))
  ([scope]
   (fn [css-$]
     (let [style-elem (or (dommy/sel1 (str "." scope))
                          (hipo/create [:div {:class (str "recurrent-style " scope)}]))]

       (when (dommy/parent style-elem) (dommy/remove! style-elem))
       (dommy/append! (.-body js/document) style-elem)

       (ulmus/subscribe! css-$
                         (fn [styles]
                           (hipo/reconciliate! style-elem
                                               [:div {:class (str "recurrent-style " scope)}
                                                [:style 
                                                 (str
                                                   (garden/css styles))]])))))))
