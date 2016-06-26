(ns full.aws.core
  (:require [full.core.config :refer [opt]])
  (:import (com.amazonaws ClientConfiguration Protocol)
           (com.amazonaws.regions Region Regions)
           (com.amazonaws.services.s3 AmazonS3Client S3ClientOptions)
           (com.amazonaws.auth AWSCredentialsProvider
                               BasicAWSCredentials
                               DefaultAWSCredentialsProviderChain)))

(def region-name   (opt [:aws :region]))
(def client-id     (opt [:aws :client-id] :default nil))
(def client-secret (opt [:aws :client-secret] :default nil))
(def s3-endpoint   (opt [:aws :s3-endpoint] :default nil))

(def region (delay (Region/getRegion (Regions/fromName @region-name))))

(defn- config-credentials-provider []
  (reify AWSCredentialsProvider
    (getCredentials [_]
      (BasicAWSCredentials. @client-id @client-secret))))

(def credentials-provider (delay (if (and @client-id @client-secret)
                                   (config-credentials-provider)
                                   (DefaultAWSCredentialsProviderChain.))))

(def http-protocol-client-config
  (delay (-> (ClientConfiguration.) (.withProtocol (Protocol/HTTP)))))

(def default-https-client
  (delay (let [c (AmazonS3Client. @credentials-provider)]
           (.setRegion c @region)
           (when @s3-endpoint
             (.setEndpoint c @s3-endpoint)
             (.setS3ClientOptions c (-> (S3ClientOptions.) (.withPathStyleAccess true))))
           c)))

(def default-http-client
  (delay (let [c (AmazonS3Client. @credentials-provider @http-protocol-client-config)]
           (.setRegion c @region)
           (when @s3-endpoint
             (.setEndpoint c @s3-endpoint)
             (.setS3ClientOptions c (-> (S3ClientOptions.) (.withPathStyleAccess true))))
           c)))
