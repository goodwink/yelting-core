(ns yelting-core.main
  (:gen-class)
  (:use [yelting-core.interface.amqp]
	[clojure.core]))

(defn -main []
  (start-interface))