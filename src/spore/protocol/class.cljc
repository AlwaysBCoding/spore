(ns spore.protocol.class)

(defprotocol SporeClassProtocol
  ""
  (construct-instance [self entity]))
