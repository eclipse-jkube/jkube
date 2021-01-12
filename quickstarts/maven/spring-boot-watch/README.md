# Spring Boot Sample

This is a sample project to use Eclipse JKube plugins.

### Steps to use

Make sure that Kubernetes cluster or Minikube is running. 


#### For Kubernetes

Below command will create your Kubernetes resource descriptors.
```
$ mvn clean k8s:resource
```

Now start docker build  by hitting the build goal.
```
$ mvn package k8s:build
```

You can now start watch goal by running. This command will apply the application
resources to the cluster and start a watcher thread.
```
$ mvn k8s:watch
```

If your source code is changed and recompiled (run `mvn package` in another shell session),
the application in the cluster should live-reload.
