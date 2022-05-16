---
name: "Maven :: Webapp :: Wildfly :: Datasource"
description: |
  Java Web Application that uses an embedded h2 database.
  Demonstrates how to create a container image with an embedded WildFly server using Eclipse JKube.
  WildFly is used instead of Apache Tomcat because there is a WildFly persistence.xml and -ds.xml configuration.
  Eclipse JKube detects this file and chooses a WildFly specific base container image.
---
# Eclipse JKube Webapp Wildfly Sample
This quick start showcases how to use Eclipse JKube with a Wildfly war project.
Demonstrates how to create a container image with an embedded WildFly server using Eclipse JKube.
WildFly is used instead of Apache Tomcat because there is a WildFly persistence.xml and -ds.xml configuration.
Eclipse JKube detects this file and chooses a WildFly specific base container image.

## Prerequisites
You will need the following to run it with Minikube:
- minikube installed and running on your computer
- minikube ingress addon enabled

      $ minikube addons enable ingress

- Use the docker daemon installed in minikube

      $ eval $(minikube -p minikube docker-env)

## Build the application and docker image
```
$ mvn clean package k8s:build
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------< org.eclipse.jkube:webapp-wildfly-datasource >-------------
[INFO] Building Eclipse JKube :: Quickstarts :: Maven :: Webapp :: Wildfly :: Datasource 1.6.0
[INFO] --------------------------------[ war ]---------------------------------
[INFO] 
[INFO] --- maven-clean-plugin:2.5:clean (default-clean) @ webapp-wildfly-datasource ---
[INFO] Deleting /home/sunix/github/eclipse/jkube/quickstarts/maven/webapp-wildfly-2/target
[INFO] 
[INFO] --- maven-resources-plugin:2.6:resources (default-resources) @ webapp-wildfly-datasource ---
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] Copying 2 resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.1:compile (default-compile) @ webapp-wildfly-datasource ---
[INFO] Changes detected - recompiling the module!
[INFO] Compiling 5 source files to /home/sunix/github/eclipse/jkube/quickstarts/maven/webapp-wildfly-2/target/classes
[INFO] 
[INFO] --- maven-resources-plugin:2.6:testResources (default-testResources) @ webapp-wildfly-datasource ---
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] skip non existing resourceDirectory /home/sunix/github/eclipse/jkube/quickstarts/maven/webapp-wildfly-2/src/test/resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.1:testCompile (default-testCompile) @ webapp-wildfly-datasource ---
[INFO] No sources to compile
[INFO] 
[INFO] --- maven-surefire-plugin:2.12.4:test (default-test) @ webapp-wildfly-datasource ---
[INFO] No tests to run.
[INFO] 
[INFO] --- maven-war-plugin:2.6:war (default-war) @ webapp-wildfly-datasource ---
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by com.thoughtworks.xstream.converters.collections.TreeMapConverter (file:/home/sunix/.m2/repository/com/thoughtworks/xstream/xstream/1.4.4/xstream-1.4.4.jar) to field java.util.TreeMap.comparator
WARNING: Please consider reporting this to the maintainers of com.thoughtworks.xstream.converters.collections.TreeMapConverter
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
[INFO] Packaging webapp
[INFO] Assembling webapp [webapp-wildfly-datasource] in [/home/sunix/github/eclipse/jkube/quickstarts/maven/webapp-wildfly-2/target/webapp-wildfly-datasource]
[INFO] Processing war project
[INFO] Copying webapp resources [/home/sunix/github/eclipse/jkube/quickstarts/maven/webapp-wildfly-2/src/main/webapp]
[INFO] Webapp assembled in [50 msecs]
[INFO] Building war: /home/sunix/github/eclipse/jkube/quickstarts/maven/webapp-wildfly-2/target/webapp-wildfly-datasource.war
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.6.0:build (default-cli) @ webapp-wildfly-datasource ---
[INFO] k8s: Running in Kubernetes mode
[INFO] k8s: Building Docker image in Kubernetes mode
[INFO] k8s: Running generator webapp
[INFO] k8s: webapp: Using jboss/wildfly:25.0.0.Final as base image for webapp
[INFO] k8s: Pulling from jboss/wildfly
f87ff222252e: Pull complete 
13776e8da872: Pull complete 
0b43aea4eeb1: Pull complete 
8116b2f7ca5a: Pull complete 
f26d32e28c29: Pull complete 
[INFO] k8s: Digest: sha256:35320abafdec6d360559b411aff466514d5741c3c527221445f48246350fdfe5
[INFO] k8s: Status: Downloaded newer image for jboss/wildfly:25.0.0.Final
[INFO] k8s: Pulled jboss/wildfly:25.0.0.Final in 17 seconds 
[INFO] k8s: [jkube/webapp-wildfly-datasource:1.6.0] "webapp": Created docker-build.tar in 48 milliseconds
[INFO] k8s: [jkube/webapp-wildfly-datasource:1.6.0] "webapp": Built image sha256:f5526
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  23.236 s
[INFO] Finished at: 2022-02-10T18:47:13+01:00
[INFO] ------------------------------------------------------------------------

$ docker images | grep webapp-wildfly
jkube/webapp-wildfly-datasource                         1.6.0          f5526f9948dd   57 seconds ago   737MB
```

