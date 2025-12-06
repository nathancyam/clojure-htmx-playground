(ns accounts.web
  (:require
   [compojure.core :refer [defroutes GET POST]]
   [views.components :refer [csrf-token]]
   [views.response :refer [page-response]]))

(defn login-page []
  [:div {:id "login-page" :class "max-w-md mx-auto mt-10 p-6 bg-white rounded-lg shadow-md"}
   [:h2 {:class "text-2xl font-bold mb-4 text-center"} "Login"]
   [:form {:hx-post "/accounts/login" :hx-target "#main-wrapper" :hx-swap "outerHTML"}
    (csrf-token)
    [:div {:class "mb-4"}
     [:label {:for "username" :class "block text-gray-700 mb-2"} "Username"]
     [:input {:type "text" :name "username" :id "username" :class "w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"}]]
    [:div {:class "mb-6"}
     [:label {:for "password" :class "block text-gray-700 mb-2"} "Password"]
     [:input {:type "password" :name "password" :id "password" :class "w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"}]]
    [:button {:type "submit" :class "w-full bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded-lg transition-colors"} "Login"]]])

(defroutes routes
  (POST "/login" [username password]
    (prn username)
    (prn password)
    (page-response [:div {:class "max-w-md mx-auto mt-10 p-6 bg-white rounded-lg shadow-md"}
                    [:h2 {:class "text-2xl font-bold mb-4 text-center"} "Login Successful"]
                    [:p {:class "text-center text-gray-700"} "Welcome back!"]]))

  (GET "/login" request
    (prn request)
    (page-response (login-page))))
