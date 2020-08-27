# Spring Boot Sample

This is a sample project to use Eclipse JKube plugins.

### Steps to use

Make sure that Kubernetes cluster or Minikube is running. 


#### For Kubernetes

Below command will create your OpenShift resource descriptors.
```
mvn clean k8s:resource
```

Now start docker build  by hitting the build goal.
```
mvn package k8s:build
```

Below command will deploy your application on OpenShift cluster.
```
mvn k8s:deploy
```

You can now start watch goal by running.
```
mvn k8s:watch
```

If your source code is changed and recompiled (`mvn package`), application in the cluster should live-reload.
