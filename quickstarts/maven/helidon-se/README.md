---
name: "Maven :: Helidon SE"
description: |
Helidon application with a simple greeting endpoint.
Demonstrates how to package the project using JVM mode or Native image mode.
Demonstrates how to build a Helidon project container with Eclipse JKube's S2I and Docker build strategies.
---
# Eclipse JKube Helidon SE Quickstart

A simple REST application demonstrating usage of Eclipse JKube with Helidon SE.

## Requirements:

- JDK 11+
- Kubernetes Cluster (Minikube, OpenShift, CRC, etc.)

## Building and deploying the application in normal mode

```shell script
# Kubernetes
$ mvn clean package k8s:build k8s:resource k8s:apply
# OpenShift
$ mvn clean package oc:build oc:resource oc:apply
```
## Expected output

Once you've deployed the application, you should be able to perform requests:

### Minikube

```shell script
$ curl $(minikube ip):$(kubectl get svc helidon-se -n default -o jsonpath='{.spec.ports[].nodePort}')/greet
{"message":"Hello World!"}
```

### OpenShift

```shell script
$ curl $(oc get routes.route.openshift.io helidon-se -o jsonpath='{.spec.host}')/greet
{"message":"Hello World!"}
```

## Building and deploying the application in native mode

In order to generate native artifact, you need to use `native-image` profile:
```shell
$ mvn clean package -Pnative-image
```

When `-Pnative-image` is provided, JKube would automatically create image including this native artifact. However, 
you might need to change the base image of application using `jkube.generator.from` property:

```shell
$ mvn clean package k8s:build k8s:resource k8s:apply \
-Pnative-image \
-Djkube.generator.from=fedora:latest
```
