(ns accounts.web
  (:require
   [accounts.core :as a]
   [compojure.core :refer [defroutes GET POST]]
   [web.components :refer [csrf-token button input-field]]
   [web.response :refer [page-response]]))

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

(defroutes routes
  (GET "/register" [:as {render :renderer}]
    (render (register-page {} [])))

  (POST "/register" [register :as {db :db render :renderer}]
    (let [{:keys [email password confirm-password]} register]
      (prn register)
      (if (not= password confirm-password)
        (render (register-page {:email email} ["Passwords do not match"]))
        (try
          (a/create-user! db {:email email :password password})
          (render [:div {:class "max-w-md mx-auto mt-10 p-6 bg-white rounded-lg shadow-md"}
                   [:h2 {:class "text-2xl font-bold mb-4 text-center"} "Registration Successful"]
                   [:p {:class "text-center text-gray-700"} "You can now log in with your new account."]])
          (catch Exception _
            (render (register-page {:email email} ["Email already in use"])))))))

  (POST "/login" [login :as {db :db render :renderer}]
    (let [{:keys [email password]} login]
      (try
        (a/authenticate! db email password)
        (page-response [:div {:class "max-w-md mx-auto mt-10 p-6 bg-white rounded-lg shadow-md"}
                        [:h2 {:class "text-2xl font-bold mb-4 text-center"} "Login Successful"]
                        [:p {:class "text-center text-gray-700"} "Welcome back!"]])
        (catch Exception ex
          (prn ex)
          (render (login-page {:email email} [(.getMessage ex)]))))))

  (GET "/login" []
    (page-response (login-page {} []))))
