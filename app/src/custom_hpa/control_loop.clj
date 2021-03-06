(ns custom-hpa.control-loop
  (:require [taoensso.timbre :as logger]
            [clojure.core.async :refer [go-loop <! timeout]]
            [iapetos.core :as prometheus]
            [custom-hpa.helpers.env :refer [int-env]]
            [custom-hpa.control-loop.period :as period]
            [custom-hpa.monitor.prometheus :refer [registry]]
            [custom-hpa.clients.kube :as kube]))

(def ^:private period-ms (delay (* 1000 (int-env "CONTROL_LOOP_PERIOD"))))

(defn start [deployment deployment-namespace metric-provider]
  (logger/info "Starting control loop every" @period-ms "milliseconds, deployment =" deployment ", namespace = " deployment-namespace)
  (let [kube-client (kube/init)]
    (go-loop []
      (try
        (prometheus/inc (registry :custom-hpa/up))
        (period/run metric-provider kube-client deployment deployment-namespace)
        (catch Exception e
          (logger/error e "Exception was thrown during control loop period"))
        (finally
          (<! (timeout @period-ms))))
      (recur))))