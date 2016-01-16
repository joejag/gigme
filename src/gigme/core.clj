(ns gigme.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [clj-fuzzy.metrics :as metrics]
            [json-path :as json-path]))

;; api searching

(def api "http://api.setlist.fm/rest")

(defn setlist-fm-search [context]
  (-> 
   (str api context)
   client/get
   :body
   (json/read-str :key-fn keyword)))

(defn find-artist [artist]
  (-> 
   (setlist-fm-search (str "/0.1/search/artists.json?artistName=" artist))
   :artists
   :artist))

(defn find-setlists [artist]
  (->
   (setlist-fm-search (str "/0.1/artist/" (get artist (keyword "@mbid")) "/setlists.json"))
   :setlists))
 
;; parsing

(defn closest-artist [artists artist]
  (if (map? artists) artists  
      (last
       (sort-by 
        (fn [cand] (metrics/jaro artist (get cand (keyword "@name")))) 
        artists))))

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

(defn print-artists-probable-gig [artist]
  (println "=================================")
  (clojure.pprint/pprint (str api "/0.1/search/artists.json?artistName=" artist))
  (let [artist-response (find-artist artist)
        probable-artist (closest-artist artist-response artist)
        gigs-response (find-setlists probable-artist)]
    (clojure.pprint/pprint probable-artist)
    (clojure.pprint/pprint (probable-gig gigs-response))))


(defn -main [artist]
  (print-artists-probable-gig artist))
