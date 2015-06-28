(ns web-app.utils)

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
