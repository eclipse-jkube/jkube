# Eclipse Jkube Quarkus Quickstart

A simple rest application demonstrating usage of Eclipse Jkube with Quarkus.

## Prerequisites:

Make sure your minikube instance is running and docker deamon is exposed. If not please run these commands:
```
# Start minikube cluster
minikube start 
# Expose minikube's docker deamon for building/pushing docker image
eval $(minikube docker-env)
```

## Build Docker Image
```
~/work/repos/jkube/quickstarts/maven/quarkus : $ mvn package k8s:build
[INFO] Scanning for projects...
[INFO] 
[INFO] -------------------< org.eclipse.jkube:quarkus-rest >-------------------
[INFO] Building Eclipse Jkube :: Quarkus :: Rest :: Quickstart 0.1.1-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- k8s-maven-plugin:0.1.1-SNAPSHOT:build (default-cli) @ quarkus-rest ---
[INFO] k8s: Running in Kubernetes mode
[INFO] k8s: Building Docker image in Kubernetes mode
[INFO] k8s: Running generator quarkus
[INFO] Copying files to /home/rohaan/work/repos/jkube/quickstarts/maven/quarkus/target/docker/jkube/quarkus-rest/latest/build/maven
[INFO] Building tar: /home/rohaan/work/repos/jkube/quickstarts/maven/quarkus/target/docker/jkube/quarkus-rest/latest/tmp/docker-build.tar
[INFO] k8s: [jkube/quarkus-rest:latest] "quarkus": Created docker-build.tar in 231 milliseconds
[INFO] k8s: [jkube/quarkus-rest:latest] "quarkus": Built image sha256:6ccff
[INFO] k8s: [jkube/quarkus-rest:latest] "quarkus": Tag with latest
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  5.904 s
[INFO] Finished at: 2020-02-13T20:10:59+05:30
[INFO] ------------------------------------------------------------------------
~/work/repos/jkube/quickstarts/maven/quarkus : $ docker images | grep quarkus
jkube/quarkus-rest                          latest              6ccffc2be415        3 minutes ago       643MB
```

## Generate Kubernetes Manifests and apply then to Kubernetes Cluster
```
~/work/repos/jkube/quickstarts/maven/quarkus : $ mvn k8s:resource k8s:apply
[INFO] Scanning for projects...
[INFO] 
[INFO] -------------------< org.eclipse.jkube:quarkus-rest >-------------------
[INFO] Building Eclipse Jkube :: Quarkus :: Rest :: Quickstart 0.1.1-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- k8s-maven-plugin:0.1.1-SNAPSHOT:resource (default-cli) @ quarkus-rest ---
[INFO] k8s: Running generator quarkus
[INFO] k8s: jkube-controller: Adding a default Deployment
[INFO] k8s: jkube-service: Adding a default service 'quarkus-rest' with ports [8080]
[INFO] k8s: jkube-healthcheck-vertx: HTTP health check disabled (path not set)
[INFO] k8s: jkube-healthcheck-vertx: HTTP health check disabled (path not set)
[INFO] k8s: jkube-revision-history: Adding revision history limit to 2
[INFO] k8s: validating /home/rohaan/work/repos/jkube/quickstarts/maven/quarkus/target/classes/META-INF/jkube/kubernetes/quarkus-rest-deployment.yml resource
[INFO] k8s: validating /home/rohaan/work/repos/jkube/quickstarts/maven/quarkus/target/classes/META-INF/jkube/kubernetes/quarkus-rest-service.yml resource
[INFO] 
[INFO] --- k8s-maven-plugin:0.1.1-SNAPSHOT:apply (default-cli) @ quarkus-rest ---
[INFO] k8s: Using Kubernetes at https://192.168.39.191:8443/ in namespace default with manifest /home/rohaan/work/repos/jkube/quickstarts/maven/quarkus/target/classes/META-INF/jkube/kubernetes.yml 
[INFO] k8s: Using namespace: default
[INFO] k8s: Creating a Service from kubernetes.yml namespace default name quarkus-rest
[INFO] k8s: Created Service: target/jkube/applyJson/default/service-quarkus-rest.json
[INFO] k8s: Creating a Deployment from kubernetes.yml namespace default name quarkus-rest
[INFO] k8s: Created Deployment: target/jkube/applyJson/default/deployment-quarkus-rest.json
[INFO] k8s: HINT: Use the command `kubectl get pods -w` to watch your pods start up
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  8.943 s
[INFO] Finished at: 2020-02-13T20:11:18+05:30
[INFO] ------------------------------------------------------------------------
~/work/repos/jkube/quickstarts/maven/quarkus : $ ls
pom.xml  quarkus-rest.iml  src  target
~/work/repos/jkube/quickstarts/maven/quarkus : $ kubectl get pods
NAME                            READY   STATUS    RESTARTS   AGE
quarkus-rest-5f8b6d7fdd-j2qww   1/1     Running   0          12s
~/work/repos/jkube/quickstarts/maven/quarkus : $ minikube service quarkus-rest
|-----------|--------------|-------------|-----------------------------|
| NAMESPACE |     NAME     | TARGET PORT |             URL             |
|-----------|--------------|-------------|-----------------------------|
| default   | quarkus-rest | http        | http://192.168.39.191:30890 |
|-----------|--------------|-------------|-----------------------------|
🎉  Opening service default/quarkus-rest in default browser...
```
On invoking this, you can see it in browser:
<img src="https://i.imgur.com/vKCoaix.png" />
