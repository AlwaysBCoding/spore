(ns spore.protocol.manifest)

(defprotocol SporeManifest
  ""
  (inflections [self]
    ;; ident
    ;; namespace
    ;; plural
    ;; datomic-prefix
    )

  (schema [self]
    ;; schema
  )

  (relations [self]
    ;; has-many
  )

  (lifecycle [self]
    ;; before-save
    ;; after-save
    ;; before-create
    ;; after-create
    ;; before-destroy
    ;; after-destroy
  ))
