# Istio Enricher

A simple enricher which adds a dummy Istio Gateway resource to generated Kubernetes resources.

## How to Build?
```sh
$ gradle clean build
```

## Publishing to local Maven repository
In this quickstart we'll be publishing this enricher to local maven repository. You can run this gradle task to achieve that:

```sh
$ gradle publishToMavenLocal
```
