(ns com.brunobonacci.disruptions.main
  (:require [com.brunobonacci.mulog :as μ]
            [com.brunobonacci.disruptions.api :as api]
            [cemerick.url :refer [url url-encode]]
            [ring.adapter.jetty :as http])
  (:gen-class))



(def DEFAULT-CONFIG
  {:server {:port 8000 :join? false}

   :endpoints
   {:base  #(str (url "https://api.tfl.gov.uk" %))
    :roads "https://api.tfl.gov.uk/road"
    :disruptions #(str (url "https://api.tfl.gov.uk/road"
                            (url-encode %)
                            "disruption"))}


   :mulog
   {:type :multi
    :publishers
    [ ;; send events to the stdout
     {:type :console}
     ;; send events to a file
     {:type :simple-file :filename "/tmp/mulog/events.log"}
     ;; send events to ELS
     {:type :elasticsearch :url  "http://localhost:9200/"}
     ;; send events to kafka
     {:type :kafka :kafka {:bootstrap.servers "localhost:9092"}}]}})



(defn- init-events!
  [{:keys [mulog] :as config}]

  ;; set global context
  (μ/set-global-context!
   {:app-name "roads-disruption", :version "0.1.0", :env "local"})

  (μ/start-publisher! mulog))



(defn- init-polling!
  [config]
  (api/poll-disruptions! config))



(defn- init!
  [config]
  (let [_       (init-events! config)
        handler (api/service-api config)
        _       (init-polling! config)
        server  (http/run-jetty handler (:server config))]

    (fn [] (.close ^java.io.Closeable server))))



(comment

  (def config DEFAULT-CONFIG)
  (def s (init! config))

  )



(defn -main [& args]
  (init! DEFAULT-CONFIG)
  (println "Server started on http://localhost:8000/disruptions")
  @(promise))



;;
