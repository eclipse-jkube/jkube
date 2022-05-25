---
name: "Gradle :: Custom Webapp"
description: |
  Java Web Application with a static index.html resource.
  Demonstrates how to use Eclipse JKube with a war project to build a container image based on a custom image instead of the defaults Tomcat/Jetty/Wildfly. 
---
# Eclipse JKube Webapp custom
This quick start showcases how to use Eclipse JKube with a war project to build a container image based on a custom image instead of the defaults Tomcat/Jetty/Wildfly.

Using a custom base image for a war project is just about configuring the `webapp` generator in `build.gradle` file.

This quickstart demonstrates how to create a container image based on a custom image: `tomcat:jdk11-openjdk-slim`. It also specify a `targetDir` where the war file will be copied in the container image.


## Prerequisites
You will need the following to run it with Minikube:
- minikube installed and running on your computer
- minikube ingress addon enabled

      $ minikube addons enable ingress

- Use the docker daemon installed in minikube

      $ eval $(minikube -p minikube docker-env)

## Build the application and docker image
```
$ ./gradlew build k8sBuild

> Task :k8sBuild
k8s: Running generator webapp
k8s: webapp: Using tomcat:jdk11-openjdk-slim as base image for webapp
k8s: Building container image in Kubernetes mode
k8s: [kubernetes/webapp-custom:latest] "webapp": Created docker-build.tar in 15 milliseconds
k8s: [kubernetes/webapp-custom:latest] "webapp": Built image sha256:7d48e
k8s: [kubernetes/webapp-custom:latest] "webapp": Removed old image sha256:c68c7
k8s: [kubernetes/webapp-custom:latest] "webapp": Tag with latest

BUILD SUCCESSFUL in 2s

$ docker images |  grep webapp-custom
kubernetes/webapp-custom                                latest               7d48e93746b8   45 seconds ago   450MB

```

## Generate Kubernetes Manifests
```
$ ./gradlew k8sResource -Djkube.createExternalUrls=true -Djkube.domain=$(minikube ip).nip.io
> Task :k8sResource
k8s: Running generator webapp
k8s: webapp: Using tomcat:jdk11-openjdk-slim as base image for webapp
k8s: Using resource templates from /home/sunix/github/eclipse/jkube/quickstarts/gradle/webapp-custom/src/main/jkube
k8s: jkube-controller: Adding a default Deployment
k8s: jkube-service: Adding a default service 'webapp-custom' with ports [8080]
k8s: jkube-service-discovery: Using first mentioned service port '8080' 
k8s: jkube-revision-history: Adding revision history limit to 2
k8s: validating /home/sunix/github/eclipse/jkube/quickstarts/gradle/webapp-custom/build/classes/java/main/META-INF/jkube/kubernetes/webapp-custom-ingress.yml resource
Unknown keyword $module - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword
Unknown keyword existingJavaType - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword
Unknown keyword javaOmitEmpty - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword
k8s: validating /home/sunix/github/eclipse/jkube/quickstarts/gradle/webapp-custom/build/classes/java/main/META-INF/jkube/kubernetes/webapp-custom-deployment.yml resource
k8s: validating /home/sunix/github/eclipse/jkube/quickstarts/gradle/webapp-custom/build/classes/java/main/META-INF/jkube/kubernetes/webapp-custom-service.yml resource

BUILD SUCCESSFUL in 3s
1 actionable task: 1 executed

$ ls build/classes/java/main/META-INF/jkube/
kubernetes  kubernetes.yml

$ ls build/classes/java/main/META-INF/jkube/kubernetes
webapp-custom-deployment.yml  webapp-custom-ingress.yml  webapp-custom-service.yml
```

## Apply Generated Manifests onto Kubernetes Cluster
```
./gradlew k8sApply

> Task :k8sApply
k8s: Running generator webapp
k8s: webapp: Using tomcat:jdk11-openjdk-slim as base image for webapp
k8s: Using Kubernetes at https://192.168.99.113:8443/ in namespace null with manifest /home/sunix/github/eclipse/jkube/quickstarts/gradle/webapp-custom/build/classes/java/main/META-INF/jkube/kubernetes.yml 
k8s: Creating a Service from kubernetes.yml namespace default name webapp-custom
k8s: Created Service: build/jkube/applyJson/default/service-webapp-custom-8.json
k8s: Creating a Deployment from kubernetes.yml namespace default name webapp-custom
k8s: Created Deployment: build/jkube/applyJson/default/deployment-webapp-custom-9.json
k8s: Applying Ingress webapp-custom from kubernetes.yml
k8s: HINT: Use the command `kubectl get pods -w` to watch your pods start up

BUILD SUCCESSFUL in 6s
1 actionable task: 1 executed

$ kubectl get pods
NAME                             READY   STATUS    RESTARTS   AGE
webapp-custom-7d79679f6b-r2j9k   1/1     Running   0          27s

```

## Access application running inside Kubernetes

Make sure the ingress resource has been created
```
$ kubectl get ingress
NAME            CLASS    HOSTS                                 ADDRESS   PORTS   AGE
webapp-custom   <none>   webapp-custom.192.168.99.113.nip.io             80      50s
```

Give it a try:
```
$ lynx --dump webapp-custom.192.168.99.113.nip.io
Eclipse JKube rocks!!
```

