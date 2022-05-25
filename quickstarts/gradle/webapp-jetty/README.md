---
name: "Gradle :: Webapp :: Jetty"
description: |
  Java Web Application with a static index.html resource.
  Demonstrates building container image with war project.
  It also tries to detect WAR builds and select container image based on configuration specified in build.gradle
---
# Eclipse JKube Webapp - Jetty
This quick start showcases how to use Eclipse JKube with a war project to build a container image. 
The webapp generator tries to detect WAR builds and selects a base servlet container image based on the configuration found in the `build.gradle`. A Tomcat base image is selected by default.
It is also possible to override the default base image with a Jetty or Wildfly image.

In this sample, JKube detects the `META-INF/jetty-logging.properties` files and will pick a jetty base image.


# Minikube

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
k8s: Running generator webapp [6s]
k8s: webapp: Using quay.io/jkube/jkube-jetty9:0.0.13 as base image for webapp
k8s: Building container image in Kubernetes mode
k8s: Pulling from jkube/jkube-jetty9
k8s: Digest: sha256:d0e6872a95a82fb80fef8e5a653baae3e4866812dc739f9fc8012514f3a52327
k8s: Status: Downloaded newer image for quay.io/jkube/jkube-jetty9:0.0.13
k8s: Pulled quay.io/jkube/jkube-jetty9:0.0.13 in 15 seconds 
k8s: [kubernetes/webapp-jetty:latest] "webapp": Created docker-build.tar in 41 milliseconds
k8s: [kubernetes/webapp-jetty:latest] "webapp": Built image sha256:d0d52
k8s: [kubernetes/webapp-jetty:latest] "webapp": Tag with latest

BUILD SUCCESSFUL in 24s
3 actionable tasks: 3 executed

$ docker images |  grep webapp
kubernetes/webapp-jetty                                 latest     d0d528ec103e   38 seconds ago   431MB

```

## Generate Kubernetes Manifests
```
$ ./gradlew k8sResource -Djkube.domain=$(minikube ip).nip.io
> Task :k8sResource
k8s: Running generator webapp
k8s: webapp: Using quay.io/jkube/jkube-jetty9:0.0.13 as base image for webapp
k8s: Using resource templates from /home/sunix/github/eclipse/jkube/quickstarts/gradle/webapp-jetty/src/main/jkube
k8s: jkube-controller: Adding a default Deployment
k8s: jkube-service: Adding a default service 'webapp-jetty' with ports [8080]
k8s: jkube-service-discovery: Using first mentioned service port '8080' 
k8s: jkube-revision-history: Adding revision history limit to 2
k8s: validating /home/sunix/github/eclipse/jkube/quickstarts/gradle/webapp-jetty/build/classes/java/main/META-INF/jkube/kubernetes/webapp-jetty-deployment.yml resource
Unknown keyword $module - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword
Unknown keyword existingJavaType - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword
Unknown keyword javaOmitEmpty - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword
k8s: validating /home/sunix/github/eclipse/jkube/quickstarts/gradle/webapp-jetty/build/classes/java/main/META-INF/jkube/kubernetes/webapp-jetty-service.yml resource
k8s: validating /home/sunix/github/eclipse/jkube/quickstarts/gradle/webapp-jetty/build/classes/java/main/META-INF/jkube/kubernetes/webapp-jetty-ingress.yml resource

BUILD SUCCESSFUL in 2s
1 actionable task: 1 executed

$ ls build/classes/java/main/META-INF/jkube/
kubernetes  kubernetes.yml

$ ls build/classes/java/main/META-INF/jkube/kubernetes
webapp-jetty-deployment.yml  webapp-jetty-ingress.yml  webapp-jetty-service.yml
```

## Apply Generated Manifests onto Kubernetes Cluster
```
./gradlew k8sApply

> Task :k8sApply
k8s: Running generator webapp
k8s: webapp: Using quay.io/jkube/jkube-jetty9:0.0.13 as base image for webapp
k8s: Using Kubernetes at https://192.168.99.120:8443/ in namespace null with manifest /home/sunix/github/eclipse/jkube/quickstarts/gradle/webapp-jetty/build/classes/java/main/META-INF/jkube/kubernetes.yml 
k8s: Creating a Service from kubernetes.yml namespace default name webapp-jetty
k8s: Created Service: build/jkube/applyJson/default/service-webapp-jetty.json
k8s: Creating a Deployment from kubernetes.yml namespace default name webapp-jetty
k8s: Created Deployment: build/jkube/applyJson/default/deployment-webapp-jetty.json
k8s: Applying Ingress webapp-jetty from kubernetes.yml
k8s: HINT: Use the command `kubectl get pods -w` to watch your pods start up

BUILD SUCCESSFUL in 5s
1 actionable task: 1 executed

$ kubectl get pods
NAME                    READY   STATUS    RESTARTS   AGE
webapp-7fd8bd64-qjc2c   1/1     Running   0          37s

```

## Access application running inside Kubernetes

Make sure the ingress resource has been created
```
$ kubectl get ingress
NAME           CLASS    HOSTS                                ADDRESS          PORTS   AGE
webapp-jetty   <none>   webapp-jetty.192.168.99.120.nip.io   192.168.99.120   80      53s
```

Give it a try:
```
$ lynx --dump webapp-jetty.192.168.99.120.nip.io
Eclipse JKube on Jetty rocks!!
```

# Red Hat Developer Sandbox

## Prerequisites
- Create an account here: https://developers.redhat.com/developer-sandbox/get-started
- Install `oc`
- Login with `oc` see https://developers.redhat.com/blog/2021/04/21/access-your-developer-sandbox-for-red-hat-openshift-from-the-command-line#

## Build the container image, generates the k8s resources and apply
Run the following command to build the container image, generates the k8s resources and apply it in the Red Hat Developer Sandbox Openshift cluster:

```
$ ./gradlew build ocBuild ocResource ocApply
```

Run the following command to get the URL of the application:
```
$ oc get route
NAME           HOST/PORT                                                          PATH      SERVICES       PORT      TERMINATION   WILDCARD
webapp-jetty   webapp-jetty-sutan-dev.apps.sandbox-m2.ll9k.p1.openshiftapps.com             webapp-jetty   8080                    None
```

Give it a try:
```
$ lynx --dump webapp-jetty-sutan-dev.apps.sandbox-m2.ll9k.p1.openshiftapps.com
Eclipse JKube on Jetty rocks!!
```
