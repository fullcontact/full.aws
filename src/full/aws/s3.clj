(ns full.aws.s3
  "Super-simple async S3 client for storing and fetching string or EDN objects."
  (:require [clojure.edn :as edn]
            [full.aws.core :as aws]
            [full.async :refer :all]
            [full.http.client :as http]
            [full.core.time :refer :all]
            [full.core.edn :refer [read-edn]]
            [full.core.log :as log])
  (:import (com.amazonaws.services.s3.model GeneratePresignedUrlRequest)
           (com.amazonaws.services.s3.model ListObjectsRequest)
           (java.util Date)
           (com.amazonaws HttpMethod)
           (java.io InputStream)))

(def client aws/default-https-client)

(defn- expiration-date [ms-to-expire]
  (let [d (Date.)
        msec (.getTime d)
        msec-inc (or ms-to-expire
                     (* 24 60 60 1000))                     ;24hr
        time (+ msec msec-inc)]
    (.setTime d time)
    d))

(defn- unrwrap-etag
  "Etag header is wrapped in double quotes, simply substring it."
  [etag]
  (when (string? etag)
    (.substring etag 1 (dec (.length etag)))))

(defn- presign-request-add-params [req params]
  (doseq [[k v] params]
    (.addRequestParameter req k v))
  req)

(defn- presign-request
  [bucket-name key & {:keys [method content-type params expiration]}]
  (-> (GeneratePresignedUrlRequest. bucket-name key)
      (.withExpiration (expiration-date expiration))
      (cond-> method (.withMethod method)
              content-type (.withContentType content-type)
              params (presign-request-add-params params))))

(defn generate-presigned-url
  [bucket-name key & {:keys [client content-type params method expiration] :or {client @client}}]
  (let [method (condp = method
                 :get HttpMethod/GET
                 :post HttpMethod/POST
                 :head HttpMethod/HEAD
                 :patch HttpMethod/PATCH
                 :delete HttpMethod/DELETE
                 HttpMethod/PUT)
        req (presign-request bucket-name
                             key
                             :method method
                             :content-type content-type
                             :params params
                             :expiration expiration)
        url (.generatePresignedUrl client req)]
    (str url)))

(defn put-object>
  "Create or updates S3 object. Returns a channel that will yield single
  etag on success or exception on error.
  Optional parameters:
    * :headers - HTTP headers such as Content-Type
    * :timeout - request timeout in seconds"
  [^String bucket-name, ^String key, ^String body
   & {:keys [headers timeout client]
      :or {client @client}}]
  {:pre [bucket-name key body]}
  (go-try
    (let [content-type (or (get headers "Content-Type")
                           "text/plain")
          url (generate-presigned-url bucket-name key :content-type content-type)
          headers (-> (or headers {})
                      (assoc "Content-Length" (.length body))
                      (assoc "Content-Type" content-type))]
      (-> (http/req> {:url url
                      :method :put
                      :body body
                      :headers headers
                      :timeout timeout
                      ; we want raw response to access headers
                      :response-parser nil})
          (<?)
          (get-in [:headers :etag])
          (unrwrap-etag)))))

(defn put-edn>
  [^String bucket-name, ^String key, ^String object
   & {:keys [headers timeout client]}]
  (put-object> bucket-name key (pr-str object)
               :headers (-> (or headers {})
                            (assoc "Content-Type" "application/edn"))
               :timeout timeout
               :client client))

(defn- string-response-parser [res]
  (cond
    (string? res) res
    (instance? InputStream res) (slurp res)
    :else res))

(defn get-object>
  [^String bucket-name, ^String key
   & {:keys [headers timeout response-parser client]
      :or {response-parser string-response-parser client @client}}]
  (go-try
    (let [url (.generatePresignedUrl client (presign-request bucket-name key))]
      (-> (http/req> {:url (str url)
                      :method :get
                      :headers headers
                      :timeout timeout})
          (<?)
          (response-parser)))))

(defn get-object-metadata>
  [^String bucket-name, ^String key
   & {:keys [headers timeout response-parser client]
      :or {response-parser string-response-parser client @client}}]
  (go-try
    (let [url (.generatePresignedUrl client (presign-request bucket-name key :method (HttpMethod/HEAD)))]
      (-> (http/req> {:url (str url)
                      :method :head
                      :timeout timeout})
          (<?)))))

(defn get-edn>
  [^String bucket-name, ^String key
   & {:keys [headers timeout client]}]
  (get-object> bucket-name key
               :headers headers
               :timeout timeout
               :response-parser (comp read-edn string-response-parser)
               :client client))

(defn list-objects> [bucket-name prefix]
  (let [req (-> (ListObjectsRequest.)
                (.withBucketName bucket-name)
                (.withPrefix prefix))]
    (thread-try
      (.listObjects @client req))))

(defn delete-object>
  [^String bucket-name, ^String key
   & {:keys [headers timeout client] :or {client @client}}]
  (go-try
    (let [url (.generatePresignedUrl client (presign-request bucket-name key :method (HttpMethod/DELETE)))]
      (-> (http/req> {:url (str url)
                      :method :delete
                      :headers headers
                      :timeout timeout})
          (<?)))))