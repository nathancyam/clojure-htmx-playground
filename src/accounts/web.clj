(ns accounts.web
  (:require
   [accounts.core :as a]
   [compojure.core :refer [defroutes GET POST]]
   [views.components :refer [csrf-token]]
   [views.response :refer [hx-response page-response]]))

(defn- login-form
  [form errors]
  [:form {:hx-post "/accounts/login" :hx-target "#main-wrapper" :hx-swap "outerHTML" :class "flex flex-col gap-4"}
   (csrf-token)
   [:div
    [:label {:for "email" :class "block text-gray-700 mb-2"} "Username"]
    [:input {:type "text" :name "email" :value (:email form) :id "email" :class "w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"}]]
   [:div
    [:label {:for "password" :class "block text-gray-700 mb-2"} "Password"]
    [:input {:type "password" :name "password" :id "password" :class "w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"}]]
   [:div
    (for [error errors]
      [:p {:class "text-red-500 text-sm mb-2"} error])]
   [:button {:type "submit" :class "w-full bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded-lg transition-colors"} "Login"]])

(defn login-page
  [form errors]
  [:div {:id "login-page" :class "max-w-md mx-auto mt-10 p-6 bg-white rounded-lg shadow-md"}
   [:h2 {:class "text-2xl font-bold mb-4 text-center"} "Login"]
   (login-form form errors)])

(defroutes routes
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
