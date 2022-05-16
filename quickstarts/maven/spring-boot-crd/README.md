---
name: "Maven :: Spring Boot - Custom Resources"
description: |
  Spring Boot application using K8s Custom Resources.
  Declares a "Framework" Custom Resource Definition.
  Initializes cluster with sample data.
  Declares an endpoint to consume Custom Resource data stored in the cluster.
---
# Spring Boot with Custom Resources

This example shows how to use Eclipse JKube to deploy a Spring Boot application that uses Custom Resource Definitions.

## Requirements

- JDK 8 or 11+
- Kubernetes Cluster (Minikube, OpenShift, CRC, etc.)

## Preparing the environment

The deployment requires access to the underlying cluster, in order to enable this we must 
provide access to the cluster to the default service account.

The example provides a resource fragment (`src/main/jkube/default-cluster-admin-crb.yml`)
to achieve this.

If you have problems with this fragment or need special customization for your K8s cluster,
remove the mentioned fragment and run a customized version of the following command
(e.g. namespace != `default`):

```shell script
# Standard Kubernetes
$ kubectl create clusterrolebinding default-cluster-admin --clusterrole cluster-admin --serviceaccount=default:default
# OpenShift in myproject
$ oc create clusterrolebinding default-cluster-admin --clusterrole cluster-admin --serviceaccount=myproject:default
```

## Building and deploying the application

```shell script
# Kubernetes
$ mvn -Pkubernetes clean package k8s:build k8s:resource k8s:apply
# OpenShift
$ mvn -Popenshift clean package oc:build oc:resource oc:apply
```
## Expected output

Once you've deployed the application, you should be able to perform requests:

### Minikube

```shell script
$ curl $(minikube ip):$(kubectl get svc spring-boot-crd -n default -o jsonpath='{.spec.ports[].nodePort}')
["Quarkus","Spring Boot","Vert.x"]
```

### OpenShift

```shell script
$ curl $(oc get routes.route.openshift.io spring-boot-crd -o jsonpath='{.spec.host}')
["Quarkus","Spring Boot","Vert.x"]
```
