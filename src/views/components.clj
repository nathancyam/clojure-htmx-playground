(ns views.components
  (:require
   [hiccup.form :refer [hidden-field]]
   [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]))

(defn button-color [color]
  (format "bg-%s-500 hover:bg-%s-600 text-white px-6 py-2 rounded-lg transition-colors cursor-pointer" color color))

(defn csrf-token []
  (hidden-field "__anti-forgery-token" (force *anti-forgery-token*)))
