(ns gigme.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [clj-fuzzy.metrics :as metrics]
            [json-path :as json-path]))

;; api searching

(def api "http://api.setlist.fm/rest")

(defn setlist-fm-search [context]
  (try (-> 
        (str api context)
        (client/get)
        :body
        (json/read-str :key-fn keyword))
       (catch Exception e 
         [])))

(defn find-artist [artist]
  (-> 
   (setlist-fm-search (str "/0.1/search/artists.json?artistName=" artist))
   (get-in [:artists :artist])))

(defn find-setlists [artist]
  (->
   (setlist-fm-search (str "/0.1/artist/" (get artist (keyword "@mbid")) "/setlists.json"))))
 
;; parsing

(defn closest-artist [artists artist]
  (if (map? artists) artists  
      (last
       (sort-by 
        (fn [cand] (metrics/jaro artist (get cand (keyword "@name")))) 
        artists))))

(defn gigs [response]
  (json-path/at-path "$.setlists.setlist[*].sets[*]" response))

(defn songs [response]
  (json-path/at-path "$.setlists.setlist[*].sets.set[*].song" response))

(defn songs-per-gig [songs gigs]
  (if (zero? (count songs)) 0 
      (int (/ (count songs) (count gigs)))))

(defn popular-songs-stats [songs]
  (reverse 
   (sort-by second 
            (frequencies (map (keyword "@name") songs)))))

(defn probable-gig [response]
  (let [songs (songs response)
        gigs (gigs response)
        popular-songs (map first (popular-songs-stats songs))]
    (take (songs-per-gig songs gigs) popular-songs)))

;; go

(defn find-closest-artist [artist]
  (-> artist
      (find-artist)
      (closest-artist artist)))

(defn artists-probable-gig [artist]
  (let [gigs-response (find-setlists artist)]
    (probable-gig gigs-response)))


