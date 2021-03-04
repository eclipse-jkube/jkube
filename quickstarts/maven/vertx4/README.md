# Eclipse JKube Vert.x 4 Web Quickstart
Eclipse Vert.x 4 Web example application declaring a Simple AbstractVerticle.

### Steps to use

Make sure that Kubernetes/OpenShift cluster or Minikube/minishift is running. 

#### For OpenShift
```shell
$ minishift start
```
Below command will create your OpenShift resource descriptors.
```shell
$ mvn -Popenshift clean oc:resource
```

Now start S2I build  by hitting the build goal.
```shell

$ mvn -Popenshift package oc:build
```

Below command will deploy your application on OpenShift cluster.
```shell
$ mvn -Popenshift oc:apply
```

#### For Kubernetes
Start your cluster:
```shell
$ minikube start
```
Below command will create your Kubernetes resource descriptors.
```shell
$ mvn -Pkubernetes clean k8s:resource
```

Now start docker build  by hitting the build goal.
```shell
$ mvn -Pkubernetes package k8s:build
```

Below command will deploy your application on Kubernetes cluster.
```shell
$ mvn -Pkubernetes k8s:deploy
```
