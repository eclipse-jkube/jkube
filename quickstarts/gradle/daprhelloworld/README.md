---
name: "Gradle :: daprhelloworld"

description: 

  dapr hello world application deployed in kubernetes using jkube.
  
  The application contains a single controller HelloWorldController.java  that prints 'Hello World'.
  
---

# Eclipse JKube DAPR Gradle Quickstart

This is a demo gradle application based on Dapr framework which can be deployed to 
Kubernetes with the help of Kubernetes jkube Gradle Plugin.

## How to Build?
In order to build,you will need to install dapr in kubernetes

--Install dapr in kubernetes

dapr init -k

--verify installation

dapr status -k

--if required update installation

dapr upgrade -k --runtime-version=x.x.x

## Deploying to Kubernetes
Kubernetes Gradle Plugin is already added to the project plugins section.
You can deploy it using the usual Kubernetes Gradle Plugin tasks, just make sure you are logged into
a Kubernetes cluster:

in order dapr application to work in kubernetes cluster it needs following annotation in deployment.yaml file under spec, template metadata section 

annotations:

          dapr.io/enabled: "true"
          
          dapr.io/app-id: "daprhelloworld"
          
          dapr.io/app-port: "8085"

          dapr.io/config: "appconfig"
          
If you are working with slow internet connection then rediness and livebess probe may timeout uncomment timeout and delay related annotation from above

If you need time obervability in your application you can use zipkin and dapr appconfig, installation instruction for zipkin with appconfig is given in zipkininstallation.txt

