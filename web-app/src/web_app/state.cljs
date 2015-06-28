(ns web-app.state)

(defn make-goto [view]
  (fn [app]
    (assoc app :view-stack (conj (:view-stack app) view))))

(defn login [user-map app]
  (let [view [(case (:roles user-map)
                "Student" :assignments
                "Instructor" :create)]]
    (assoc app
      :view-stack (conj (:view-stack app) view)
      :user user-map)))

(defn logout [app]
  (dissoc app
    :view-stack
    :user))
