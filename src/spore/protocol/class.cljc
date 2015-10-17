(ns spore.protocol.class)

(defprotocol SporeClassProtocol
  ""
  (manifest [self])
  (ident [self])
  (schema [self])

  (data [self data-fn] [self data-fn options])
  (query [self query-fn] [self query-fn options])

  (build [self params] [self params options])

  (all [self] [self options])
  ; (one [self] [self options])
  ; (where [self params] [self params options])
  ; (detect [self params] [self params options])
  ; (lookup [self id] [self id options])
  ; (destroy-all [self] [self options])
  ; (destroy-where [self params] [self params options])
  ; (create [self params] [self params options])
  ; (detect-or-create [self params] [self params options])

  )
