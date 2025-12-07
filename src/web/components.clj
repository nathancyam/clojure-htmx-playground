(ns web.components
  (:require
   [hiccup.form :refer [hidden-field]]
   [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]))

(defn button-color [color]
  (format "bg-%s-500 hover:bg-%s-600 text-white px-6 py-2 rounded-lg transition-colors cursor-pointer" color color))

(defn button [kind text & [attrs]]
  (let [color (case kind
                :primary "blue"
                :secondary "gray"
                :success "green"
                :danger "red"
                "blue")
        class-override (:class attrs "")
        attrs (dissoc attrs :class)
        merged-attrs (merge {:class (str (button-color color) " " class-override)} attrs)]
    [:button merged-attrs text]))

(defn input-field [{:keys [name type label value]}]
  [:div
   [:label {:for name :class "block text-gray-700 mb-2"} label]
   [:input {:type type :value value :name name :class "w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"}]])

(defn csrf-token []
  (hidden-field "__anti-forgery-token" (force *anti-forgery-token*)))

(defn hx-csrf-header [attrs]
  (merge attrs {:hx-headers (str "\"x-csrf-token\": \"" (force *anti-forgery-token*) "\"")}))
