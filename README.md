# full.aws

[![Clojars Project](https://img.shields.io/clojars/v/fullcontact/full.aws.svg)](https://clojars.org/fullcontact/full.aws)

Async clients for Amazon Web Services. `full.aws` wraps Amazon's Java SDK and
provides asyns clients for Dynamo DB, SQS, and S3.


## Configuration

full.aws provides the following configuration options:

```
aws:
  region: "us-east-1"
  client-id: "xxx"
  client-secret: "xxx"
  sqs:
    message-count-fetch-interval: 10 # used for monitoring

kvstore:
  dynamo-table: "table name"
  dynamo-throughput:
    read: 1 # defaults to 1
    write: 1 # defaults to 1
```

If client id and secret are absent, default credentials will be loaded via the
[Java SDK directly](http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/credentials.html).

`kvstore` is used for Dynamo configuration, if needed.


## Services

### SQS

```clojure

(require '[full.aws.sqs :as sqs])


(defn handler> [{:keys [body]}]
  (go
    ; do async things with the message
    ))

(defn sqs-consumer>
  (sqs/subscribe "http://sqs.queue.url" handler> {:parallelism 5}))
```

### S3

```clojure

(require '[full.aws.s3 :as s3])

; asynchronously fetch a JSON file and parse it with full.json/read-json`
(->> (comp full.json/read-json s3/string-response-parser)
     (s3/get-object> "bucket-name" "key" :response-parser)
     (<!))

; Writing objects is done in the similar fashion:

(->> (full.json/write-json {:foo "bar"})
     (s3/put-object> "bucket-name" "key")
     (<!))
```

`full.aws.s3` provdies `get-edn>` and `put-edn>` for interfacing with edn
files stored on S3.


### Dynamo DB

`full.aws.dynamo.kvstore` wraps [faraday](https://github.com/ptaoussanis/faraday)
and provides `get>`, `put>`, and `delete>` methods that return a go block.
