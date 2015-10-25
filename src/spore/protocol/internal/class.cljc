(ns spore.protocol.internal.class)

(defprotocol SporeInternalClassProtocol
  ""
  (manifest [self])
  (dependencies [self])
  (ident [self])
  (schema [self])

  (data [self data-fn] [self data-fn options])
  (query [self query-fn] [self query-fn options])

  (build [self params] [self params options])
  (create [self params] [self params options] [self params options db-uri])

  (all [self] [self options] [self options db-uri])
  (where [self params] [self params options] [self params options db-uri])
  (detect [self params] [self params options] [self params options db-uri])
  (lookup [self id] [self id options] [self id options db-uri])
  (one [self] [self options] [self options db-uri])
  (detect-or-create [self params] [self params options] [self params options db-uri])

  (destroy-all [self] [self options] [self options db-uri])
  (destroy-where [self params] [self params options] [self params options db-uri]))
