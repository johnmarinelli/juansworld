(ns juansworld.core
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [clojure.string :as str])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [clojure.string :as str]
            [juansworld.db :as db]
            [juansworld.helpers :as helpers]
            [juansworld.views :as views]))

(enable-console-print!)

(om/root
  views/map-view
  {}
  {:target (. js/document (getElementById "map-container"))})

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
