(ns web-app.state)

(defn make-goto [view]
  (fn [app]
    (assoc app :view-stack (conj (:view-stack app) view))))

(defn login [user-map app]
  (let [view [(case (:roles user-map)
                "Student" :student-home
                "Instructor" :teacher-home)]]
    (assoc app
      :view-stack (conj (:view-stack app) view)
      :user user-map)))

(defn logout [app]
  (dissoc app
    :view-stack
    :user))

(defn save-new-assignment [new-assign app]
  (assoc app
    :assignments (apply conj (:assignments app [])
                        (map (fn [u]
                               (assoc (dissoc new-assign :users)
                                 :status :pending
                                 :user u))
                             (:users new-assign)))))

(defn get-section [app id]
  #_(first ))

(defn student-assignments [app username]
  (filter (fn [a]
            (= (:user a) username))
          (:assignments app)))
