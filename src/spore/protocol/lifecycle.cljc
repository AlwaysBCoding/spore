(ns spore.protocol.lifecycle)

(defprotocol SporeInstanceLifecycleProtocol
  ""
  (before-save [self params])
  (after-save [self tx-result]))

(defprotocol SporeClassLifecycleProtocol
  ""
  (before-create [self params])
  (after-create [self tx-result]))
