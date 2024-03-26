---
name: "Gradle :: Micronaut 4"
description: Micronaut 4 application featuring REST endpoints (micronaut-http) with validation (micronaut-validation).
---
# Eclipse JKube Micronaut 4 quickstart

Micronaut 4 application featuring REST endpoints (micronaut-http) with validation (micronaut-validation).

## Requirements

- JDK 17+
- Kubernetes Cluster (Minikube, OpenShift, CRC, etc.)

## Building and deploying the application

```shell script
# Kubernetes
$ ./gradlew clean build k8sBuild k8sResource k8sApply
# OpenShift
$ ./gradlew clean build ocBuild ocResource ocApply
```
## Expected output

Once you've deployed the application, you should be able to perform requests:

### Minikube

```shell script
$ curl $(minikube ip):$(kubectl get svc micronaut-zero-config -n default -o jsonpath='{.spec.ports[].nodePort}')
Hello from Micronaut deployed with JKube!
$ curl $(minikube ip):$(kubectl get svc micronaut-zero-config -n default -o jsonpath='{.spec.ports[].nodePort}')/hello/world
Hello word!
```

### OpenShift

```shell script
$ curl $(oc get routes micronaut-zero-config -o jsonpath='{.spec.host}')
Hello from Micronaut deployed with JKube!
$ curl $(oc get routes micronaut-zero-config -o jsonpath='{.spec.host}')/hello/world
Hello world!
```
