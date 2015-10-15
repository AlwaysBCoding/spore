(ns spore.protocol.instance)

(defprotocol SporeInstanceProtocol
  ""
  (id [self])
  (attr [self attribute] [self attribute options])
  (display [self] [self options])

  (serialize [self serializer] [self serializer options])
  (data [self data-fn] [self data-fn options])

  (destroy [self] [self options])
  (revise [self params] [self params options])
  (retract-components [self attribute] [self attribute options]))
