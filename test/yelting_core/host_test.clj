(ns yelting-core.host-test
  (:use yelting-core.host)
  (:use clojure.test))

(defn test-setup []
  (remove-customer "test123")
  (remove-account "99901")
  (remove-account "99902")
  (remove-account "99903")
  (remove-account-product "FCK")
  (remove-account-product "MMA")
  (remove-account-product "30F"))

(defn test-add-account-products []
  (is (= true (create-account-product "Free Checking" "FCK" "DDA" "CR")))
  (is (= true (create-account-product "Money Market Savings" "MMA" "SAV" "CR")))
  (is (= true (create-account-product "30-year Fixed Mortgage" "30F" "MTG" "DB"))))

(defn test-add-customer []
  (is (= true (create-customer "test123" "test" "bob" "test bob" "123456789" false))))

(defn test-add-accounts []
  (is (= true (create-account "test123" "99901" "FCK" 0.0M)))
  (is (= true (create-account "test123" "99902" "MMA" 0.000015M)))
  (is (= true (create-account "test123" "99903" "30F" 0.000164M))))

(defn test-is-debit-account []
  (is (= false (is-debit-account? "FCK")))
  (is (= false (is-debit-account? "MMA")))
  (is (= true (is-debit-account? "30F"))))

(defn test-get-account []
  (is (= {:available-balance 0 :has-memo? false :id "99901" :account-product "FCK" :ledger-balance 0 :daily-interest-rate 0.0M}
	 (account-details "99901")))
  (is (= {:available-balance 0 :has-memo? false :id "99902" :account-product "MMA" :ledger-balance 0 :daily-interest-rate 0.000015M}
	 (account-details "99902"))))

(defn test-get-customer []
  (is (= {:accounts ["99903" "99902" "99901"] :id "test123" :first-name "test" :last-name "bob" :legal-name "test bob" :tax-id "123456789" :is-business? false}
	 (customer-details "test123"))))

(defn test-transfer []
  (is (= true (transfer "99902" "99901" 123.45M "Test transfer"))))

(defn test-get-memos []
  (is (= true (has-memo? "99901")))
  (is (= true (has-memo? "99902")))
  (is (= [{:account-id "99901" :amount 123.45M :description "Test transfer"}] (map #(dissoc %1 :sent-at :id) (current-memos "99901"))))
  (is (= [{:account-id "99902" :amount -123.45M :description "Test transfer"}] (map #(dissoc %1 :sent-at :id) (current-memos "99902")))))

(defn test-post-memos []
  (is (= true (post-memos)))
  (is (= false (has-memo? "99901")))
  (is (= false (has-memo? "99902")))
  (is (= (ledger-balance "99901") (available-balance "99901")))
  (is (= (ledger-balance "99902") (available-balance "99902")))
  (is (= (- (ledger-balance "99902")) (ledger-balance "99901"))))

(defn test-get-history []
  (is (= [{:account-id "99901" :amount 123.45M :description "Test transfer"}] (map #(dissoc %1 :sent-at :posted-at :id) (transaction-history "99901"))))
  (is (= [{:account-id "99902" :amount -123.45M :description "Test transfer"}] (map #(dissoc %1 :sent-at :posted-at :id) (transaction-history "99902")))))

(defn test-accrued-interest []
  )

(defn test-post-interest []
  )

(defn test-post-ach []
  )

(defn test-get-image []
  )

(defn test-tran-codes []
  )

(defn test-get-statement []
  )

(defn test-post-fees []
  )

(deftest test-host
  (do
    (test-setup)
    (test-add-account-products)
    (test-add-customer)
    (test-add-accounts)
    (test-is-debit-account)
    (test-get-account)
    (test-get-customer)
    (test-transfer)
    (test-get-memos)
    (test-post-memos)
    (test-get-history)
    (test-accrued-interest)
    (test-post-interest)
    (test-post-ach)
    (test-get-image)
    (test-tran-codes)
    (test-get-statement)
    (test-post-fees)))
