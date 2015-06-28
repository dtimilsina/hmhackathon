(ns ^:figwheel-always web-app.core
    (:require [cljs.core.async :refer [<! >! chan close! put! timeout]]

              [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]

              [web-app.api :as api]
              [web-app.state :as $s :refer [make-goto]]
              [web-app.utils :as $])
    (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:view-stack nil
                          :assignments []
                          :things []}))

(defn mini-logo []
  (dom/div #js {:className "mini-logo"}
           (dom/img #js {:src "image/logo.png"})))

(defn teacher-options-view [app owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/div #js {:className "teacher-options-screen screen"}
             (mini-logo)
             (for [[opt-name opt] [["Create New Assignment" :create]
                                   ["View Assignments" :view-assignments]]]
               (dom/button #js {:className "big-button huge-button"
                                :onClick (fn [e]
                                           (om/transact! app (make-goto [opt]))
                                           ($/cancel e))}
                           opt-name))))))

(defn student-options-view [app owner]
  (reify
    om/IRender
    (render [_]
      (let [assignments ($s/student-assignments app (get-in app [:user :id]))
            grp (group-by :status assignments)
            make-as (fn [as]
                      (dom/button
                       #js {:className "big-button assignment-button"
                            :onClick (fn [e]
                                       (om/transact! app
                                                     (comp
                                                      (make-goto [:assignment as])
                                                      #(assoc % :working-on as)))
                                       ($/cancel e))}
                       (:category as)))]

        (dom/div #js {:className "student-options-screen screen"}
                 (mini-logo)
                 (apply dom/div #js {:className "assignment-group"}
                        (map make-as (:pending grp)))
                 (dom/label nil "Complete:")
                 (dom/br nil)
                 (if-let [compl (:completed grp)]
                   (apply dom/div #js {:className "assignment-group"}
                          (map make-as compl))
                   (dom/em nil "No completed assignments")))))))

(defn assignment-things-view [])

(defn assignment-view [app owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [as mode]}]
      (case mode
        (:capture, :check)
        (dom/div #js {:className "assignment-screen screen"}
                 (apply dom/div #js {:className "things"}
                        (map (fn [thing]
                               (dom/button
                                #js {:className "big-button huge-button thing-button"
                                     :onClick (fn [e]
                                                (om/transact! app
                                                              (make-goto
                                                               [:camera [as thing]])))}
                                thing))
                             (:things as))))

        (dom/div #js {:className "assignment-screen screen"}
                 (mini-logo)
                 (dom/div #js {:className "headline"}
                          (:category as))

                 (dom/button #js {:className "big-button huge-button camera-button"
                                  :onClick (fn [e]
                                             (om/set-state! owner :mode :capture))}
                             (dom/div nil (str "Capture " (:category as) " items"))
                             (dom/img #js {:src "image/camera_icon.png"}))
                 (dom/button #js {:className "big-button huge-button check-button"}
                             (dom/div nil (str "Check Translations"))
                             (dom/img #js {:src "image/white_x_circle.png"})
                             (dom/img #js {:src "image/check_rect_white.png"})))))))

(defn camera-view [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (go-loop [last nil]
        (<! (timeout 500))
        (let [file (aget (.-files ($/id "capture-field")) 0)]
          (when (not= last file)
            (let [url (<! ($/load-data-url file))]
              (om/set-state! owner :src url)))
          (recur file))))

    om/IRenderState
    (render-state [_ {:keys [as thing src]}]
      (dom/div
       #js {:className "camera-screen screen"}
       (mini-logo)
       (dom/div #js {:className "headline"} thing)
       (dom/div #js {:className "camera-area"}
                (when src
                  (dom/img #js {:src src})))
       (dom/div #js {:className "mini-menu"}
                (dom/button #js {:className "demi-button capture-button"
                                 :onClick (fn [e]
                                            (.click ($/id "capture-field")))}
                            (dom/input #js {:type "file"
                                            :accept "video/*;capture=camera"
                                            :className "hidden"
                                            :id "capture-field"})
                            (dom/img #js {:src "image/camera_icon.png"})
                            (dom/div #js {:className "detail"} "Take photo"))
                (dom/button #js {:className "demi-button upload-button"
                                 :onClick (fn [e]
                                            (.click ($/id "upload-field")))}
                            (dom/input #js {:type "file"
                                            :id "upload-field"
                                            :className "hidden"})
                            (dom/img #js {:src "image/image_upload.png"})
                            (dom/div #js {:className "detail"} "Upload photo")))
       (dom/div #js {:className "query-text"} "What's the Spanish word?")
       (dom/input #js {:type "text"
                       :className "big-input"
                       :name "spanish"})))))

(defn creation-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {})

    om/IDidUpdate
    (did-update [_ _ prev-state]
      (when (om/get-state owner :editing)
        (when-let [field ($/id "new-thing-field")]
          (.focus field)))
      (when (and (om/get-state owner :flash)
                 (:flash prev-state))
        (om/set-state! owner :flash nil)))

    om/IWillMount
    (will-mount [_]
      (when-let [user (:user @app)]
        (go
          (let [sections (<! (api/get-sections user))]
            (om/transact! app (fn [app]
                                (assoc-in app [:user :sections]
                                          sections)))))))

    om/IRenderState
    (render-state [_ {:keys [creating flash] :as state}]
      (apply dom/div #js {:className "creation-screen screen"}
             (mini-logo)
             (when flash
               (dom/div #js {:className "flash-message"}
                        flash))
             (cond
              (not (:section creating))
              (if-let [sections (get-in app [:user :sections])]
                (map (fn [section]
                       (dom/button
                        #js {:type "button"
                             :className "section-button big-button"
                             :onClick (fn [e]
                                        (om/set-state! owner :creating
                                                       {:section (:id section)
                                                        :users (:student-usernames section)})
                                        ($/cancel e))}
                        (:name section)
                        (dom/br nil)
                        (dom/div #js {:className "detail"}
                                 (let [c (count (:student-usernames section))]
                                   (str c " student" (when (not= c 1) "s"))))))
                     sections)

                [(dom/div #js {:className "loading-filler"}
                          "Loading...")])

              :default
              [(dom/div #js {:className "options"}
                        (dom/label #js {:for "due-date-field"
                                        :className "mid"}
                                   "Due")
                        (dom/input #js {:className "big-input"
                                        :id "due-date-field"
                                        :name "due-date"
                                        :type "date"})
                        (dom/label #js {:for "category-field"
                                        :className "mid"}
                                   "Category")
                        (dom/input #js {:className "text big-input"
                                        :id "category-field"
                                        :name "category"})
                        (dom/hr nil)
                        (dom/div
                         #js {:className "things"}
                         (apply dom/div
                                #js {:className "existing"}
                                (map (fn [thing]
                                       (dom/button #js {:className "big-button thing-button"}
                                                   thing))
                                     (:things creating)))

                         (if-let [t (:editing state)]
                           (dom/form
                            #js {:onSubmit (fn [e]
                                             (let [v (.-value ($/id "new-thing-field"))]
                                               (om/update-state! owner
                                                                 :creating
                                                                 (fn [c]
                                                                   (assoc c
                                                                     :things (conj
                                                                              (:things c [])
                                                                              v)))))
                                             (om/set-state! owner :editing false)
                                             ($/cancel e))
                                 :onBlur (fn [e]
                                           (let [v (.. e -target -value)]
                                             (when (empty? v)
                                               (om/set-state! owner :editing false))))}
                            (dom/input #js {:type "text"
                                            :id "new-thing-field"
                                            :className "big-input"}))

                           (dom/button #js {:className "add-button big-button"
                                            :onClick (fn [e]
                                                       (om/set-state! owner
                                                                      :editing
                                                                      true))}
                                       (dom/img #js {:src "image/add_circle.png"}))))
                        (dom/button #js {:className "big-button save-button"
                                         :onClick (fn [e]
                                                    (let [cat (.-value ($/id "category-field"))
                                                          date (.-value ($/id "due-date-field"))
                                                          things (:things creating)]
                                                      (om/transact!
                                                       app
                                                       (partial $s/save-new-assignment
                                                                {:category cat
                                                                 :date date
                                                                 :things things
                                                                 :users (:users creating)}))
                                                      (om/set-state! owner
                                                                     :creating
                                                                     nil)
                                                      (om/set-state! owner
                                                                     :flash
                                                                     "Assignment created!")))}
                                    "Save"))])))))

(defn login-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:login-chan (chan)
       :login-pending false
       :errors nil})

    om/IWillMount
    (will-mount [_]
      (let [login-chan (om/get-state owner :login-chan)]
        (go
          (loop []
            (let [creds (<! login-chan)]
              (om/set-state! owner :errors nil)
              (om/set-state! owner :login-pending true)

              (let [user-map  (<! (api/auth (:username creds)
                                            (:password creds)))]
                (om/set-state! owner :login-pending false)

                (if user-map
                  (om/transact! app (partial $s/login user-map))

                  (do
                    (om/set-state! owner :errors "Bad username or password")
                    (recur))))))
          (close! login-chan))))

    om/IDidMount
    (did-mount [_]
      (when-let [elt ($/id "login-field")]
        (.focus elt)))

    om/IRenderState
    (render-state [_ {:keys [errors login-chan login-pending]}]
      (dom/div #js {:className "login-screen"}
               (dom/form
                #js {:onSubmit (fn [e]
                                 (put! login-chan
                                       ($/make-form-data (.-target e)))
                                 ($/cancel e))}
                (dom/div #js {:className "logo"}
                         (dom/img #js {:src "image/logo.png"}))
                (when errors
                  (dom/div #js {:className "errors"}
                           errors))
                (dom/input #js {:name "username"
                                :id "login-field"
                                :type "text"
                                :placeholder "Username"})
                (dom/input #js {:name "password"
                                :type "password"
                                :placeholder "Password"})

                (dom/div #js {:className "float-bottom"}
                         (dom/input #js {:type "submit"
                                         :className "big-button login"
                                         :value "Login"})
                         (dom/input #js {:type "button"
                                         :value "Sign Up"
                                         :className "big-button sign-up"})))))))

(defn root-view [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "app-container"}
               (apply dom/div #js {:className "header"}
                        (if-let [user (:user app)]
                          [(dom/span #js {:className "user"}
                                      "Logged in as "
                                      (dom/em nil (:name user))
                                      " "
                                      ;" (" (:roles user) ") "
                                      )
                           (dom/a #js {:onClick (fn [e]
                                                  (om/transact! app $s/logout)
                                                  ($/cancel e))
                                       :href "#"}
                                  "Log Out")]

                          ["Not logged in"]))
               (let [[v p] (peek (:view-stack app))]
                 (case v
                   :teacher-home (om/build teacher-options-view app)
                   :student-home (om/build student-options-view app)
                   :create (om/build creation-view app)
                   :assignment (om/build assignment-view app {:state {:as p}})
                   :camera (om/build camera-view app {:state {:as (first p)
                                                              :thing (second p)}})
                   (om/build login-view app)))))))

(defn main []
  (om/root root-view app-state
           {:target (.getElementById js/document "app")}))

(defn init []
  (main))


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  (main)
)
