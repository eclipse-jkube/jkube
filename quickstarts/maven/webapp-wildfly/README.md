# Eclipse JKube Webapp Wildfly Sample

## Build Application's Docker Image
```
~/work/repos/webapp-wildfly : $ mvn k8s:build
[INFO] Scanning for projects...
[INFO] 
[INFO] --------< org.eclipse.jkube:jkube-maven-sample-webapp-wildfly >---------
[INFO] Building jkube-maven-sample-webapp-wildfly 1.0.0-SNAPSHOT
[INFO] --------------------------------[ war ]---------------------------------
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.0.0-SNAPSHOT:build (default-cli) @ jkube-maven-sample-webapp-wildfly ---
[INFO] k8s: Running in Kubernetes mode
[INFO] k8s: Building Docker image in Kubernetes mode
[INFO] k8s: Running generator webapp
[INFO] k8s: webapp: Using jboss/wildfly:10.1.0.Final as base image for webapp
[INFO] k8s: [jkube/jkube-maven-sample-webapp-wildfly:latest] "webapp": Created docker-build.tar in 43 milliseconds
[INFO] k8s: [jkube/jkube-maven-sample-webapp-wildfly:latest] "webapp": Built image sha256:fa2af
[INFO] k8s: [jkube/jkube-maven-sample-webapp-wildfly:latest] "webapp": Removed old image sha256:11e06
[INFO] k8s: [jkube/jkube-maven-sample-webapp-wildfly:latest] "webapp": Tag with latest
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  4.814 s
[INFO] Finished at: 2020-04-28T15:10:27+05:30
[INFO] ------------------------------------------------------------------------
~/work/repos/webapp-wildfly : $ docker images | grep webapp-wild
jkube/jkube-maven-sample-webapp-wildfly   latest                        fa2af15e1a24        8 seconds ago       676MB
```

## Generate Kubernetes Manifests
```
~/work/repos/webapp-wildfly : $ mvn k8s:resource
[INFO] Scanning for projects...
[INFO] 
[INFO] --------< org.eclipse.jkube:jkube-maven-sample-webapp-wildfly >---------
[INFO] Building jkube-maven-sample-webapp-wildfly 1.0.0-SNAPSHOT
[INFO] --------------------------------[ war ]---------------------------------
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.0.0-SNAPSHOT:resource (default-cli) @ jkube-maven-sample-webapp-wildfly ---
[INFO] k8s: Running generator webapp
[INFO] k8s: webapp: Using jboss/wildfly:10.1.0.Final as base image for webapp
[INFO] k8s: jkube-controller: Adding a default Deployment
[INFO] k8s: jkube-service: Adding a default service 'jkube-maven-sample-webapp-wildfly' with ports [8080]
[INFO] k8s: jkube-revision-history: Adding revision history limit to 2
[INFO] k8s: validating /home/rohaan/work/repos/webapp-wildfly/target/classes/META-INF/jkube/kubernetes/jkube-maven-sample-webapp-wildfly-service.yml resource
[INFO] k8s: validating /home/rohaan/work/repos/webapp-wildfly/target/classes/META-INF/jkube/kubernetes/jkube-maven-sample-webapp-wildfly-deployment.yml resour
ce
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  2.495 s
[INFO] Finished at: 2020-04-28T15:11:56+05:30
[INFO] ------------------------------------------------------------------------
~/work/repos/webapp-wildfly : $ ls target/classes/META-INF/jkube/
kubernetes  kubernetes.yml
~/work/repos/webapp-wildfly : $ ls target/classes/META-INF/jkube/kubernetes
jkube-maven-sample-webapp-wildfly-deployment.yml  jkube-maven-sample-webapp-wildfly-service.yml
~/work/repos/webapp-wildfly : $ 
```

## Apply Generated Manifests onto Kubernetes Cluster
```
~/work/repos/webapp-wildfly : $ mvn k8s:apply
[INFO] Scanning for projects...
[INFO] 
[INFO] --------< org.eclipse.jkube:jkube-maven-sample-webapp-wildfly >---------
[INFO] Building jkube-maven-sample-webapp-wildfly 1.0.0-SNAPSHOT
[INFO] --------------------------------[ war ]---------------------------------
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.0.0-SNAPSHOT:apply (default-cli) @ jkube-maven-sample-webapp-wildfly ---
[INFO] k8s: Using Kubernetes at https://172.17.0.2:8443/ in namespace default with manifest /home/rohaan/work/repos/webapp-wildfly/target/classes/META-INF/jku
be/kubernetes.yml 
[INFO] k8s: Using namespace: default
[INFO] k8s: Updating a Service from kubernetes.yml
[INFO] k8s: Updated Service: target/jkube/applyJson/default/service-jkube-maven-sample-webapp-wildfly-1.json
[INFO] k8s: Updating Deployment from kubernetes.yml
[INFO] k8s: Updated Deployment: target/jkube/applyJson/default/deployment-jkube-maven-sample-webapp-wildfly-1.json
[INFO] k8s: HINT: Use the command `kubectl get pods -w` to watch your pods start up
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  7.646 s
[INFO] Finished at: 2020-04-28T15:12:56+05:30
[INFO] ------------------------------------------------------------------------
~/work/repos/webapp-wildfly : $ kubectl get pods
NAME                                                READY   STATUS    RESTARTS   AGE
jkube-maven-sample-webapp-wildfly-f6c5b7866-54v4b   1/1     Running   0          10m
~/work/repos/webapp-wildfly : $ 
```

## Access application running inside Kubernetes
```
~/work/repos/webapp-wildfly : $ kubectl get pods                                                                                                              
NAME                                                READY   STATUS    RESTARTS   AGE                                                                          
jkube-maven-sample-webapp-wildfly-f6c5b7866-54v4b   1/1     Running   0          11m                                                                          
~/work/repos/webapp-wildfly : $ MINIKUBE_IP=`minikube ip`                                                                                                     
~/work/repos/webapp-wildfly : $ kubectl get svc                                                                                                               
NAME                                TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)          AGE                                                            
jkube-maven-sample-webapp-wildfly   NodePort    10.96.101.229   <none>        8080:31820/TCP   11m                                                            
kubernetes                          ClusterIP   10.96.0.1       <none>        443/TCP          18h                                                            
~/work/repos/webapp-wildfly : $ PORT=`kubectl get svc jkube-maven-sample-webapp-wildfly -o=jsonpath='{.spec.ports[0].nodePort}'`                              
~/work/repos/webapp-wildfly : $ curl $MINIKUBE_IP:$PORT/jkube-maven-sample-webapp-wildfly/  

```
Or you can also use `minikube service`:
```
~/work/repos/webapp-wildfly : $ minikube service jkube-maven-sample-webapp-wildfly
|-----------|-----------------------------------|-------------|-------------------------|
| NAMESPACE |               NAME                | TARGET PORT |           URL           |
|-----------|-----------------------------------|-------------|-------------------------|
| default   | jkube-maven-sample-webapp-wildfly | http/8080   | http://172.17.0.2:31820 |
|-----------|-----------------------------------|-------------|-------------------------|
🎉  Opening service default/jkube-maven-sample-webapp-wildfly in default browser...

```

![Wildfly Running](https://i.imgur.com/A6Tw2h2.png)
![Application Running](https://i.imgur.com/c2eITmW.png)



