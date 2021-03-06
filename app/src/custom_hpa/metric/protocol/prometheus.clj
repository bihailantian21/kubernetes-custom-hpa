(ns custom-hpa.metric.protocol.prometheus
  (:require [custom-hpa.metric.protocol.provider :refer [Provider]]
            [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [taoensso.timbre :as logger]))

(defn- parse-double [val] (Double/parseDouble val))

(defn- extract-sample [body]
  (-> body
      :data
      :result
      first
      :value
      second
      parse-double))

(defn- response-ok?
  [status error body]
  (and (= 200 status)
       (nil? error)
       (= "success" (:status body))))

(defrecord Prometheus [host query]
  Provider
  (fetch [this]
    (let [{:keys [status error body]} @(http/get host {:timeout 1000 :query-params {:query query}})
          parsed-body (when body (json/read-str body :key-fn keyword))]
      (logger/debug "Got" status "response from prometheus:" parsed-body)
      (when (response-ok? status error parsed-body)
        (extract-sample parsed-body)))))