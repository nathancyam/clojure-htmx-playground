(ns accounts.web
  (:require
   [accounts.core :as a]
   [ring.util.response :refer [response redirect]]
   [compojure.core :refer [defroutes GET POST]]
   [web.components :refer [csrf-token button input-field]]))

(defn- login-form
  [form errors]
  [:form {:hx-post "/accounts/login" :class "flex flex-col gap-4"}
   (csrf-token)
   (input-field {:name "login[email]" :type "text" :label "Email" :value (:email form)})
   (input-field {:name "login[password]" :type "password" :label "Password" :value nil})
   [:div
    (for [error errors]
      [:p {:class "text-red-500 text-sm mb-2"} error])]
   (button :primary "Login")
   [:a {:hx-get "/accounts/register" :hx-push-url "true" :class "text-blue-500 hover:underline text-center mt-2"} "Don't have an account? Register"]])

(defn- login-page
  [form errors]
  [:div {:id "login-page" :hx-target "#main-wrapper" :class "max-w-md mx-auto mt-10 p-6 bg-white rounded-lg shadow-md"}
   [:h2 {:class "text-2xl font-bold mb-4 text-center"} "Login"]
   (login-form form errors)])

(defn- register-page
  [form errors]
  [:div {:id "register-page" :class "max-w-md mx-auto mt-10 p-6 bg-white rounded-lg shadow-md"}
   [:h2 {:class "text-2xl font-bold mb-4 text-center"} "Register"]
   [:form {:hx-post "/accounts/register" :hx-target "#main-wrapper" :hx-swap "outerHTML" :class "flex flex-col gap-4"}
    (csrf-token)
    (input-field {:name "register[email]" :type "text" :label "Email" :value (:email form)})
    (input-field {:name "register[password]" :type "password" :label "Password" :value nil})
    (input-field {:name "register[confirm-password]" :type "password" :label "Confirm Password" :value nil})
    [:div
     (for [error errors]
       [:p {:class "text-red-500 text-sm mb-2"} error])]
    (button :primary "Register")]])

(defn wrap-user-session [handler]
  (fn [{:keys [db session] :as request}]
    (let [user (when-let [session-token (:token session nil)]
                 (when-let [token (some-> (java.util.Base64/getDecoder)
                                          (.decode session-token))]
                   (a/get-user-by-session-token db token)))]
      (handler (assoc request :current-user user)))))

(defroutes routes
  (GET "/dashboard" {render :renderer current-user :current-user}
    (if current-user
      (response (render [:div {:class "max-w-md mx-auto mt-10 p-6 bg-white rounded-lg shadow-md"}
                         [:h2 {:class "text-2xl font-bold mb-4 text-center"} "Dashboard"]
                         [:p {:class "text-center text-gray-700"} (str "Welcome, " (:users/email current-user) "!")]]))
      (redirect "/accounts/login")))

  (GET "/register" [:as {render :renderer}]
    (response (render (register-page {} []))))

  (POST "/register" [register :as {db :db render :renderer}]
    (let [{:keys [email password confirm-password]} register]
      (if (not= password confirm-password)
        (render (register-page {:email email} ["Passwords do not match"]))
        (try
          (a/create-user! db {:email email :password password})
          (redirect "/accounts/login")
          (catch Exception _
            (response (render (register-page {:email email} ["Email already in use"]))))))))

  (GET "/login" {render :renderer current-user :current-user}
    (if current-user
      (redirect "/accounts/dashboard")
      (response (render (login-page {} [])))))

  (POST "/login" [login :as {db :db render :renderer session :session}]
    (let [{:keys [email password]} login]
      (try
        (let [{:keys [token]} (a/authenticate! db email password)
              regenerated-session (vary-meta session assoc :recreate true)
              encoded-token (-> (java.util.Base64/getEncoder)
                                (.encode token)
                                (String.))
              auth-session (assoc regenerated-session :token encoded-token)]
          (-> (response (render [:div {:class "max-w-md mx-auto mt-10 p-6 bg-white rounded-lg shadow-md"}
                                 [:h2 {:class "text-2xl font-bold mb-4 text-center"} "Login Successful"]
                                 [:p {:class "text-center text-gray-700"} "Welcome back!"]]))
              (assoc :session auth-session)))
        (catch Exception ex
          (response (render (login-page {:email email} [(.getMessage ex)]))))))))

