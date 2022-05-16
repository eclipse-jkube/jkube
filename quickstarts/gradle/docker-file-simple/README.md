---
name: "Gradle :: Docker File Simple"
description: |
  Spring Boot Web application with a single @RestController.
  Shows how to deploy Spring Boot applications to Kubernetes using existing Dockerfile with Eclipse JKube.
---
# Eclipse JKube sample with simple Dockerfile mode

You can build your Docker images with just providing one `Dockerfile` in your project root directory and Eclipse JKube
will pick it up for docker build. 
For simple `Dockerfile` mode, your project's current directory is provided as docker context directory for build.
If you want to copy some files from your current directory(`build/libs/docker-file-simple.jar` in this case), you need
to prefix project directory contents with default assembly name(i.e `maven`). So your `Dockerfile` would look like this:
```Dockerfile
FROM quay.io/jkube/jkube-java:0.0.13
ENV JAVA_APP_DIR=/deployments
EXPOSE 8080 8778 9779
COPY maven/build/libs/docker-file-simple-0.0.1-SNAPSHOT.jar /deployments/
```

# Building Docker image
```shell script
docker-file-simple : $ gradle build k8sBUild

> Task :k8sBuild
k8s: Cannot access cluster for detecting mode: Unknown host kubernetes.default.svc: Name or service not known
k8s: Running in Kubernetes mode
k8s: Building Docker image in Kubernetes mode
k8s: Pulling from jkube/jkube-java-binary-s2i
k8s: Digest: sha256:dd5c9f44a86e19438662d293e180acc8d864887cf19c165c1b24ae703b16c2d4
k8s: Status: Image is up to date for quay.io/jkube/jkube-java:0.0.13
k8s: Pulled quay.io/jkube/jkube-java:0.0.13 in 3 seconds
k8s: [helloworld/docker-file-simple:latest]: Created docker-build.tar in 94 milliseconds
k8s: [helloworld/docker-file-simple:latest]: Built image sha256:4b6b1

BUILD SUCCESSFUL in 5s
5 actionable tasks: 1 executed, 4 up-to-date
docker-file-simple : $ docker images | grep docker-file-simple
helloworld/docker-file-simple                                                latest                  45eaf08bcec1   15 seconds ago   517MB
```

# Generating Kubernetes Manifests and Deploying to Kubernetes
```shell script
```

