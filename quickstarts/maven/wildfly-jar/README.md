---
name: "Maven :: Wildfly JAR"
description: Java web application with a single JAX-RS endpoint packaged with WildFly Jar Maven Plugin.
---
# Wildfly JAR Sample

This is a sample project to use Eclipse JKube plugins.

### Steps to use

Make sure that Kubernetes/OpenShift cluster or Minikube/minishift is running. In case, if anything of this is not running, you can
run minishift/minikube to test this application by using following command.



#### For OpenShift
```
minishift start
```

Below command will build and deploy your application on OpenShift cluster.
```
mvn oc:deploy -Popenshift
```

#### For Kubernetes
Start your cluster:
```
minikube start
```
Below command will create your OpenShift resource descriptors.
```
mvn clean k8s:resource -Pkubernetes
```

Now start docker build  by hitting the build goal.
```
mvn package k8s:build -Pkubernetes
```

Below command will deploy your application on OpenShift cluster.
```
mvn k8s:apply -Pkubernetes
```
