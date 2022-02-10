# Eclipse JKube Webapp Wildfly Sample
This quick start showcases how to use Eclipse JKube with a Wildfly war project.
Demonstrates how to create a container image with an embedded WildFly server using Eclipse JKube.
WildFly is used instead of Apache Tomcat because there is a WildFly persistence.xml and -ds.xml configuration.
Eclipse JKube detects this file and chooses a WildFly specific base container image.

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
k8s: webapp: Using jboss/wildfly:25.0.0.Final as base image for webapp
k8s: Building container image in Kubernetes mode
k8s: Pulling from jboss/wildfly7s]
k8s: Digest: sha256:35320abafdec6d360559b411aff466514d5741c3c527221445f48246350fdfe5
k8s: Status: Downloaded newer image for jboss/wildfly:25.0.0.Final
k8s: Pulled jboss/wildfly:25.0.0.Final in 16 seconds 
k8s: [kubernetes/webapp-wildfly:latest] "webapp": Created docker-build.tar in 51 milliseconds
k8s: [kubernetes/webapp-wildfly:latest] "webapp": Built image sha256:622fa
k8s: [kubernetes/webapp-wildfly:latest] "webapp": Tag with latest

BUILD SUCCESSFUL in 25s
4 actionable tasks: 2 executed, 2 up-to-date

$ docker images |  grep webapp-wildfly
kubernetes/webapp-wildfly                               latest         622fa19f80b5   About a minute ago   737MB

```

## Generate Kubernetes Manifests
```
$ ./gradlew k8sResource -Djkube.createExternalUrls=true -Djkube.domain=$(minikube ip).nip.io
> Task :k8sResource
k8s: Running in Kubernetes mode
k8s: Running generator webapp
k8s: webapp: Using jboss/wildfly:25.0.0.Final as base image for webapp
k8s: Using resource templates from /home/sunix/github/eclipse/jkube/quickstarts/gradle/webapp-wildfly/src/main/jkube
k8s: jkube-controller: Adding a default Deployment
k8s: jkube-service: Adding a default service 'webapp-wildfly' with ports [8080]
k8s: jkube-service-discovery: Using first mentioned service port '8080' 
k8s: jkube-revision-history: Adding revision history limit to 2
k8s: validating /home/sunix/github/eclipse/jkube/quickstarts/gradle/webapp-wildfly/build/classes/java/main/META-INF/jkube/kubernetes/webapp-wildfly-deployment.yml resource
Unknown keyword $module - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword
Unknown keyword existingJavaType - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword
Unknown keyword javaOmitEmpty - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword
k8s: validating /home/sunix/github/eclipse/jkube/quickstarts/gradle/webapp-wildfly/build/classes/java/main/META-INF/jkube/kubernetes/webapp-wildfly-ingress.yml resource
k8s: validating /home/sunix/github/eclipse/jkube/quickstarts/gradle/webapp-wildfly/build/classes/java/main/META-INF/jkube/kubernetes/webapp-wildfly-service.yml resource

BUILD SUCCESSFUL in 2s
1 actionable task: 1 executed

$ ls build/classes/java/main/META-INF/jkube/
kubernetes  kubernetes.yml

$ ls build/classes/java/main/META-INF/jkube/kubernetes
webapp-wildfly-deployment.yml  webapp-wildfly-ingress.yml  webapp-wildfly-service.yml

```

## Apply Generated Manifests onto Kubernetes Cluster
```
./gradlew k8sApply

> Task :k8sApply
k8s: Running in Kubernetes mode
k8s: Running generator webapp
k8s: webapp: Using jboss/wildfly:25.0.0.Final as base image for webapp
k8s: Using Kubernetes at https://192.168.99.110:8443/ in namespace null with manifest /home/sunix/github/eclipse/jkube/quickstarts/gradle/webapp-wildfly/build/classes/java/main/META-INF/jkube/kubernetes.yml 
k8s: Creating a Service from kubernetes.yml namespace default name webapp-wildfly
k8s: Created Service: build/jkube/applyJson/default/service-webapp-wildfly-1.json
k8s: Creating a Deployment from kubernetes.yml namespace default name webapp-wildfly
k8s: Created Deployment: build/jkube/applyJson/default/deployment-webapp-wildfly-1.json
k8s: Applying Ingress webapp-wildfly from kubernetes.yml
k8s: HINT: Use the command `kubectl get pods -w` to watch your pods start up

BUILD SUCCESSFUL in 5s
1 actionable task: 1 executed

$ kubectl get pods
NAME                             READY   STATUS    RESTARTS   AGE
webapp-wildfly-5c8446cfb-7z7s7   1/1     Running   0          58s

```

## Access application running inside Kubernetes

Make sure the ingress resource has been created
```
$ kubectl get ingress
kubectl get ingress
NAME             CLASS    HOSTS                                  ADDRESS          PORTS   AGE
webapp-wildfly   <none>   webapp-wildfly.192.168.99.110.nip.io   192.168.99.110   80      110s
```


Give it a try:
```
$ lynx --dump webapp-wildfly.192.168.99.110.nip.io
☕🧊 Eclipse JKube commiters

     Username   First name Last name
   manusa       Marc       Nuri
   rohanKanojia Rohan      Kumar
   sunix        Sun        Tan
```

