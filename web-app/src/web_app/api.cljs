(ns web-app.api
  (:require [cljs.core.async :refer [timeout <! >!]]
            [cljs-http.client :as http])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def base-url "http://sandbox.api.hmhco.com/v1")
(def api-key "0708c364971bea95d01dc2fef0f6127c")
(def client-id "9aec3c24-55ed-4121-8289-76f2cd71d15d.hmhco.com")
(def client-secret "cCPObRlzqQHnou1R-s9_7UjSNTDuw2oDc90oGmappC_mAeVqKCDfvuo6txE")
(def user-key "b31eaf0f10bebab88ee1fb61f7a39954")

(defn get-token [username password]
  (go
    (let [resp (<! (http/post (str base-url "/sample_token")
                              {:form-params {"client_id" client-id
                                             "username" username
                                             "password" password}
                               :headers {"Vnd-HMH-Api-Key" user-key
                                         "Accept" "application/json"}}))]
      (prn resp)
      resp)))


(defn auth [user password]
  ;; Hardcoded!
  (go
    (or (if (= password "password")
          (case user
            "sauron" {:roles "Instructor"
                      :id "sauron"
                      :name "Sauron Baraddur"}
            "gollum" {:roles "Student"
                      :id "gollum"
                      :name "Smeagol Smeagolovitch"}
            nil))
        )))

(defn get-sections [user]
  (go
    (<! (timeout 500))
    (or (:sections user)
        [{:name "Kindergarten Spanish"
          :id "123"
          :student-usernames ["gollum"]}
         {:name "First Grade Spanish"
          :id "124"
          :student-usernames ["smaug" "bilbo" "frodo"]}
         {:name "Second Grade Spanish"
          :id "125"
          :student-usernames ["bob crachit" "tiny tim"]}])))
