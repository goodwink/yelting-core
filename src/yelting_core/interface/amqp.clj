(ns yelting-core.interface.amqp
  (:use [yelting-core.host]
	[com.mefesto.wabbitmq]
	[clojure.data.json :only (json-str read-json)]))

(defn- handle-message [headers message]
  (let [fields (read-json message)
	type (.toString (:message-type headers))]
    (case type
	  "account-inquiry" (account-details (:from-account fields))
	  "transfer" (transfer (:from-account fields) (:to-account fields) (:amount fields) (:description fields))
	  "memo-inquiry" (current-memos (:from-account fields))
	  "history-inquiry" (transaction-history (:from-account fields))
	  "customer-inquiry" (customer-details (:customer-id fields))
	  {:error "Message type is not supported"})))

(defn- process-incoming-message [from headers message]
  (with-exchange "yelting.banking.core.interface.amqp.exchange"
    (publish from (.getBytes (json-str (handle-message headers message))))))

(defn- setup-exchange []
  (with-broker {:host "localhost"}
    (with-channel
      (exchange-declare "yelting.banking.core.interface.amqp.exchange" "direct" true)
      (queue-declare "core.queue")
      (queue-bind "core.queue" "yelting.banking.core.interface.amqp.exchange" "core"))))

(def num-consumers 5)

(defn- consumer []
  (with-channel
    (with-queue "core.queue"
      (doseq [msg (consuming-seq true)]
	(process-incoming-message (:reply-to (:props msg)) (:headers (:props msg)) (String. (:body msg)))))))

(defn start-interface []
  (do
    (setup-exchange)
    (with-broker {:host "localhost"}
      (invoke-consumers num-consumers consumer))))