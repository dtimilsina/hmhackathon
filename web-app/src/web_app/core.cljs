(ns ^:figwheel-always web-app.core
    (:require [cljs.core.async :refer [<! >! chan close! put!]]

              [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]

              [web-app.api :as api]
              [web-app.state :as $s :refer [make-goto]]
              [web-app.utils :as $])
    (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:view-stack nil}))

(defn creation-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {})

    om/IWillMount
    (will-mount [_]
      (when-let [user (:user @app)]
        (go
          (let [sections (<! (api/get-sections user))]
            (prn "sections:" sections)
            (om/transact! app (fn [app]
                                (assoc-in app [:user :sections]
                                          sections)))))))

    om/IRenderState
    (render-state [_ {:keys [creating] :as state}]
      (apply dom/div #js {:className "creation-screen screen"}
             (dom/div #js {:className "mini-logo"}
                      (dom/img #js {:src "image/logo.png"}))
             (cond
              (not (:section creating))
              (if-let [sections (get-in app [:user :sections])]
                (map (fn [section]
                       (dom/button
                        #js {:type "button"
                             :className "section-button big-button"
                             :onClick (fn [e]
                                        (om/set-state! owner
                                                       [:creating :section]
                                                       (:id section)))}
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
                        (prn creating)
                        (dom/label #js {:for "due-date-field"}
                                   "Due:")
                        (dom/br nil)
                        (dom/input #js {:className ""
                                        :id "due-date-field"
                                        :name "due-date"
                                        :type "date"})
                        (dom/br nil)
                        (dom/input #js {:className "text"
                                        :id "category-field"
                                        :name "category"})
                        (dom/hr nil)
                        (dom/div
                         #js {:className "things"}
                         (apply dom/div
                                #js {:className "existing"}
                                (map (fn [thing]
                                       (dom/button #js {:className "big-button added-thing"}
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
                                       (dom/img #js {:src "image/add_circle.png"})))))])))))

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
                   :create (om/build creation-view app)
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
