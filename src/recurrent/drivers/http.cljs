(ns recurrent.drivers.http
  (:require 
    [elmalike.signal :as e-signal]
    [ajax.core :as ajax]))

;Outgoing
; { :method, :route, :body }

; Incoming
; { :method, :route, :body }

(def AJAX-METHODS {:get ajax/GET
                   :post ajax/POST})

(defn for-url
  ([url] (for-url url {}))
  ([url config]
   (fn [outgoing-$]
     (let [incoming-$ (e-signal/signal)]
       (e-signal/subscribe-next 
         outgoing-$
         (fn [outgoing]
           ((AJAX-METHODS (:method outgoing))
            (str url (:route outgoing))
            (merge {:keywords? true
                    :format :json
                    :response-format :json
                    :params (:body outgoing)
                    :handler (fn [res]
                               (e-signal/on-next 
                                 incoming-$
                                 (with-meta
                                   res
                                   {:method (:method outgoing)
                                    :route (:route outgoing)})))
                    :error-handler 
                    (fn [err]
                      (println "There was an error sending to " url " on http driver with outgoing message " outgoing ": " err))} config))))

       (fn sink 
         ([method] (sink method "/" {}))
         ([method route] (sink method route {}))
         ([method route body]
         (e-signal/on-next outgoing-$ 
                           {:method method
                            :route route
                            :body body})
         (e-signal/filter
           (fn [res] (= (meta res) {:method method
                                    :route route}))
           incoming-$)))))))