## Generate Kubernetes Manifests
```
$ mvn k8s:resource -Djkube.createExternalUrls=true -Djkube.domain=$(minikube ip).nip.io
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------< org.eclipse.jkube:webapp-wildfly-datasource >-------------
[INFO] Building Eclipse JKube :: Quickstarts :: Maven :: Webapp :: Wildfly :: Datasource 1.6.0
[INFO] --------------------------------[ war ]---------------------------------
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.6.0:resource (default-cli) @ webapp-wildfly-datasource ---
[INFO] k8s: Running generator webapp
[INFO] k8s: webapp: Using jboss/wildfly:25.0.0.Final as base image for webapp
[INFO] k8s: Using resource templates from /home/sunix/github/eclipse/jkube/quickstarts/maven/webapp-wildfly-2/src/main/jkube
[INFO] k8s: jkube-controller: Adding a default Deployment
[INFO] k8s: jkube-service: Adding a default service 'webapp-wildfly-datasource' with ports [8080]
[INFO] k8s: jkube-service-discovery: Using first mentioned service port '8080' 
[INFO] k8s: jkube-revision-history: Adding revision history limit to 2
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  2.612 s
[INFO] Finished at: 2022-02-10T18:48:44+01:00
[INFO] ------------------------------------------------------------------------

$ ls target/classes/META-INF/jkube/
kubernetes  kubernetes.yml

$ ls target/classes/META-INF/jkube/kubernetes
webapp-wildfly-datasource-deployment.yml  webapp-wildfly-datasource-ingress.yml  webapp-wildfly-datasource-service.yml
```

## Apply Generated Manifests onto Kubernetes Cluster
```
$ mvn k8s:apply
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------< org.eclipse.jkube:webapp-wildfly-datasource >-------------
[INFO] Building Eclipse JKube :: Quickstarts :: Maven :: Webapp :: Wildfly :: Datasource 1.6.0
[INFO] --------------------------------[ war ]---------------------------------
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.6.0:apply (default-cli) @ webapp-wildfly-datasource ---
[INFO] k8s: Using Kubernetes at https://192.168.99.111:8443/ in namespace null with manifest /home/sunix/github/eclipse/jkube/quickstarts/maven/webapp-wildfly-2/target/classes/META-INF/jkube/kubernetes.yml 
[INFO] k8s: Creating a Service from kubernetes.yml namespace default name webapp-wildfly-datasource
[INFO] k8s: Created Service: target/jkube/applyJson/default/service-webapp-wildfly-datasource.json
[INFO] k8s: Creating a Deployment from kubernetes.yml namespace default name webapp-wildfly-datasource
[INFO] k8s: Created Deployment: target/jkube/applyJson/default/deployment-webapp-wildfly-datasource.json
[INFO] k8s: Applying Ingress webapp-wildfly-datasource from kubernetes.yml
[INFO] k8s: HINT: Use the command `kubectl get pods -w` to watch your pods start up
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  6.651 s
[INFO] Finished at: 2022-02-10T18:50:28+01:00
[INFO] ------------------------------------------------------------------------

$ kubectl get pods
NAME                                         READY   STATUS    RESTARTS   AGE
webapp-wildfly-datasource-6f5bfd7f54-ltgkf   1/1     Running   0          30s

```

## Access application running inside Kubernetes

Make sure the ingress resource has been created
```
$ kubectl get ingress
NAME                        CLASS    HOSTS                                             ADDRESS          PORTS   AGE
webapp-wildfly-datasource   <none>   webapp-wildfly-datasource.192.168.99.111.nip.io   192.168.99.111   80      75s
```


Give it a try:
```
$ lynx --dump http://webapp-wildfly-datasource.192.168.99.111.nip.io
☕🧊 Eclipse JKube commiters

     Username   First name Last name
   manusa       Marc       Nuri
   rohanKanojia Rohan      Kumar
   sunix        Sun        Tan
```

