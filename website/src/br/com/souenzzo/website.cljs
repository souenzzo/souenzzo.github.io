(ns br.com.souenzzo.website)

(defn ^:export main
  []
  (.log js/console "hello!"))

(defn after-load
  [])