(ns br.com.souenzzo.dvm
  (:import (java.io StringWriter Writer)))

(set! *warn-on-reflection* true)

;; TODO: These definitions are
;; in https://github.com/weavejester/hiccup/blob/master/src/hiccup/compiler.clj#L65
;; many https://github.com/fulcrologic/fulcro/blob/develop/src/main/com/fulcrologic/fulcro/dom_server.clj#L347
;; repos. Could it be done in a simple "common html" package?
;; https://html.spec.whatwg.org/#elements-2
(def void-elements
  #{:area :base :br :col :command :embed :hr :img :input
    :keygen :link :meta :param :source :track :wbr})

(def template-elements
  #{:template})

(def raw-text-elements
  #{:style :script})

(def escapable-raw-text-elements
  #{:textarea :title})

(defn render
  [^Writer w env element]
  (if (coll? element)
    (let [el-head (first element)]
      (cond
        (keyword? el-head)
        (let [[tag-ident attributes & children] element
              opts? (map? attributes)
              ;; TODO: we should transform `:A` into `a`
              ;; TODO: we should throw DOMException cases like `:>`
              tag-name (name tag-ident)]
          (.write w "<")
          (.write w tag-name)
          (when opts?
            (doseq [[attribute-ident attribute-value] attributes]
              (.write w " ")
              ;; TODO: handle cases like `:>`
              (.write w (name attribute-ident))
              (.write w "=")
              ;; TODO: handle true, false, "><>"...
              (.write w (pr-str attribute-value))))
          ;; TODO: For void tags, should we `/>` ?! It's HTML? XML?
          (.write w ">")
          (when-not (void-elements tag-ident)
            (doseq [el (if opts?
                         children
                         (cons attributes children))]
              (render w env el))
            (.write w "</")
            (.write w tag-name)
            (.write w ">")))
        (fn? el-head) (render w env (apply el-head env (rest element)))
        :else (doseq [el element]
                (render w env el))))
    (.write w (str element))))

(defn render-to-string
  [env element]
  (with-open [w (StringWriter.)]
    (render w env element)
    (str w)))
