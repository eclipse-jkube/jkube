# Eclipse JKube External Resources Quickstart

This is a simple SpringBoot application which demonstrates how you can use Eclipse JKube's
resource fragments to customize final generated Kubernetes/OpenShift manifests. You can have
a look at the resources placed in `src/main/jkube` directory
```
~/work/repos/jkube/quickstarts/maven/external-resources : $ ls src/main/jkube/
configmap.yml  deployment.yml  sa.yml  service.yml
```

Plugin would read this and create an additional `ConfigMap` and `ServiceAccount`, it would merge
the `Deployment` fragment into final generated `Deployment`(or `DeploymentConfig` in case of 
OpenShift). Similarly `Service` would be merged too.

# Deploying on Kubernetes(Plain Docker Build)
For deploying onto Kubernetes please run these goals, make sure you have exposed your minikube's docker daemon with `minikube docker-env` :
```
mvn k8s:build k8s:resource k8s:apply -Pkubernetes
```
Once application is deployed check whether application is accessible:
```
~/work/repos/jkube/quickstarts/maven/external-resources : $ kubectl get pods  -w
NAME                                 READY   STATUS    RESTARTS   AGE
external-resources-dc6dbc499-c2xnk   0/1     Running   0          18s
external-resources-dc6dbc499-c2xnk   1/1     Running   0          19s
~/work/repos/jkube/quickstarts/maven/external-resources : $ kubectl get svc
NAME                 TYPE           CLUSTER-IP      EXTERNAL-IP   PORT(S)          AGE
external-resources   LoadBalancer   10.103.39.113   <pending>     8080:30263/TCP   23s
kubernetes           ClusterIP      10.96.0.1       <none>        443/TCP          5d5h
~/work/repos/jkube/quickstarts/maven/external-resources : $ curl `minikube ip`:30263/hello
Hello from Kubernetes ConfigMap!!!
```

Once everything is done, clean up using undeploy goal:
```
mvn k8s:undeploy -Pkubernetes
```

# Deploying on Kubernetes(JIB Build)
For building with JIB mode, you would need to specify one additional argument `jkube.build.strategy=jib`:
```
mvn k8s:build k8s:push k8s:resource k8s:apply -Djkube.build.strategy=jib -Djkube.generator.name="<full-image-name-with-registry-and-user>" -Pkubernetes
```
Note that you would need to provide your registry credentials either in maven settings(`~/.m2/settings.xml`), plugin `<authConfig>` section or via `docker login`.

Once deployed you can access your application like this:
```
~/work/repos/jkube/quickstarts/maven/external-resources : $ kubectl get pods
NAME                                  READY   STATUS        RESTARTS   AGE
external-resources-7658f8c667-nvkdc   1/1     Running       0          1m
external-resources-7658f8c667-wnkhh   0/1     Terminating   4          19m
external-resources-s2i-1-build        0/1     Completed     0          5m
~/work/repos/jkube/quickstarts/maven/external-resources : $ kubectl get svc
NAME                 TYPE           CLUSTER-IP      EXTERNAL-IP                 PORT(S)          AGE
external-resources   LoadBalancer   172.30.28.146   172.29.5.112,172.29.5.112   8080:30223/TCP   2m
~/work/repos/jkube/quickstarts/maven/external-resources : $ curl `minishift ip`:30223/hello
Hello from Kubernetes ConfigMap!!!
```
Once everything is done, clean up using undeploy goal:
```
mvn k8s:undeploy -Pkubernetes
```

# Deploying on OpenShift(S2I Build)
Just need to run this command:
```
mvn oc:build oc:resource oc:apply -Popenshift
```
Once command executes, you can try accessing your application like this:
```
~/work/repos/jkube/quickstarts/maven/external-resources : $ oc get pods
NAME                                  READY     STATUS        RESTARTS   AGE
external-resources-1-zkqht            1/1       Running       0          25s
external-resources-7658f8c667-wnkhh   0/1       Terminating   4          14m
external-resources-s2i-1-build        0/1       Completed     0          37s
~/work/repos/jkube/quickstarts/maven/external-resources : $ oc get svc
NAME                 TYPE           CLUSTER-IP      EXTERNAL-IP                   PORT(S)          AGE
external-resources   LoadBalancer   172.30.109.23   172.29.227.45,172.29.227.45   8080:30184/TCP   31s
~/work/repos/jkube/quickstarts/maven/external-resources : $ curl `minishift ip`:30184/hello
Hello from Kubernetes ConfigMap!!!
```

To clean up deployed resources, issue this goal:
```
mvn oc:undeploy -Popenshift
```
