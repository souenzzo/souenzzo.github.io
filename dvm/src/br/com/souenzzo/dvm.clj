(ns br.com.souenzzo.dvm
  (:import (java.io StringWriter Writer)))

(set! *warn-on-reflection* true)

;; https://html.spec.whatwg.org/#elements-2
(def void-elements
  #{:area :base :br :col :command :embed :hr :img :input
    :keygen :link :meta :param :source :track :wbr})

(def template-elements
  #{:template})

(def raw-text-elements
  #{:style :script})

(def rcdata-elements
  #{:textarea :title})

(defn render
  [^Writer w env element]
  (cond
    (and (coll? element)
         (keyword? (first element)))
    (let [[k v & vs] element
          opts? (map? v)
          kn (name k)]
      (.write w "<")
      (.write w kn)
      (when opts?
        (doseq [[k v] v]
          (.write w " ")
          (.write w (name k))
          (.write w "=")
          (.write w (pr-str v))))
      (.write w ">")
      (when-not (void-elements k)
        (doseq [el (if opts?
                     vs
                     (cons v vs))]
          (render w env el))
        (.write w "</")
        (.write w kn)
        (.write w ">")))
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
