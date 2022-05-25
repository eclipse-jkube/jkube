---
name: "Gradle :: Quarkus"
description: |
  Quarkus application with a single JAX-RS endpoint.
  Demonstrates how to package the project using JVM mode or Native image mode.
  Demonstrates how to build a Quarkus project container with Eclipse JKube's S2I, Docker and JIB build strategies.
---
# Eclipse JKube Quarkus Quickstart

A simple REST application demonstrating usage of Eclipse JKube with Quarkus.

## Requirements:

- JDK 11+
- Kubernetes Cluster (Minikube, OpenShift, CRC, etc.)

## Regular mode

### For Kubernetes
Make sure your Minikube instance is running and docker daemon is exposed. If not please run these commands:
```shell
$ minikube start
$ eval $(minikube -p minikube docker-env)
```

Generate the cluster manifests by executing the following command:
```shell
$ ./gradlew build k8sResource

> Task :k8sResource
k8s: Running generator quarkus
k8s: quarkus: Using Docker image quay.io/jkube/jkube-java:0.0.13 as base / builder
k8s: Using resource templates from /home/anurag/Work/jkube/quickstarts/gradle/quarkus/src/main/jkube
k8s: jkube-controller: Adding a default Deployment
k8s: jkube-service: Adding a default service 'quarkus' with ports [8080]
k8s: jkube-service-discovery: Using first mentioned service port '8080' 
k8s: jkube-revision-history: Adding revision history limit to 2
k8s: validating /home/anurag/Work/jkube/quickstarts/gradle/quarkus/build/classes/java/main/META-INF/jkube/kubernetes/quarkus-service.yml resource
k8s: validating /home/anurag/Work/jkube/quickstarts/gradle/quarkus/build/classes/java/main/META-INF/jkube/kubernetes/quarkus-deployment.yml resource

BUILD SUCCESSFUL in 14s
7 actionable tasks: 1 executed, 6 up-to-date
```

Start docker build  by hitting the build task.
```shell
$ ./gradlew k8sBuild

> Task :k8sBuild
k8s: Running generator quarkus
k8s: quarkus: Using Docker image quay.io/jkube/jkube-java:0.0.13 as base / builder
k8s: Building container image in Kubernetes mode
k8s: [gradle/quarkus:1.7.0] "quarkus": Created docker-build.tar in 378 milliseconds
k8s: [gradle/quarkus:1.7.0] "quarkus": Built image sha256:c1df2
k8s: [gradle/quarkus:1.7.0] "quarkus": Removed old image sha256:04421

BUILD SUCCESSFUL in 10s
1 actionable task: 1 executed

```

Deploy your application on Kubernetes cluster.
```shell
$ ./gradlew k8sApply

> Task :k8sApply
k8s: Running generator quarkus7s]
k8s: quarkus: Using Docker image quay.io/jkube/jkube-java:0.0.13 as base / builder
k8s: Using Kubernetes at https://192.168.49.2:8443/ in namespace null with manifest /home/anurag/Work/jkube/quickstarts/gradle/quarkus/build/classes/java/main/META-INF/jkube/kubernetes.yml k8sApply
k8s: Updating Service from kubernetes.yml
k8s: Updated Service: build/jkube/applyJson/default/service-quarkus-2.json
k8s: Creating a Deployment from kubernetes.yml namespace default name quarkus
k8s: Created Deployment: build/jkube/applyJson/default/deployment-quarkus-2.json
k8s: HINT: Use the command `kubectl get pods -w` to watch your pods start up

BUILD SUCCESSFUL in 15s
1 actionable task: 1 executed

$ kubectl get pods                 
NAME                       READY   STATUS    RESTARTS   AGE
quarkus-689c7d4bcc-t2jfw   1/1     Running   0          56s

$ minikube service quarkus --url
http://192.168.49.2:30185
```

On invoking this, you can see it in browser:
![Imgur](https://i.imgur.com/YNCvhuf.png)

## Native mode

### Docker

```shell
$ eval $(minikube -p minikube docker-env)
$ ./gradlew clean build -Pquarkus.package.type=native -Pquarkus.native.remote-container-build=true
$ ./gradlew k8sBuild k8sResource k8sApply
```

### JIB

```shell
$ eval $(minikube -p minikube docker-env)
$ ./gradlew clean build -Pquarkus.package.type=native -Pquarkus.native.remote-container-build=true
$ ./gradlew k8sBuild -Pjkube.build.strategy=jib
$ docker load -i ./build/docker/gradle/quarkus/1.7.0/tmp/docker-build.tar
$ ./gradlew k8sResource k8sApply
```
