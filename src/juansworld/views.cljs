(ns juansworld.views
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [clojure.string :as str])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <! timeout]]
            [clojure.string :as str]
            [goog.events :as events]
            [juansworld.db :as db]
            [juansworld.helpers :as helpers]))

(def *map* nil)

(defn- map-load [ele map-opts]
  (set! *map* (js/google.maps.Map. ele (clj->js map-opts)))
  (.setTilt *map* 45)
  *map*)

(defn- avg-coord [coords]
  (let [coord-avg (fn [num-coords ps]
                    [(/ (get ps "lat") num-coords)
                     (/ (get ps "lng") num-coords)])
        coord+ (fn [c1 c2]
                   (let [p1 [(get c1 "lat") (get c1 "lng")]
                         p2 [(get c2 "lat") (get c2 "lng")]]
                     {"lat" (+ (first p1) (first p2))
                      "lng" (+ (second p1) (second p2))}))]
    (->> coords
         (reduce coord+ {"lat" 0 "lng" 0})
         (coord-avg (count coords)))))

(defn- geocode-lat-lng
  [geocoder coord]
  (let [location-name-ch (chan)]
    (->
      geocoder
      (.geocode
        (clj->js coord)
        (fn [res status]
          (if (= status "OK")
            (let [chosen-address (if (> (count res) 1) (second res) (first res))
                  loc-name (get (js->clj chosen-address) "formatted_address")]
              (go
                (>! location-name-ch loc-name)))
            (go
              (>! location-name-ch "n/a"))))))
    location-name-ch))

(defn- to-date-string
  [epoch]
  (let [datetime (js/Date. epoch)
        datetime-parts (-> datetime .toUTCString (.split " "))
        date-parts (take 4 datetime-parts)]
    (clojure.string/join " " date-parts)))

(defn- init-marker
  [marker-model infowindow geocoder]
  (let [coord (select-keys marker-model ["lat" "lng"])
        marker (js/google.maps.Marker. (clj->js {:position coord}))
        format-infowindow-content (fn [date loc-name desc]
                                    (str "<i>" date "</i>" "<br/>"
                                         "<b>" loc-name "</b>" "<br/>"
                                         desc))
        marker-on-click (fn []
                          (let [loc-name-recv-ch (geocode-lat-lng geocoder {:location coord})]
                            (go
                              (let [loc-name (<! loc-name-recv-ch)
                                    marker-desc (get marker-model "desc")
                                    epoch-ts (get marker-model "ts")
                                    date-string (to-date-string (* 1000 epoch-ts))
                                    infowindow-content (format-infowindow-content date-string loc-name marker-desc)]
                                (.setContent infowindow infowindow-content)
                                (.open infowindow *map* marker)))))]
    (.addListener marker "click" marker-on-click)
    marker))

(defn map-view [state owner]
  (reify
    om/IInitState
    (init-state [_]
     {:zoom 8
      :fb-chan (chan)
      :infowindow (js/google.maps.InfoWindow.)
      :geocoder (js/google.maps.Geocoder.)})

    om/IWillMount
    (will-mount [_]
      (let [{:keys [fb-chan]} (om/get-state owner)]
        (db/fb-get
          "coords"
          (fn [coords]
            (go
              (>! fb-chan coords))))))

    om/IDidMount
    (did-mount [this]
      (let [{:keys [fb-chan infowindow geocoder zoom]} (om/get-state owner)
            map-node (om/get-node owner)]
        (go
          (let [marker-models (-> (<! fb-chan) js->clj vals)
                coords (map #(select-keys % ["lat" "lng"]) marker-models)
                avg-coord (avg-coord coords)
                center {:lat (first avg-coord) :lng (second avg-coord)}]
            (map-load map-node {"zoom" zoom
                                "center" center
                                "mapTypeId" "hybrid"})
            (doseq [marker-model marker-models]
              (let [marker (init-marker marker-model infowindow geocoder)]
                (.setMap marker *map*)))))))

    om/IRenderState
    (render-state [this opts]
     (dom/div #js {:id "map"} nil))))

(defn add-coordinate-view [state owner]
  (reify
    om/IInitState
    (init-state [_] {})))
