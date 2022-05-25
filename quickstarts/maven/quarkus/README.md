# Eclipse JKube Quarkus Quickstart

A simple REST application demonstrating usage of Eclipse JKube with Quarkus.

## Requirements:

- JDK 11+
- Kubernetes Cluster (Minikube, OpenShift, CRC, etc.)

## Regular mode

## Build Container Image

Make sure your Minikube instance is running and docker daemon is exposed. If not please run these commands:
```shell
# Start minikube cluster
minikube start 
# Expose minikube's docker deamon for building/pushing docker image
eval $(minikube docker-env)
```

Build the application, and the container image:
```shell
$ mvn package k8s:build
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------< org.eclipse.jkube.quickstarts.maven:quarkus >-------------
[INFO] Building Eclipse JKube :: Quickstarts :: Maven :: Quarkus 1.3.0
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.3.0:build (default-cli) @ quarkus ---
[INFO] k8s: Building Docker image in Kubernetes mode
[INFO] k8s: Running generator quarkus
[INFO] k8s: quarkus: Using Docker image quay.io/jkube/jkube-java:0.0.13 as base / builder
[INFO] k8s: Pulling from jkube/jkube-java-binary-s2i
[INFO] k8s: Digest: sha256:dd5c9f44a86e19438662d293e180acc8d864887cf19c165c1b24ae703b16c2d4
[INFO] k8s: Status: Downloaded newer image for quay.io/jkube/jkube-java:0.0.13
[INFO] k8s: Pulled quay.io/jkube/jkube-java:0.0.13 in 21 seconds 
[INFO] k8s: [maven/quarkus:1.3.0] "quarkus": Created docker-build.tar in 241 milliseconds
[INFO] k8s: [maven/quarkus:1.3.0] "quarkus": Built image sha256:c4e4b
[INFO] k8s: [maven/quarkus:1.3.0] "quarkus": Removed old image sha256:3c4d4
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  27.258 s
[INFO] Finished at: 2021-05-19T09:32:50+02:00
[INFO] ------------------------------------------------------------------------


$ docker images | grep quarkus
maven/quarkus                                           1.3.0      c4e4be500963   About a minute ago   522MB
```

## Generate Kubernetes Manifests and apply then to Kubernetes Cluster
```shell
$ mvn k8s:resource k8s:apply
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------< org.eclipse.jkube.quickstarts.maven:quarkus >-------------
[INFO] Building Eclipse JKube :: Quickstarts :: Maven :: Quarkus 1.3.0
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.3.0:resource (default-cli) @ quarkus ---
[INFO] k8s: Running generator quarkus
[INFO] k8s: quarkus: Using Docker image quay.io/jkube/jkube-java:0.0.13 as base / builder
[INFO] k8s: Using resource templates from /home/user/00-MN/projects/forks/jkube/quickstarts/maven/quarkus/src/main/jkube
[INFO] k8s: jkube-controller: Adding a default Deployment
[INFO] k8s: jkube-service: Adding a default service 'quarkus' with ports [8080]
[INFO] k8s: jkube-service-discovery: Using first mentioned service port '8080' 
[INFO] k8s: jkube-revision-history: Adding revision history limit to 2
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.3.0:apply (default-cli) @ quarkus ---
[INFO] k8s: Using Kubernetes at https://192.168.49.2:8443/ in namespace default with manifest /home/user/00-MN/projects/forks/jkube/quickstarts/maven/quarkus/target/classes/META-INF/jkube/kubernetes.yml 
[INFO] k8s: Creating a Service from kubernetes.yml namespace default name quarkus
[INFO] k8s: Created Service: target/jkube/applyJson/default/service-quarkus.json
[INFO] k8s: Creating a Deployment from kubernetes.yml namespace default name quarkus
[INFO] k8s: Created Deployment: target/jkube/applyJson/default/deployment-quarkus.json
[INFO] k8s: HINT: Use the command `kubectl get pods -w` to watch your pods start up
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  7.690 s
[INFO] Finished at: 2021-05-19T09:34:40+02:00
[INFO] ------------------------------------------------------------------------

$ kubectl get pods
NAME                                            READY   STATUS    RESTARTS   AGE
quarkus-766f6c5b84-ng4s4                        1/1     Running   0          44s

$ minikube service quarkus
|-----------|--------------|-------------|-----------------------------|
| NAMESPACE |     NAME     | TARGET PORT |             URL             |
|-----------|--------------|-------------|-----------------------------|
| default   | quarkus      | http        | http://192.168.39.191:30890 |
|-----------|--------------|-------------|-----------------------------|
🎉  Opening service default/quarkus in default browser...
```
On invoking this, you can see it in browser:
<img src="https://i.imgur.com/vKCoaix.png" />


## Native mode

### Warning

### Docker

To build the application, just reproduce the steps for the regular mode appending `-Pnative ` to
all your maven commands:
```shell
$ eval $(minikube docker-env)
$ mvn clean package -Pnative-docker
$ mvn k8s:build k8s:resource k8s:apply -Pnative-docker
$ minikube service quarkus
```

### JIB

```shell
$ eval $(minikube docker-env)
$ mvn clean package k8s:build -Pnative-jib
$ docker load -i target/docker/maven/quarkus/${project.version}/tmp/docker-build.tar
$ mvn k8s:resource k8s:apply -Pnative-jib
$ minikube service quarkus
```
