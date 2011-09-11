(ns yelting-core.host
  (:import (java.io FileWriter FileReader PushbackReader))
  (:use [yelting-achlib.parser]
        [clj-time.core :exclude 'extend]
	[clj-time.coerce :only (to-string from-string)]
	[fleetdb.client]))

(def client (connect))

(defn guid [] (.toString (java.util.UUID/randomUUID)))

(defn- for-account [account-id]
  {:where ["=" :id account-id]})

(defn- for-customer [customer-id]
  {:where ["=" :id customer-id]})

(defn- add-memo [account-id amount description]
  [:insert :memos {:id (guid) :account-id account-id :amount amount :description description :sent-at (to-string (now))}])

(defn- account-and-memos [account-id]
  (client [:multi-read
	   [[:select :accounts (for-account account-id)]
	    [:select :memos {:where ["=" :account-id account-id]}]]]))

(defn available-balance
  ([account memos]
     (reduce + (:ledger-balance account) (map :amount memos)))
  ([account-id]
     (let [[[account] memos] (account-and-memos account-id)]
       (reduce + (:ledger-balance account) memos))))

(defn current-memos [account-id]
  (client [:select :memos {:where ["=" :account-id account-id]}]))

(defn post-memos
  ([]
     (loop [accounts (client [:select :accounts])]
       (cond
	(empty? accounts) true
	:else
	(let [result (post-memos
		      (first accounts)
		      (current-memos (:id (first accounts))))]
	  (cond
	   (first result) (recur (rest accounts))
	   :else (recur
		  (conj
		   (rest accounts)
		   (first
		    (client [:select :accounts
			     (for-account (:id (first accounts)))])))))))))
  ([account memos]
     (client
      [:checked-write
       [:select :accounts {:where ["=" :id (:id account)]
			   :only :ledger-balance}]
       [(:ledger-balance account)]
       [:multi-write
	[[:update :accounts
	  {:ledger-balance (available-balance account memos)}
	  (for-account (:id account))]
	 [:insert :posted-transactions
	  (map (fn[memo] (assoc memo :posted-at (to-string (now)))) memos)]
	 [:delete :memos
	  {:where ["=" :account-id (:id account)]}]]]])))

(defn post-ach [records]
  )

(defn transfer [from-account-id to-account-id amount description]
  (= [1 1] (client [:multi-write 
		    [(add-memo from-account-id (- amount) description)
		     (add-memo to-account-id amount description)]])))

(defn has-memo? [account-id]
  (> (client [:count :memos {:where ["=" :account-id account-id]}]) 0))

(defn ledger-balance [account-id]
  (:ledger-balance
   (first (client [:select :accounts (for-account account-id)]))))

(defn transaction-history [account-id]
  (client [:select :posted-transactions {:where ["=" :account-id account-id]}]))

(defn account-details [account-id]
  (let [[[account] memos] (account-and-memos account-id)]
    (merge account
	   {:has-memo? (not (empty? memos))
	    :available-balance (available-balance account memos)})))

(defn customer-details [customer-id]
  (let [customer (first (client [:select :customers (for-customer customer-id)]))
	accounts (client [:select :customer-accounts {:where ["=" :customer-id customer-id]}])]
    (assoc customer :accounts (vec (map :account-id accounts)))))

(defn create-customer [customer-id first-name last-name legal-name tax-id is-business?]
  (= 1 (client [:insert :customers
		{:id customer-id :first-name first-name :last-name last-name
		 :legal-name legal-name :tax-id tax-id :is-business? is-business?}])))

(defn create-account [customer-id account-id account-product]
  (= [1 1] (client [:multi-write
		    [[:insert :accounts
		      {:id account-id :account-product account-product :ledger-balance 0}]
		     [:insert :customer-accounts
		      {:customer-id customer-id :account-id account-id :id (guid)}]]])))

(defn remove-customer [customer-id]
  (client [:multi-write
	   [[:delete :customer-accounts {:where ["=" :customer-id customer-id]}]
	    [:delete :customers (for-customer customer-id)]]]))

(defn remove-account [account-id]
  (client [:multi-write
	   [[:delete :customer-accounts {:where ["=" :account-id account-id]}]
	    [:delete :memos {:where ["=" :account-id account-id]}]
	    [:delete :posted-transactions {:where ["=" :account-id account-id]}]
	    [:delete :accounts (for-account account-id)]]]))

(defn is-debit-account? [account-product]
  (= "DB" (:type (first (client [:select :account-products {:where ["=" :id account-product]}])))))

(defn create-account-product [account-product-name account-product account-subtype account-type]
  (= 1 (client [:insert :account-products {:id account-product :name account-product-name :type account-type :subtype account-subtype}])))

(defn remove-account-product [account-product]
  (client [:delete :account-products {:where ["=" :id account-product]}]))
