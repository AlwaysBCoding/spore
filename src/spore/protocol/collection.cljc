(ns spore.protocol.collection)

(defprotocol SporeCollectionProtocol
  ""
  (ident [self])
  (total [self])
  (scope [self scope-name] [self scope-name options])
  (sorter [self sorter-name] [self sorter-name options])
  (serialize [self serializer] [self serializer options])
  (top [self] [self n]))
