---
name: "Maven :: Hello World"
description: |
  Demo project for getting started with Eclipse JKube.
  It starts a simple HTTP server on port 8080 that replies with "Hello World" to the /hello endpoint.
---
# JKube Hello World Sample

This is a demo project for getting started with Eclipse JKube.
It starts a simple HTTP server on port 8080.
Performing a request to `/hello` endpoint will reply with "Hello World".
We will be using Eclipse JKube for building a docker image and deploying to Kubernetes with a single command.

## Prerequisites

- Java 8+
- Maven
- Minikube
- JKube

## Deploying Demo app to Kubernetes with JKube

1. Make sure you've [Minikube](https://minikube.sigs.k8s.io/docs/start/) up and running.
   ```shell
   minikube start
   ```

2. Configure your local environment to re-use the Docker daemon inside the Minikube instance.
   ```shell
    $ eval $(minikube -p minikube docker-env) 
   ```

3. Run the following command to run and deploy hello-world demo app to Kubernetes
   ```shell
   $ mvn clean install k8s:build k8s:resource k8s:apply
   ```

4. Check logs of the created Pod
   ```
   $ kubectl get pods
   NAME                          READY   STATUS    RESTARTS   AGE
   helloworld-664bf5fdff-2bmrt   1/1     Running   0          9s
   ```

5. Log the running Kubernetes services
   ```shell
   $ kubectl get svc
   helloworld   NodePort    10.110.92.145   <none>        8080:32353/TCP   58m
   kubernetes   ClusterIP   10.96.0.1       <none>        443/TCP          7h
   ```

6. Call the `/hello` endpoint
   ```shell
   $ curl `minikube ip`:32353/hello
   Hello World
   ```
