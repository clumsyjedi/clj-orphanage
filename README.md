# clj-orphanage

A Clojure library to find orphaned clojure files.

## Obtention

```
[clj-orphanage "0.2.1"]
```

## Usage

    (require 'orphanage.core)
    (orphanage.core/find-orphans "/path/to/proj/root")
    ;; returns a list of namespaces that are not referenced by any other namespace
    
    (orphanage.core/find-refs "/path/to/proj/root" 'some.ns)
    ;; returns a list of namespaces that reference (require) some.ns

## License

Copyright © 2013 Frazer Irving

Distributed under the Eclipse Public License, the same as Clojure.
