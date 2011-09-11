(ns yelting-core.host-test
  (:use yelting-core.host)
  (:use clojure.test))

(defn test-setup []
  (remove-customer "test123")
  (remove-account "99901")
  (remove-account "99902"))

(defn test-add-customer []
  (is (= true (create-customer "test123" "test" "bob" "test bob" "123456789" false))))

(defn test-add-accounts []
  (is (= true (create-account "test123" "99901" "DDA")))
  (is (= true (create-account "test123" "99902" "SAV"))))

(defn test-get-account []
  (is (= {:available-balance 0 :has-memo? false :id "99901" :account-type "DDA" :ledger-balance 0}
	 (account-details "99901")))
  (is (= {:available-balance 0 :has-memo? false :id "99902" :account-type "SAV" :ledger-balance 0}
	 (account-details "99902"))))

(defn test-get-customer []
  (is (= {:accounts ["99902" "99901"] :id "test123" :first-name "test" :last-name "bob" :legal-name "test bob" :tax-id "123456789" :is-business? false}
	 (customer-details "test123"))))

(defn test-transfer []
  (is (= true (transfer "99902" "99901" 12345 "Test transfer"))))

(defn test-get-memos []
  (is (= true (has-memo? "99901")))
  (is (= true (has-memo? "99902")))
  (is (= [{:account-id "99901" :amount 12345 :description "Test transfer"}] (map #(dissoc %1 :sent-at :id) (current-memos "99901"))))
  (is (= [{:account-id "99902" :amount -12345 :description "Test transfer"}] (map #(dissoc %1 :sent-at :id) (current-memos "99902")))))

(defn test-post-memos []
  (is (= true (post-memos)))
  (is (= false (has-memo? "99901")))
  (is (= false (has-memo? "99902")))
  (is (= (ledger-balance "99901") (available-balance "99901")))
  (is (= (ledger-balance "99902") (available-balance "99902")))
  (is (= (- (ledger-balance "99902")) (ledger-balance "99901"))))

(defn test-get-history []
  (is (= [{:account-id "99901" :amount 12345 :description "Test transfer"}] (map #(dissoc %1 :sent-at :posted-at :id) (transaction-history "99901"))))
  (is (= [{:account-id "99902" :amount -12345 :description "Test transfer"}] (map #(dissoc %1 :sent-at :posted-at :id) (transaction-history "99902")))))

(deftest test-host
  (do
    (test-setup)
    (test-add-customer)
    (test-add-accounts)
    (test-get-account)
    (test-get-customer)
    (test-transfer)
    (test-get-memos)
    (test-post-memos)
    (test-get-history)))
