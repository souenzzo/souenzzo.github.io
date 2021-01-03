(ns drawing.main
  (:require [goog.dom :as gdom]))

;; https://tmcw.github.io/literate-raytracer/
(def width (* 0.5 640))
(def height (* 0.5 480))

(defn v-subtract
  [a b]
  {:x (- (:x a) (:x b))
   :y (- (:y a) (:y b))
   :z (- (:z a) (:z b))})
(defn v-dot-product
  [a b]
  (+ (* (:x a) (:x b))
     (* (:y a) (:y b))
     (* (:z a) (:z b))))

(defn v-length
  [a]
  (Math/sqrt (v-dot-product a a)))

(defn v-scale
  [a t]
  {:x (* t (:x a))
   :y (* t (:y a))
   :z (* t (:z a))})

(def v-up
  {:x 0
   :y 0
   :z 0})
(defn v-unit-vector
  [a]
  (v-scale a (/ 1 (v-length a))))
(defn v-cross-product
  [a b])

(defonce canvas
         (doto (gdom/getElement "drawing")
           (set! -width width)                              ;
           (set! -height height)))                          ;))

(defonce context (.getContext canvas "2d"))
(defonce image-data (.getImageData canvas 0 0 width height))

(def initial-scene
  {:camera  {:point         {:x 0
                             :y 1.8
                             :z 10}
             :field-of-view 45
             :vector        {:x 0
                             :y 3
                             :z 0}}
   :lights  {:x -30
             :y -10
             :z 20}
   :objects [{:type     :sphere
              :point    {:x 0
                         :y 3.5
                         :z -3}
              :color    {:x 155
                         :y 200
                         :z 155}
              :specular 0.2
              :lambert  0.7
              :ambient  0.1
              :radius   3}
             {:type     :sphere
              :point    {:x -4
                         :y 2
                         :z -1}
              :color    {:x 155
                         :y 155
                         :z 155}
              :specular 0.1
              :lambert  0.9
              :ambient  0.0
              :radius   0.2}
             {:type     :sphere
              :point    {:x -4
                         :y 3
                         :z -1}
              :color    {:x 255
                         :y 255
                         :z 255}
              :specular 0.2
              :lambert  0.7
              :ambient  0.1
              :radius   0.1}]})
(defonce *scene (atom initial-scene))

(defn render
  [{:keys [camera objects lights]}]
  (let [eye-vector (-> (v-subtract (:vector camera)
                                   (:point camera))
                       (v-unit-vector))
        vp-right (-> (v-cross-product eye-vector v-up)
                     (v-unit-vector))
        vp-up (-> (v-cross-product vp-right eye-vector)
                  (v-unit-vector))
        fov-radians (-> (:field-of-view camera)
                        (/ 2)
                        (* Math/PI)
                        (/ 180))
        height-width-ratio (/ height width)
        half-width (Math/tan height-width-ratio)]))


(defn start
  [])


(defn after-load
  [])

