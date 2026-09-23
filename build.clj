(ns build
  "Build the production uberjar: clj -T:build uber"
  (:require [clojure.tools.build.api :as b]))

(def class-dir "target/classes")
(def uber-file "target/app.jar")
(def basis (delay (b/create-basis {:project "deps.edn"})))

(defn clean [_]
  (b/delete {:path "target"}))

(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  ;; AOT-compiling the entry point compiles everything it requires, so the
  ;; app starts without compiling Clojure source at runtime.
  (b/compile-clj {:basis @basis
                  :ns-compile '[endpoint.core]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis @basis
           :main 'endpoint.core}))
