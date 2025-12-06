(ns views.components)

(defn button-color [color]
  (format "bg-%s-500 hover:bg-%s-600 text-white px-6 py-2 rounded-lg transition-colors cursor-pointer" color color))
