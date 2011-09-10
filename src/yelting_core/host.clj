(ns yelting-core.host
  (:import (java.io FileWriter FileReader PushbackReader))
  (:use [yelting-achlib.parser]
        [clj-time.core :exclude 'extend]
	[fleetdb.client]))

(def client (connect))

(defn- for-account [account-number]
  {:where ["=" :account-number account-number]})

(defn- for-customer [customer-id]
  {:where ["=" :customer-id customer-id]})

(defn- add-memo [account-number amount description]
  [:insert :memos {:account-number account-number :amount amount :description description :sent-at (now)}])

(defn- account-and-memos [account-number]
  (client ([:multi-read
	    [:select :accounts (for-account account-number)]
	    [:select :memos (for-account account-number)]])))

(defn available-balance
  ([account memos]
     (reduce + (:ledger-balance account) memos))
  ([account-number]
     (let [[[account] memos] (account-and-memos account-number)]
       (reduce + (:ledger-balance account) memos))))

(defn post-memos
  ([]
     (loop [accounts (client [:select :accounts])]
       (cond
	(empty? accounts) true
	:else
	(let [result (post-memos (first accounts) (client [:select :memos (for-account (:account-number (first accounts)))]))]
	  (cond
	   (first result) (recur (rest accounts))
	   :else (recur (conj (rest accounts) (first (client [:select :accounts (for-account (:account-number (first accounts)))])))))))))
  ([[account memos]]
     (client
      [:checked-write
       [:select :accounts (for-account (:account-number account))]
       (:ledger-balance account)
       [:multi-write
	[:update :accounts {:ledger-balance (available-balance account memos)}]
	[:insert :posted-transactions (map (fn[memo] (assoc memo :posted-at (now))) memos)]
	[:delete :memos (for-account (:account-number account))]]])))

(defn post-ach [records]
  )

(defn transfer [from-account-number to-account-number amount description]
  (client [:multi-write 
	   (add-memo from-account-number (- amount) description)
	   (add-memo to-account-number amount description)]))

(defn has-memo? [account-number]
  (> 0 (client [:count :memos (for-account account-number)])))

(defn ledger-balance [account-number]
  (:ledger-balance
   (first (client [:select :accounts (for-account account-number)]))))

(defn transaction-history [account-number]
  [:select :posted-transactions (for-account account-number)])

(defn current-memos [account-number]
  [:select :memos (for-account account-number)])

(defn account-details [account-number]
  (let [[[account] memos] (account-and-memos account-number)]
    (merge account
	   {:has-memo? (not (empty? memos))
	    :available-balance (available-balance account memos)})))

(defn customer-details [customer-id]
  (first (client [:select :customers (for-customer customer-id)])))

(defn create-account [])