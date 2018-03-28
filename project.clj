(defproject recurrent "0.1.0-SNAPSHOT"
  :description "Reactive programming implemented as a kinder layer over core.async."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/core.async "0.3.443"]
                 [cljs-ajax "0.6.0"]
                 [prismatic/dommy "1.1.0"]
                 [garden "1.3.3"]
                 [hipo "0.5.2"]
                 [ulmus "0.1.0-SNAPSHOT"]]

  :plugins [[lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]
            [lein-codox "0.10.3"]]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]
                :compiler {:output-to "target/js/recurrent.js"
                           :output-dir "target/js/out"}}
               {:id "min"
                :source-paths ["src"]
                :compiler {:output-to "target/js/recurrent.js"
                           :optimizations "advanced"}}]}
  :codox {:output-path "docs"
          :language :clojurescript
          :metadata {:doc/format :markdown}})

