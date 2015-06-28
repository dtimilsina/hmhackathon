(ns web-app.utils
  (:require [cljs.core.async :refer [chan put!]]))

(extend-type js/HTMLCollection
  ISeqable
  (-seq [hl] (array-seq hl 0)))

(defn make-form-data [form]
  (reduce (fn [m elt]
            (assoc m
              (keyword (.-name elt))
              (.-value elt)))
          {}
          (remove #(#{"button" "submit"} (.-type %))
                  (.-elements form))))

(defn cancel [e]
  (doto e (.preventDefault) (.stopPropagation)))

(defn id [id]
  (.getElementById js/document id))

(defn load-data-url [file]
  (let [fr (js/FileReader.)
        c (chan)]
    (set! (.-onload fr) (fn [e]
                          (put! c (.. e -target -result))))
    (.readAsDataURL fr file)
    c))
