(defproject fullcontact/full.aws "1.0.0-SNAPSHOT"
  :description "Async Amazon Webservices client."
  :url "https://github.com/fullcontact/full.aws"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  :deploy-repositories [["releases" {:url "https://clojars.org/repo/" :creds :gpg}]]
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [com.amazonaws/aws-java-sdk "1.12.270"]
                 [com.taoensso/faraday "1.11.4" ; DynamoDB sugar
                  :exclusions [com.amazonaws/aws-java-sdk-dynamodb joda-time]]
                 [fullcontact/full.http "1.1.0"]
                 [fullcontact/full.json "0.12.0"
                  :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [fullcontact/full.async "1.1.0"]
                 [fullcontact/full.core "1.1.1"
                  :exclusions [org.clojure/clojurescript]]
                 [javax.xml.bind/jaxb-api "2.4.0-b180830.0359"]]
  :aot :all
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]
  :plugins [[lein-midje "3.1.3"]]
  :profiles {:dev {:dependencies [[midje "1.7.0"]]}})
