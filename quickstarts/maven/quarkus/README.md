# Eclipse JKube Quarkus Quickstart

A simple REST application demonstrating usage of Eclipse JKube with Quarkus.

## Requirements:

- JDK 11+
- Kubernetes Cluster (Minikube, OpenShift, CRC, etc.)

## Regular mode

## Build Docker Image

Make sure your Minikube instance is running and docker daemon is exposed. If not please run these commands:
```
# Start minikube cluster
minikube start 
# Expose minikube's docker deamon for building/pushing docker image
eval $(minikube docker-env)
```

Build the application and the docker image:
```
$ mvn package k8s:build
[INFO] Scanning for projects...
[INFO] 
[INFO] -------------------< org.eclipse.jkube:quarkus >------------------------
[INFO] Building Eclipse JKube :: Quarkus :: Rest :: Quickstart 0.1.1-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- kubernetes-maven-plugin:0.1.1-SNAPSHOT:build (default-cli) @ quarkus ---
[INFO] k8s: Running in Kubernetes mode
[INFO] k8s: Building Docker image in Kubernetes mode
[INFO] k8s: Running generator quarkus
[INFO] Copying files to /home/rohaan/work/repos/jkube/quickstarts/maven/quarkus/target/docker/jkube/quarkus/latest/build/maven
[INFO] Building tar: /home/rohaan/work/repos/jkube/quickstarts/maven/quarkus/target/docker/jkube/quarkus/latest/tmp/docker-build.tar
[INFO] k8s: [jkube/quarkus:latest] "quarkus": Created docker-build.tar in 231 milliseconds
[INFO] k8s: [jkube/quarkus:latest] "quarkus": Built image sha256:6ccff
[INFO] k8s: [jkube/quarkus:latest] "quarkus": Tag with latest
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  5.904 s
[INFO] Finished at: 2020-02-13T20:10:59+05:30
[INFO] ------------------------------------------------------------------------
$ docker images | grep quarkus
jkube/quarkus                          latest              6ccffc2be415        3 minutes ago       643MB
```

## Generate Kubernetes Manifests and apply then to Kubernetes Cluster
```
$ mvn k8s:resource k8s:apply
[INFO] Scanning for projects...
[INFO] 
[INFO] -------------------< org.eclipse.jkube:quarkus >------------------------
[INFO] Building Eclipse JKube :: Quarkus :: Rest :: Quickstart 0.1.1-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- kubernetes-maven-plugin:0.1.1-SNAPSHOT:resource (default-cli) @ quarkus ---
[INFO] k8s: Running generator quarkus
[INFO] k8s: jkube-controller: Adding a default Deployment
[INFO] k8s: jkube-service: Adding a default service 'quarkus' with ports [8080]
[INFO] k8s: jkube-healthcheck-vertx: HTTP health check disabled (path not set)
[INFO] k8s: jkube-healthcheck-vertx: HTTP health check disabled (path not set)
[INFO] k8s: jkube-revision-history: Adding revision history limit to 2
[INFO] k8s: validating /home/rohaan/work/repos/jkube/quickstarts/maven/quarkus/target/classes/META-INF/jkube/kubernetes/quarkus-deployment.yml resource
[INFO] k8s: validating /home/rohaan/work/repos/jkube/quickstarts/maven/quarkus/target/classes/META-INF/jkube/kubernetes/quarkus-service.yml resource
[INFO] 
[INFO] --- kubernetes-maven-plugin:0.1.1-SNAPSHOT:apply (default-cli) @ quarkus ---
[INFO] k8s: Using Kubernetes at https://192.168.39.191:8443/ in namespace default with manifest /home/rohaan/work/repos/jkube/quickstarts/maven/quarkus/target/classes/META-INF/jkube/kubernetes.yml 
[INFO] k8s: Using namespace: default
[INFO] k8s: Creating a Service from kubernetes.yml namespace default name quarkus
[INFO] k8s: Created Service: target/jkube/applyJson/default/service-quarkus.json
[INFO] k8s: Creating a Deployment from kubernetes.yml namespace default name quarkus
[INFO] k8s: Created Deployment: target/jkube/applyJson/default/deployment-quarkus.json
[INFO] k8s: HINT: Use the command `kubectl get pods -w` to watch your pods start up
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  8.943 s
[INFO] Finished at: 2020-02-13T20:11:18+05:30
[INFO] ------------------------------------------------------------------------
$ kubectl get pods
NAME                            READY   STATUS    RESTARTS   AGE
quarkus-5f8b6d7fdd-j2qww   1/1     Running   0          12s
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

> There is a [known issue](https://github.com/quarkusio/quarkus/issues/1610)
> when building a quarkus native image with a remote docker daemon.
> 
> If running with Minikube, first build the application (`mvn package -Pnative`) with 
> your host machine Docker daemon.
> 
> Once the application is built invoke `eval $(minikube docker-env)` before running `mvn k8s:build`.

To build the application, just reproduce the steps for the regular mode appending `-Pnative ` to
all your maven commands:
```
$ mvn clean package -Pnative-docker
$ eval $(minikube docker-env)
$ mvn k8s:build k8s:resource k8s:apply -Pnative-docker
$ minikube service quarkus
```

### JIB

```
$ mvn clean package k8s:build -Pnative-jib
$ eval $(minikube docker-env)
$ docker load -i target/docker/maven/quarkus/latest/tmp/docker-build.tar
$ mvn k8s:resource k8s:apply -Pnative-jib
$ minikube service quarkus
```