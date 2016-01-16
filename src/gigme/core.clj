(ns gigme.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [clj-fuzzy.metrics :as metrics]
            [json-path :as json-path]))

;; api searching

(def api "http://api.setlist.fm/rest")

(defn closest-artist [artists artist]
  (if (map? artists) artists  
      (last
       (sort-by 
        (fn [cand] (metrics/jaro artist (get cand (keyword "@name")))) 
        artists))))

(defn find-artist [artist]
  (-> 
   (str api "/0.1/search/artists.json?artistName=" artist)
   client/get
   :body
   (json/read-str :key-fn keyword)
   :artists
   :artist
   (closest-artist artist)))

(defn setlists [artist]
  (->
   (str api "/0.1/artist/" (get artist (keyword "@mbid")) "/setlists.json")
   client/get
   :body
   (json/read-str :key-fn keyword)
   :setlists))
 
;; parsing

(defn gigs [response]
  (json-path/at-path "$.setlist[*].sets[*]" response))

(defn songs [response]
 (json-path/at-path "$.setlist[*].sets.set[*].song" response))

(defn songs-per-gig [songs gigs]
  (int (/ (count songs) (count gigs))))

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

(def artist "weezer")

(def artist-response (find-artist artist))
(def gigs-response (setlists artist-response))

(println "=================================")
(clojure.pprint/pprint (str api "/0.1/search/artists.json?artistName=" artist))
(clojure.pprint/pprint artist-response)
(clojure.pprint/pprint (probable-gig gigs-response))
