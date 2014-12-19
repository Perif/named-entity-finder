(ns named-entity.core
 (:require [clojure.java.io :as io])
 (:import [opennlp.tools.namefind NameFinderME TokenNameFinderModel]))

;; Named Entity Extraction Tools
;; ****************************************************

(defn tokenize
  "Very basic tokenization based on whitespace"
  [input]
  (clojure.string/split input #"\s+"))

(defn fixture
  "Helper function that can be used to load training data
   to test the named entity extractor"
  [f]
  (slurp (str "training-data/" f ".txt")))

(defn extract-span
  "Extracts the information from a span"
  [s]
  (let [start (.getStart s)
        end (.getEnd s)
        token-type (.getType s)]
    { :token token-type :start start :end end }))

(defn- make-name-finder
  "Builds a named entity classifier"
  [model]
  (with-open [model-input-stream (io/input-stream model)]
    (NameFinderME.
      (TokenNameFinderModel. model-input-stream))))

(defn- in?
  "Determine whether a sequence xs contains x"
  [xs x]
  (if (empty? xs)
    false
    (reduce #(or %1 %2) (map #(= %1 x) xs))))

(defn- valid-finder? [f]
  (in? [:person :date :time :location] (keyword f)))

(defn- finder
  "Utility function to build finders from existing models"
  [finder-type]
  (when (valid-finder? finder-type)
    (make-name-finder
      (str "models/namefind/en-ner-" (name finder-type) ".bin"))))

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
      (map deref
        (doall (map
                 #(future (extract-entities % sentence))
                  [:person :date :time :location])))))
  ([finder-type sentence]
   (->> sentence
        (tokenize)
        (find-entities (finder finder-type)))))

;; An alias for a nicer DSL

(def ?e extract-entities)
