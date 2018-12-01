(ns recurrent.drivers.http
  (:require [cljs-http.client :as http]
            [cljs.core.async :as async]
            [ulmus.signal :as ulmus])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn create!
  ([base-path] (create! base-path {}))
  ([base-path default-params]
  (fn [req-$]
    (let [res-$ (ulmus/signal)]
      (ulmus/subscribe! req-$
        (fn [[verb path params]]
          (let [verb-proc ({:get http/get
                            :post http/post} verb)]
            (go
              (let [res (async/<! (verb-proc (str base-path path) (merge default-params params)))]
                (ulmus/>! res-$ {:verb verb :path path :res res}))))))
      (fn [[verb path]]
        (let [res-for-$ (ulmus/signal)]
          (ulmus/subscribe! res-$
                            (fn [res]
                              (when (and (= (:verb res) verb) (= (:path res) path))
                                (ulmus/>! res-for-$ (:body (:res res))))))
          res-for-$))))))

