(ns accounts.handler
  (:require
   [clojure.string :as str]
   [compojure.core :refer [defroutes GET POST]]
   [views.pages :refer [layout]]
   [views.util :refer [html-response]]))

(defn login-page [csrf-token]
  (let [token (-> csrf-token
                  (str/replace "&" "&amp;")
                  (str/replace "\"" "&quot;")
                  (str/replace "<" "&lt;"))]
    [:div {:id "login-page" :class "max-w-md mx-auto mt-10 p-6 bg-white rounded-lg shadow-md"}
     [:h2 {:class "text-2xl font-bold mb-4 text-center"} "Login"]
     [:form {:hx-post "/accounts/login" :hx-target "#main-wrapper" :hx-swap "outerHTML"}
      [:input {:type "hidden" :name "__anti-forgery-token" :value token}]
      [:div {:class "mb-4"}
       [:label {:for "username" :class "block text-gray-700 mb-2"} "Username"]
       [:input {:type "text" :name "username" :id "username" :class "w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"}]]
      [:div {:class "mb-6"}
       [:label {:for "password" :class "block text-gray-700 mb-2"} "Password"]
       [:input {:type "password" :name "password" :id "password" :class "w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"}]]
      [:button {:type "submit" :class "w-full bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded-lg transition-colors"} "Login"]]]))

(defroutes routes
  (POST "/login" [username password]
    (prn username)
    (prn password)
    (html-response (layout
                    [:div {:class "max-w-md mx-auto mt-10 p-6 bg-white rounded-lg shadow-md"}
                     [:h2 {:class "text-2xl font-bold mb-4 text-center"} "Login Successful"]
                     [:p {:class "text-center text-gray-700"} "Welcome back!"]])))

  (GET "/login" request
    (prn request)
    (html-response (layout (login-page (:anti-forgery-token request))))))
