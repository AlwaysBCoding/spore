(ns spore.protocol.instance)

(defprotocol SporeInstanceProtocol
  ""
  (ident [self])
  
  (id [self] [self options])
  (display [self] [self options])
  
  (attr [self attribute] [self attribute options])

  (serialize [self serializer] [self serializer options])
  (data [self data-fn] [self data-fn options])

  (destroy [self] [self options] [self options db-uri])
  (revise [self params] [self params options] [self params options db-uri])
  (retract-components [self attribute] [self attribute options] [self attribute options db-uri])

)
