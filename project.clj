(defproject gigme "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/data.json "0.2.6"]
                 [json-path "0.2.0"]
                 [clj-fuzzy "0.1.8"]
                 [ring "1.4.0"]
                 [bidi "1.25.0"]
                 [clj-http "2.0.0"]]

  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler gigme.routes/app :port 5000}
  :main gigme.routes)

