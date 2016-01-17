(ns gigme.routes
  (:require [gigme.core :as core]
            [bidi.ring :refer (make-handler)]
            [ring.adapter.jetty :refer (run-jetty)]
            [ring.util.response :as res]))

(defn index-handler
  [request]
  (res/response "Let's find an artist"))

(defn happy-results [artist songs]
  (let [songs-html (map #(str "<li>" %) songs)]
    (str
     "<h1>" (get artist (keyword "@name")) "</h1>"
     "<h3>" (get artist (keyword "@disambiguation")) "</h3>"
     "<ul>" (apply str (vec songs-html)) "</ul>")))

(defn no-artist-results []
  "<h1>Cannot find Artist!</h1>")

(defn other-artists [name]
  (let [artists-html (map 
                      #(str "<li><a href='/gigfor/" (get % (keyword "@name") ) "/" (get % (keyword "@mbid")) "'>" (get % (keyword "@name")) "</a>") 
                      (core/find-artist name)) ]
    (str
     "<h1>This artist has no setlists, please choose another</h1>"
     "<ul>"
     (apply str artists-html)
     "</ul>")))

(defn result [response]
  (-> (res/response response)
      (res/content-type "text/html")))

(defn gigfor-artist [artist name]
 (let [songs (core/artists-probable-gig artist)]
   (if (zero? (count songs)) 
     (result (other-artists name)) 
     (result (happy-results artist songs)))) )

(defn gigfor-handler [{:keys [route-params]}]
  (let [artist (core/find-closest-artist (:name route-params))]
    (if (nil? artist) 
      (result (no-artist-results))
      (gigfor-artist artist (:name route-params)))))

(defn artistid-handler [{:keys [route-params]}]
  (gigfor-artist {(keyword "@mbid") (:id route-params)} (:name route-params)))

(def app
  (make-handler ["/" {"" index-handler
                      ["gigfor/" [#".*" :name]] gigfor-handler
                      ["gigfor/" [#".*" :name] "/" [#".*" :id]] artistid-handler
}]))

(defn -main []
  (run-jetty app {:port 5000}))
