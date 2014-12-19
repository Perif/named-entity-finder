(ns named-entity.core
 (:require [clojure.java.io :as io])
 (:import [opennlp.tools.namefind NameFinderME TokenNameFinderModel]))

;; Named Entity Extraction Tools
;; ****************************************************

(defn tokenize
  [input]
  (clojure.string/split input #"\s+"))

(defn extract-span
  "Extracts the information from a span"
  [s]
  (let [start (.getStart s)
        end (.getEnd s)
        token-type (.getType s)]
    { :token token-type :start start :end end }))

(defn make-name-finder
  "Builds a named entity classifier"
  [model]
  (with-open [model-input-stream (io/input-stream model)]
    (NameFinderME.
      (TokenNameFinderModel. model-input-stream))))

(defn finder [finder-type]
  (make-name-finder
    (str "models/namefind/en-ner-" (name finder-type) ".bin")))

;; ****************************************************

(def person-finder   (finder :person))
(def date-finder     (finder :date))
(def time-finder     (finder :time))
(def location-finder (finder :location))

;; ****************************************************

(defn find-entities
  "Finds named entities in a sequence of tokens"
  [finder tokens]
  (let [array-tokens (into-array tokens)
        entities (map extract-span
                   (.find finder array-tokens))]
    (mapv
      (fn [entity]
        (let [[start end] ((juxt :start :end :token-type) entity)
               e (subvec tokens start end)]
          {:token (:token entity) :value (clojure.string/join " " e)}))
         entities)))

(defn extract-entities
  "Extract named entities from a sentence
   i.e (extract-entities :person \"Mr David James is a person of great taste\")"
  ([sentence]
    ((comp flatten into) []
      (for [f [:person :date :time :location]]
        (extract-entities f sentence))))
  ([finder-type sentence]
   (->> sentence
        (tokenize)
        (find-entities (finder finder-type)))))
