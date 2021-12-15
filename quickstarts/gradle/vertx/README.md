# Vertx sample for Eclipse JKube gradle plugins
This is a sample project to use Eclipse JKube plugins.

### Steps to use

 Make sure that Kubernetes/OpenShift cluster or CRC/minishift is running. In case, if anything of this is not running, you can
run CRC/minikube to test this application by using following command.



#### For OpenShift
```
crc start
```
Below command will create your OpenShift resource descriptors.
```
gradle build ocResource
```

 Now start S2I build  by hitting the build task.
```
gradle ocBuild
```

 Below command will deploy your application on OpenShift cluster.
```
gradle ocApply
```

#### For Kubernetes
Start your cluster:
```
minikube start
```
Below command will create your Kubernetes resource descriptors.
```
gradle build k8sResource
```

Now start docker build  by hitting the build task.
```
gradle k8sBuild
```

Below command will deploy your application on Kubernetes cluster.
```
gradle k8sApply
```


