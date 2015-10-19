# Spore

A Clojure framework for building web applications

### Version Information
[![Clojars Project](http://clojars.org/alwaysbcoding/spore/latest-version.svg)](http://clojars.org/alwaysbcoding/spore)

# Docs

### Artifacts
Config

Model
Query
Serializer
Scope
Data

### Config Spec
(def default-db-uri "...")
A string that represents the Datomic URI of the default database... (i.e. (.all Team)) would reach to this database. A model can be configured with a db-uri, but if not this is the fallback

### Manifest Spec
Valid keypairs...
{:type :string} <- defines the type of the attribute
{:ref-type :game} <- only to be used for ref-types, specifies the ident of the ref that it should create an instance of when called with (.attr ) ...
{:required true} <- the attribute is required and a record cannot be created without it

The ident is the top-level key in the manifest. Idents should be camelCase. A dot is a valid ident character to use as name-spacing. The next character after the dot starts a new camelCase string.
It's actually important that these names follow a specific pattern.

{:player {...}}
{:playerGame {...}}
{:basketball.gameEvent {...}}

### Model Spec
The model namespace/file needs to define an exports map
(def exports
  {:class (->Player)}) etc...
The exports map is what's picked up by the Spore parser.

### Query Spec

### License

Copyright © 2015 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
