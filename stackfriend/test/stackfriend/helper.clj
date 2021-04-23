(ns stackfriend.helper)


(defn ^Throwable create-exception
  []
  (ex-info "" {}))

(defn simple-exception
  []
  (let [p (promise)]
    (.start (Thread. (fn []
                       (let [x (create-exception)]
                         (deliver p x)))))
    @p))
