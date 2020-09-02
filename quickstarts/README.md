# Eclipse JKube Quick start examples

This directory contains quick start sample projects:
 - [**kit**](kit): Example projects using Eclipse JKube standalone Kit
 - [**maven**](maven): Example projects using Eclipse JKube Maven plugins (Kubernetes & OpenShift)
 
## Versioning

Examples are bound to the latest Maven Central release
![latest-release](https://img.shields.io/maven-central/v/org.eclipse.jkube/jkube.svg)


To override the JKube release you can pass the `jkube.version` property when running the examples.

e.g.
```shell script
$ mvn -Pkubernetes clean install k8s:build -Djkube.version=1.33.7
```