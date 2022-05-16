---
name: "Gradle :: Webapp"
description: |
  Java Web Application with a static index.html resource.
  Demonstrates how to create a container image with an embedded Apache Tomcat server using Eclipse JKube.
---
# Eclipse JKube Webapp
This quick start showcases how to use Eclipse JKube with a war project to build a container image. 
The webapp generator tries to detect WAR builds and selects a base servlet container image based on the configuration found in the `build.gradle`. A Tomcat base image is selected by default.
It is also possible to override the default base image with a Jetty or Wildfly image.


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
k8s: Running in Kubernetes mode
k8s: Running generator webapp
k8s: webapp: Using quay.io/jkube/jkube-tomcat9:0.0.13 as base image for webapp
k8s: Building container image in Kubernetes mode
k8s: [kubernetes/webapp:latest] "webapp": Created docker-build.tar in 6 milliseconds
k8s: [kubernetes/webapp:latest] "webapp": Built image sha256:78b98
k8s: [kubernetes/webapp:latest] "webapp": Removed old image sha256:79e56
k8s: [kubernetes/webapp:latest] "webapp": Tag with latest

BUILD SUCCESSFUL in 1s

$ docker images |  grep webapp
kubernetes/webapp                                       latest     78b980e82095   48 seconds ago   525MB

```
### Using Jetty or Wildfly instead of Tomcat base image
Alternatively, you can choose to use a Jetty or Wildfly:

In `build.gradle`, uncomment this part:

```
kubernetes {
    generator {
        config {
            'webapp' {
                server = "jetty"
            }
        }
    }
}
```
Run
```
$ ./gradlew build k8sBuild

> Task :k8sBuild
k8s: Running in Kubernetes mode
k8s: Running generator webapp
k8s: webapp: Using quay.io/jkube/jkube-jetty9:0.0.13 as base image for webapp
k8s: Building container image in Kubernetes mode
k8s: [kubernetes/webapp:latest] "webapp": Created docker-build.tar in 5 milliseconds
k8s: [kubernetes/webapp:latest] "webapp": Built image sha256:5e016
k8s: [kubernetes/webapp:latest] "webapp": Removed old image sha256:78b98
k8s: [kubernetes/webapp:latest] "webapp": Tag with latest

BUILD SUCCESSFUL in 1s

```

It is now using `quay.io/jkube/jkube-jetty9:0.0.13` as base image!

## Generate Kubernetes Manifests
```
$ ./gradlew k8sResource -Djkube.domain=$(minikube ip).nip.io
> Task :k8sResource
k8s: Running in Kubernetes mode
k8s: Running generator webapp
k8s: webapp: Using quay.io/jkube/jkube-jetty9:0.0.13 as base image for webapp
k8s: Using resource templates from /home/sunix/github/eclipse/jkube/quickstarts/gradle/webapp/src/main/jkube
k8s: jkube-controller: Adding a default Deployment
k8s: jkube-service: Adding a default service 'webapp' with ports [8080]
k8s: jkube-service-discovery: Using first mentioned service port '8080' 
k8s: jkube-revision-history: Adding revision history limit to 2
k8s: validating /home/sunix/github/eclipse/jkube/quickstarts/gradle/webapp/build/classes/java/main/META-INF/jkube/kubernetes/webapp-deployment.yml resource
Unknown keyword $module - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword
Unknown keyword existingJavaType - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword
Unknown keyword javaOmitEmpty - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword
k8s: validating /home/sunix/github/eclipse/jkube/quickstarts/gradle/webapp/build/classes/java/main/META-INF/jkube/kubernetes/webapp-service.yml resource
k8s: validating /home/sunix/github/eclipse/jkube/quickstarts/gradle/webapp/build/classes/java/main/META-INF/jkube/kubernetes/webapp-ingress.yml resource

BUILD SUCCESSFUL in 2s
1 actionable task: 1 executed

$ ls build/classes/java/main/META-INF/jkube/
kubernetes  kubernetes.yml

$ ls build/classes/java/main/META-INF/jkube/kubernetes
webapp-deployment.yml  webapp-ingress.yml  webapp-service.yml
```

## Apply Generated Manifests onto Kubernetes Cluster
```
./gradlew k8sApply

> Task :k8sApply
k8s: Running in Kubernetes mode
k8s: Running generator webapp
k8s: webapp: Using quay.io/jkube/jkube-jetty9:0.0.13 as base image for webapp
k8s: Using Kubernetes at https://192.168.99.115:8443/ in namespace null with manifest /home/sunix/github/eclipse/jkube/quickstarts/gradle/webapp/build/classes/java/main/META-INF/jkube/kubernetes.yml 
k8s: Creating a Service from kubernetes.yml namespace default name webapp
k8s: Created Service: build/jkube/applyJson/default/service-webapp-2.json
k8s: Creating a Deployment from kubernetes.yml namespace default name webapp
k8s: Created Deployment: build/jkube/applyJson/default/deployment-webapp-2.json
k8s: Applying Ingress webapp from kubernetes.yml
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
NAME     CLASS    HOSTS                          ADDRESS          PORTS   AGE
webapp   <none>   webapp.192.168.99.115.nip.io   192.168.99.115   80      56s
```

Give it a try:
```
$ lynx --dump webapp.192.168.99.115.nip.io
Eclipse JKube rocks!!
```

# Red Hat Developer Sandbox

## Prerequisites
- Create an account here: https://developers.redhat.com/developer-sandbox/get-started
- Install `oc`
- Login with `oc` see https://developers.redhat.com/blog/2021/04/21/access-your-developer-sandbox-for-red-hat-openshift-from-the-command-line#

## Build the container image, generates the k8s resources and apply
Run the following command to build the container image, generates the k8s resources and apply it in the Red Hat Developer Sandbox Openshift cluster:

```
$ ./gradlew ocBuild ocResource ocApply
```

Run the following command to get the URL of the application:
```
$ oc get route
NAME      HOST/PORT                                                    PATH      SERVICES   PORT      TERMINATION   WILDCARD
webapp    webapp-sutan-dev.apps.sandbox-m2.ll9k.p1.openshiftapps.com             webapp     8080                    None
```
