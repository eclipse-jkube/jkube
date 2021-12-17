# Eclipse JKube Micronaut quickstart

Micronaut application featuring REST endpoints (micronaut-http) with validation (micronaut-validation).

## Requirements

- JDK 11+
- Kubernetes Cluster (Minikube, OpenShift, CRC, etc.)

## Building and deploying the application

```shell script
# Kubernetes
$ gradle clean build k8sBuild k8sResource k8sApply
# OpenShift
$ gradle clean build ocBuild ocResource ocApply
```
## Expected output

Once you've deployed the application, you should be able to perform requests:

### Minikube

```shell script
$ curl $(minikube ip):$(kubectl get svc micronaut-zero-config -n default -o jsonpath='{.spec.ports[].nodePort}')
Hello from Micronaut deployed with JKube!
$ curl $(minikube ip):$(kubectl get svc micronaut-zero-config -n default -o jsonpath='{.spec.ports[].nodePort}')/hello/world
Hello word!
$ curl $(minikube ip):$(kubectl get svc micronaut-zero-config -n default -o jsonpath='{.spec.ports[].nodePort}')/hello/1
{"message":"name: size must be between 3 and 2147483647","_links":{"self":{"href":"/hello/1","templated":false}}}
```

### OpenShift

```shell script
$ curl $(oc get routes micronaut-zero-config -o jsonpath='{.spec.host}')
Hello from Micronaut deployed with JKube!
$ curl $(oc get routes micronaut-zero-config -o jsonpath='{.spec.host}')/hello/world
Hello world!%
$ curl $(oc get routes micronaut-zero-config -o jsonpath='{.spec.host}')/hello/1
{"message":"name: size must be between 3 and 2147483647","_links":{"self":{"href":"/hello/1","templated":false}}}
```
