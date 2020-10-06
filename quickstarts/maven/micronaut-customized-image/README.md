# Eclipse JKube Micronaut customized image quickstart

Micronaut application featuring REST endpoints (micronaut-http) with validation (micronaut-validation).

Includes configuration to enable health check probes (livenessProbe &amp; readinessProbe) using JKube's
powerful resource fragments and micronaut-management feature.

XML based image configuration compatible with OpenShift and Kubernetes.

## Requirements

- JDK 11+
- Kubernetes Cluster (Minikube, OpenShift, CRC, etc.)

## Building and deploying the application

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
$ curl $(minikube ip):$(kubectl get svc micronaut-customized-image -n default -o jsonpath='{.spec.ports[].nodePort}')
Hello from Micronaut deployed with JKube!
$ curl $(minikube ip):$(kubectl get svc micronaut-customized-image -n default -o jsonpath='{.spec.ports[].nodePort}')/hello/world
Hello word!
$ curl $(minikube ip):$(kubectl get svc micronaut-customized-image -n default -o jsonpath='{.spec.ports[].nodePort}')/hello/1
{"message":"name: size must be between 3 and 2147483647","_links":{"self":{"href":"/hello/1","templated":false}}}
```

### OpenShift

```shell script
$ curl $(oc get routes.route.openshift.io micronaut-customized-image -o jsonpath='{.spec.host}')
Hello from Micronaut deployed with JKube!
$ curl $(oc get routes.route.openshift.io micronaut-customized-image -o jsonpath='{.spec.host}')/hello/world
Hello world!%
$ curl $(oc get routes.route.openshift.io micronaut-customized-image -o jsonpath='{.spec.host}')/hello/1
{"message":"name: size must be between 3 and 2147483647","_links":{"self":{"href":"/hello/1","templated":false}}}
```
