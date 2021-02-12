(ns br.com.souenzzo.dvm
  (:import (java.io StringWriter Writer)))

(defn render
  [^Writer w env element]
  (cond
    (and (coll? element)
         (keyword? (first element)))
    (let [[k v & vs] element
          opts? (map? v)]
      (.write w "<")
      (.write w (name k))
      (when opts?
        (doseq [[k v] v]
          (.write w " ")
          (.write w (name k))
          (.write w "=")
          (.write w (pr-str v))))
      (.write w ">")
      (doseq [el (if opts?
                   vs
                   (cons v vs))]
        (render w env el))
      (.write w "</")
      (.write w (name (first element)))
      (.write w ">"))
    (and (coll? element)
         (fn? (first element)))
    (let [[f & args] element]
      (render w env (apply f env args)))
    (coll? element)
    (doseq [el element]
      (render w env el))
    :else (.write w (str element)))
  w)

(defn render-to-string
  [env element]
  (with-open [w (StringWriter.)]
    (str (render w env element))))
