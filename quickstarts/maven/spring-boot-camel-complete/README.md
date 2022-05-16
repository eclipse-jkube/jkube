---
name: "Maven :: Spring Boot - Camel"
description: |
  Spring Boot application with Camel Spring Boot integration.
  Programmatically (RouteBuilder) declares 2 routes to log messages and process orders.
  Declares an Apache Camel servlet REST endpoint.
---
# Eclipse JKube Spring Boot with Camel integration Quick start

This is a quickstart project to use Eclipse JKube plugin with [Apache Camel](https://camel.apache.org/).

The example project includes several routes:
 - Route to generate log entries in a customized logger.
 - Route to generate fake order XML files in a directory and process them.
 - REST DSL route to demonstrate CamelHttpTransportServlet.

## Requirements:

- JDK 8 or 11+
- Kubernetes compatible Cluster (Minikube, OpenShift, CRC, etc.)

## How to test

### Docker build strategy (default)
With Minikube running, perform the following commands:
```shell script
$ eval $(minikube docker-env)
# Deploy application into Minikube
$ mvn -Pkubernetes clean package k8s:build k8s:resource k8s:apply
# Check logs
$ mvn -Pkubernetes k8s:log
# ...
[INFO] k8s: 2020-08-05 07:37:31.077  INFO 1 --- [- timer://order] route1                                   : Generating order order1.xml
[INFO] k8s: 2020-08-05 07:37:31.538  INFO 1 --- [rk/orders/input] route2                                   : Processing order order1.xml
[INFO] k8s: 2020-08-05 07:37:32.041  INFO 1 --- [3 - timer://foo] o.e.j.q.s.camel.log.CamelRouteBuilder    : Hello from Camel!
[INFO] k8s: 2020-08-05 07:37:32.042  INFO 1 --- [3 - timer://foo] o.e.j.q.s.camel.log.CamelRouteBuilder    : My id is ID-spring-boot-camel-complete-55b988884-9plb7-1596613048859-0-8
# ...
# Check exposed REST endpoint
$ curl $(minikube ip):$(kubectl get svc spring-boot-camel-complete -o jsonpath='{.spec.ports[].nodePort}')"/camel/hello-camel"
"Hello from Camel!"
```

### OpenShift (S2I build)
With a valid OpenShift cluster, perform the following commands:
```shell script
# Deploy application into OpenShift
$ mvn -Popenshift clean package oc:build oc:resource oc:apply
# Check logs
$ mvn -Popenshift oc:log
# ...
[INFO] oc: 2020-08-05 07:43:56.523  INFO 1 --- [- timer://order] route1                                   : Generating order order1.xml
[INFO] oc: 2020-08-05 07:43:57.012  INFO 1 --- [rk/orders/input] route2                                   : Processing order order1.xml
[INFO] oc: 2020-08-05 07:43:57.513  INFO 1 --- [3 - timer://foo] o.e.j.q.s.camel.log.CamelRouteBuilder    : Hello from Camel!
[INFO] oc: 2020-08-05 07:43:57.513  INFO 1 --- [3 - timer://foo] o.e.j.q.s.camel.log.CamelRouteBuilder    : My id is ID-spring-boot-camel-complete-1-rhdzd-1596613433416-0-8
# ...
# Check exposed REST endpoint
$ curl $(kubectl get routes.route.openshift.io spring-boot-camel-complete -o jsonpath='{.spec.host}')"/camel/hello-camel"
"Hello from Camel!"
```
