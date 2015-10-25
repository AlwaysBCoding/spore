(ns spore.protocol.lifecycle)

(defprotocol SporeInstanceLifecycleProtocol
  ""
  (before-save [self params])
  (after-save [self record tx-result]))

(defprotocol SporeClassLifecycleProtocol
  ""
  (before-create [self params])
  (after-create [self record tx-result]))
