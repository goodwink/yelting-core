(ns yelting-core.host
  (:import (java.io FileWriter FileReader PushbackReader))
  (:use [yelting-achlib.parser]
        [clj-time.core :exclude 'extend]
	[clj-time.coerce :only (to-string from-string)]
	[fleetdb.client]))

(def client (connect))

(defn guid [] (.toString (java.util.UUID/randomUUID)))

(defn- for-account [account-number]
  {:where ["=" :account-number account-number]})

(defn- for-customer [customer-id]
  {:where ["=" :customer-id customer-id]})

(defn- add-memo [account-number amount description]
  [:insert :memos {:id (guid) :account-number account-number :amount amount :description description :sent-at (to-string (now))}])

(defn- account-and-memos [account-number]
  (client [:multi-read
	   [[:select :accounts (for-account account-number)]
	    [:select :memos (for-account account-number)]]]))

(defn available-balance
  ([account memos]
     (reduce + (:ledger-balance account) (map :amount memos)))
  ([account-number]
     (let [[[account] memos] (account-and-memos account-number)]
       (reduce + (:ledger-balance account) memos))))

(defn current-memos [account-number]
  (client [:select :memos (for-account account-number)]))

(defn post-memos
  ([]
     (loop [accounts (client [:select :accounts])]
       (cond
	(empty? accounts) true
	:else
	(let [result (post-memos
		      (first accounts)
		      (current-memos (:account-number (first accounts))))]
	  (cond
	   (first result) (recur (rest accounts))
	   :else (recur
		  (conj
		   (rest accounts)
		   (first
		    (client [:select :accounts
			     (for-account (:account-number (first accounts)))])))))))))
  ([account memos]
     (client
      [:checked-write
       [:select :accounts {:where ["=" :account-number (:account-number account)]
			   :only :ledger-balance}]
       [(:ledger-balance account)]
       [:multi-write
	[[:update :accounts
	  {:ledger-balance (available-balance account memos)}
	  (for-account (:account-number account))]
	 [:insert :posted-transactions
	  (map (fn[memo] (assoc memo :posted-at (to-string (now)) :id (guid))) memos)]
	 [:delete :memos
	  (for-account (:account-number account))]]]])))

(defn post-ach [records]
  )

(defn transfer [from-account-number to-account-number amount description]
  (client [:multi-write 
	   [(add-memo from-account-number (- amount) description)
	    (add-memo to-account-number amount description)]]))

(defn has-memo? [account-number]
  (> 0 (client [:count :memos (for-account account-number)])))

(defn ledger-balance [account-number]
  (:ledger-balance
   (first (client [:select :accounts (for-account account-number)]))))

(defn transaction-history [account-number]
  (client [:select :posted-transactions (for-account account-number)]))

(defn account-details [account-number]
  (let [[[account] memos] (account-and-memos account-number)]
    (merge account
	   {:has-memo? (not (empty? memos))
	    :available-balance (available-balance account memos)})))

(defn customer-details [customer-id]
  (let [customer (first (client [:select :customers (for-customer customer-id)]))
	accounts (client [:select :customer-accounts (for-customer customer-id)])]
    (assoc customer :accounts accounts)))

(defn create-customer [customer-id first-name last-name legal-name tax-id is-business?]
  (client [:insert :customers
	   {:customer-id customer-id :first-name first-name :last-name last-name
	    :legal-name legal-name :tax-id tax-id :is-business? is-business? :id (guid)}]))

(defn create-account [customer-id account-number account-type]
  (client [:multi-write
	   [[:insert :accounts
	     {:account-number account-number :account-type account-type :ledger-balance 0 :id (guid)}]
	    [:insert :customer-accounts
	     {:customer-id customer-id :account-number account-number :id (guid)}]]]))