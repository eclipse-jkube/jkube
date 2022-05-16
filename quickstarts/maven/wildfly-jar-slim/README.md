---
name: "Maven :: Wildfly JAR :: Slim"
description: Java web application with a single JAX-RS endpoint packaged with WildFly Jar Maven Plugin.
---
# Wildfly JAR Slim Server Sample

This is a sample project to use Eclipse JKube plugins.

This is an example of using `jboss-maven-dist` and `jboss-maven-repo` bootable JAR Maven plugin options in order to store 
all the JBoss Modules maven artifacts used by the Bootable Jar in an external maven repository. 
The generated maven repository is copied into the generated docker image by the JKube plugin. 
Externalizing the JBoss modules artifacts makes for a faster JAR start-up.


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
