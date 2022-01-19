# Eclipse JKube OpenLiberty Gradle Quickstart

This is a demo gradle application based on OpenLiberty framework which can be deployed to Kubernetes with the help of Kubernetes Gradle Plugin. 

## How to Build?
In order to build, you'll need to run this command:
```shell
./gradlew clean build
```

## Deploying to Kubernetes
Kubernetes Gradle Plugin is already added to the project plugins section. You can deploy it using the usual Kubernetes Gradle Plugin tasks, just make sure you are logged into a Kubernetes cluster:
```shell
./gradlew k8sBuild k8sResource k8sApply
```

