(ns yelting-core.host
  (:import (java.io FileWriter FileReader PushbackReader))
  (:use [yelting-achlib.parser]
        [clj-time.core :exclude 'extend]))

(def customers (ref {}))
(def accounts (ref {}))

(defn- serialize [data-structure #^java.lang.String filename]
  (with-open [writer (FileWriter. filename)]
    (print-dup data-structure writer)))

(defn- deserialize [#^java.lang.String filename]
  (with-open [r (PushbackReader. (FileReader. filename))]
    (read r)))

(defn- save-accounts []
  (dosync
    (serialize @accounts "accounts.data")))

(defn load-accounts []
  (dosync
    (ref-set accounts (deserialize "accounts.data"))))

(defn- save-customers []
  (dosync
    (serialize @customers "customers.data")))

(defn save-periodically []
  (do
    (Thread/sleep 10000)
    (save-accounts)
    (save-customers)
    (recur)))

(defn load-customers []
  (dosync
    (ref-set customers (deserialize "customers.data"))))

(defn- add-memo [accounts-deref account amount description]
  (-> accounts-deref
    (update-in [account :available-balance] + amount)
    (update-in [account :memos] conj {:amount amount :description description :sent-at (now)})))

(defn post-memos
  ([]
    (dosync
      (alter accounts (fn[accounts-deref] (map post-memos accounts-deref)))))
  ([account]
    (-> account
      (update-in [:ledger-balance] + (reduce + (:memos account)))
      (update-in [:posted-transactions] into (map (fn[memo] (assoc memo :posted-at (now))) (:memos account)))
      (assoc :memos '()))))

(defn post-ach [records]
  )

(defn transfer [from-account-number to-account-number amount description]
  (dosync
    (alter accounts add-memo from-account-number (- amount) description)
    (alter accounts add-memo to-account-number amount description)))

(defn has-memo? [account-number]
  (not (empty? (:memos (@accounts account-number)))))

(defn ledger-balance [account-number]
  (dosync
    (:ledger-balance (@accounts account-number))))

(defn available-balance [account-number]
  (dosync
    (:available-balance (@accounts account-number))))

(defn transaction-history [account-number]
  (dosync
    (:posted-transactions (@accounts account-number))))

(defn current-memos [account-number]
  (dosync
    (:memos (@accounts account-number))))

(defn account-details [account-number]
  (dosync
   (let [account (@accounts account-number)]
     {:account-number account-number
      :account-type (:account-type account)
      :has-memo? (has-memo? account-number)
      :ledger-balance (ledger-balance account-number)
      :available-balance (available-balance account-number)})))

(defn customer-details [customer-id]
  (dosync
   (let [customer (@customers customer-id)]
     {:customer-id customer-id
      :legal-name (:legal-name customer)
      :tax-id (:tax-id customer)
      :is-business? (:is-business? customer)
      :accounts (:accounts customer)})))

(defn create-account [])