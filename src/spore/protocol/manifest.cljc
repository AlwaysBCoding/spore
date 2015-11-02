(ns spore.protocol.manifest)

(defprotocol SporeManifest
  ""
  (inflections [self]
    #_{:ident :...
       :namespace :...
       :plural :...
       :datomic-prefix :...}
    )

  (schema [self]
    #_{}
  )

  (relations [self]
    #_{:has-many :...}
  )

  (lifecycle [self]
    #_{:before-save :...
       :after-save :...
       :before-create :...
       :after-create :...
       :before-destroy :...
       :after-destroy :...}
  ))
