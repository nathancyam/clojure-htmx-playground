(ns accounts.web
  (:require
   [accounts.core :as a]
   [compojure.core :refer [defroutes GET POST]]
   [views.components :refer [csrf-token button input-field]]
   [views.response :refer [hx-response page-response]]))

(defn- login-form
  [form errors]
  [:form {:hx-post "/accounts/login" :class "flex flex-col gap-4"}
   (csrf-token)
   (input-field {:name "email" :type "text" :label "Email" :value (:email form)})
   (input-field {:name "password" :type "password" :label "Password" :value nil})
   [:div
    (for [error errors]
      [:p {:class "text-red-500 text-sm mb-2"} error])]
   (button :primary "Login")
   [:a {:hx-get "/accounts/register" :class "text-blue-500 hover:underline text-center mt-2"} "Don't have an account? Register"]])

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
    (input-field {:name "email" :type "text" :label "Email" :value (:email form)})
    (input-field {:name "password" :type "password" :label "Password" :value nil})
    (input-field {:name "confirm-password" :type "password" :label "Confirm Password" :value nil})
    [:div
     (for [error errors]
       [:p {:class "text-red-500 text-sm mb-2"} error])]
    (button :primary "Register")]])

(defroutes routes
  (GET "/register" [:as {render :renderer}]
    (render (register-page {} [])))

  (POST "/login" [email password :as {db :db}]
    (try
      (a/authenticate! db email password)
      (page-response [:div {:class "max-w-md mx-auto mt-10 p-6 bg-white rounded-lg shadow-md"}
                      [:h2 {:class "text-2xl font-bold mb-4 text-center"} "Login Successful"]
                      [:p {:class "text-center text-gray-700"} "Welcome back!"]])
      (catch Exception _
        (hx-response (login-page {:email email} ["Invalid user or password"])))))

  (GET "/login" []
    (page-response (login-page {} []))))
