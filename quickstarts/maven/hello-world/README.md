---
name: "Maven :: Hello World"
description: |
  Demo project for getting started with Eclipse JKube.
  It just prints "Hello World" on command line and exits.
---
# JKube Hello World Sample

This is a demo project for getting started with Eclipse JKube. It just prints "Hello World" on command
line and exits. We would be using Eclipse JKube for building a docker image and deploying to Kubernetes
in single command.

1. Make sure you've minikube up and running.
2. Configure your local environment to re-use the Docker daemon inside the Minikube instance.
```shell
~ jkube/quickstarts/maven/hello-world : $ eval $(minikube -p minikube docker-env) 
```
3. Run the following command to run helloworld sample: 
```
~/work/repos/jkube/quickstarts/maven/hello-world : $ mvn clean install k8s:build k8s:resource k8s:apply
[INFO] Scanning for projects...
[INFO] 
[INFO] -----------< org.eclipse.jkube.quickstarts.maven:helloworld >-----------
[INFO] Building Eclipse JKube :: Quickstarts :: Maven :: Hello World 1.16.1
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- k8s:1.16.1:build (default-cli) @ helloworld ---
[INFO] k8s: Building Docker image
[INFO] k8s: [helloworld-java:1.16.1] "hello-world": Created docker-build.tar in 78 milliseconds
[INFO] k8s: [helloworld-java:1.16.1] "hello-world": Built image sha256:53a82
[INFO] 
[INFO] --- k8s:1.16.1:resource (default-cli) @ helloworld ---
[INFO] k8s: Using resource templates from /Users/jj/Documents/Open-source/jkube/quickstarts/maven/hello-world/src/main/jkube
[INFO] k8s: jkube-controller: Adding a default Deployment
[INFO] k8s: jkube-service: Adding a default service 'helloworld' with ports [8080]
[INFO] k8s: jkube-service-discovery: Using first mentioned service port '8080' 
[INFO] k8s: jkube-revision-history: Adding revision history limit to 2
[INFO] k8s: validating /Users/jj/Documents/Open-source/jkube/quickstarts/maven/hello-world/target/classes/META-INF/jkube/kubernetes/helloworld-service.yml resource
[INFO] k8s: validating /Users/jj/Documents/Open-source/jkube/quickstarts/maven/hello-world/target/classes/META-INF/jkube/kubernetes/helloworld-deployment.yml resource
[INFO] 
[INFO] --- k8s:1.16.1:apply (default-cli) @ helloworld ---
[INFO] k8s: Using Kubernetes at https://127.0.0.1:50097/ in namespace null with manifest /Users/jj/Documents/Open-source/jkube/quickstarts/maven/hello-world/target/classes/META-INF/jkube/kubernetes.yml 
[INFO] k8s: Updating Service from kubernetes.yml
[INFO] k8s: Updated Service: target/jkube/applyJson/default/service-helloworld.json
[INFO] k8s: Updating Deployment from kubernetes.yml
[INFO] k8s: Updated Deployment: target/jkube/applyJson/default/deployment-helloworld.json
[INFO] k8s: HINT: Use the command `kubectl get pods -w` to watch your pods start up
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  8.286 s
[INFO] Finished at: 2024-03-12T18:49:36+03:00
[INFO] ------------------------------------------------------------------------
``` 

4. Check logs of Created Pod:
```
~ jkube/quickstarts/maven/hello-world : $ kubectl get pods
NAME                          READY   STATUS    RESTARTS   AGE
helloworld-664bf5fdff-2bmrt   1/1     Running   0          9s
~ jkube/quickstarts/maven/hello-world : $ kubectl get svc
helloworld   NodePort    10.110.92.145   <none>        8080:32353/TCP   58m
kubernetes   ClusterIP   10.96.0.1       <none>        443/TCP          7h
~ jkube/quickstarts/maven/hello-world : $ curl `minikube ip`:32353/hello
Hello World
```