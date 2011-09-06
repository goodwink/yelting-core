(ns yelting-core.main
  (:gen-class)
  (:use [yelting-core.host]
	[yelting-core.interface.amqp]
	[clojure.core]))

(defn -main []
  (do
    (future-call load-accounts)
    (future-call load-customers)
    (future-call save-periodically)
    (start-interface)))