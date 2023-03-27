---
name: "Gradle :: dapr-hello-world"

description: 

  Dapr hello world application deployed in Kubernetes using Eclipse JKube.
  
  The application contains a single controller HelloWorldController.java that prints 'Hello World'.

---

# Eclipse JKube Dapr Gradle Quickstart

This is a demo gradle application based on Dapr framework which can be deployed to 
Kubernetes with the help of Kubernetes JKube Gradle Plugin.

## How to Build?
In order to build,you will need to install Dapr in kubernetes

- Install Dapr in kubernetes
  ```shell script
  $ dapr init -k
  ```

- Verify installation
  ```shell script
  $ dapr status -k
  ```

- If required, update installation
  ```shell script
  $ dapr upgrade -k --runtime-version=x.x.x
  ```

## Deploying to Kubernetes
Kubernetes Gradle Plugin is already added to the project plugins section.
You can deploy it using the usual Kubernetes Gradle Plugin tasks, just make sure you are logged into
a Kubernetes cluster:

For the Dapr application to work in a Kubernetes cluster it needs following annotation in the `deployment.yaml` file under spec, template metadata section 

```yaml
annotations:
  dapr.io/enabled: "true"
  dapr.io/app-id: "dapr-hello-world"
  dapr.io/app-port: "8085"
  dapr.io/config: "appconfig"
```
          
In order to deploy the application, you need to run the Kubernetes Gradle Plugin build, resource, and apply goals:

```shell script
# Kubernetes
$ gradle clean build k8sBuild k8sResource k8sApply
# OpenShift
$ gradle clean build ocBuild ocResource ocApply
```

If you are working with slow internet connection then readiness and liveness probe may timeout uncomment timeout and delay related annotation from above

If you need time observability in your application you can use zipkin and Dapr appconfig, installation instruction for zipkin with appconfig is given in zipkininstallation.sh

