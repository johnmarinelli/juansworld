(ns juansworld.db
  (:require [clojure.string :as str]
            [juansworld.env :as env])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [clojure.string :as str]))

(defn init-firebase []
  (js/firebase.initializeApp
    env/firebase))

(defn- fb-val [snapshot]
  "Firebase returns an object whose value is retrieved with .val"
  (.val snapshot))

(defn- fb-ref [path]
  "Reference to the Firebase database.  `path` must be a vector."
  (let [path-vec (flatten [path])]
    (.ref (js/firebase.database) (str/join "/" path-vec))))


(defn- fb-get [path cb]
  "js/firebase.database.Query#once:
   once(eventType, successCallback, failureCallbackOrContext, context)
   Listens for exactly one event of the specified event type, and then stops listening."
  (.once
    (fb-ref path)
    "value"
    (comp cb fb-val)))

(init-firebase)
