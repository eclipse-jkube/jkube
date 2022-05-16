---
name: "Maven :: Spring Boot JIB"
description: |
  Spring Boot application with a single REST endpoint.
  Demonstrates how to build a project using Eclipse JKube JIB build strategy.
---
# Spring Boot Sample with JIB Build Mode

This is also a Spring Boot application to demonstrate how Eclipse JKube handles Kubernetes/OpenShift workflows by 
integrating with [JIB](https://github.com/GoogleContainerTools/jib) which makes Eclipse JKube independent of docker
daemon.

### How to Build?
You can compile project as usual by issuing a simple `mvn clean install` command.

* [Zero Configuration](#zero-configuration)
  * [Build](#zero-configuration-build)
  * [Push](#zero-configuration-push)
  * [Resource-Apply](#zero-configuration-resource-apply)
* [JIB with Customized Assembly](#jib-with-customized-assembly)
  * [Build](#jib-with-customized-assembly-build)
  * [Push](#jib-with-customized-assembly-push)
  * [Resource-Apply](#jib-with-customized-assembly-resource-apply)
* [Registry Configuration](#registry-configuration)
* [Running on Minikube](#how-to-deploy-on-kubernetes)  

## Zero Configuration

### Zero Configuration Build
To build project issue this command:
> mvn package k8s:build -PJib-Zero-Config
```
~/work/repos/jkube/quickstarts/maven/spring-boot-with-jib : $ mvn package k8s:build -PJib-Zero-Config
[INFO] Scanning for projects...
[INFO] 
[INFO] --< org.eclipse.jkube.quickstarts.maven:eclipse-jkube-sample-spring-boot-jib >--
[INFO] Building Eclipse JKube Maven :: Sample :: Spring Boot Web with JIB 1.0.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.0.0-SNAPSHOT:build (default-cli) @ eclipse-jkube-sample-spring-boot-jib ---
[INFO] k8s: Running in Kubernetes mode
[INFO] k8s: Building Docker image in Kubernetes mode
[INFO] k8s: Running generator spring-boot
[INFO] k8s: spring-boot: Using Docker image quay.io/jkube/jkube-java:0.0.13 as base / builder
[INFO] k8s: JIB image build started
JIB> Base image 'quay.io/jkube/jkube-java:0.0.13' does not use a specific image digest - build may not be reproducible
JIB> Containerizing application with the following files:                                                                    
JIB> 	:                                                                                                                      
JIB> 		/home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot-with-jib/target/docker/maven/eclipse-jkube-sample-spring-boot-jib/latest/build/Dockerfilek8s: 
JIB> 	:                                                                                                                      
JIB> 		/home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot-with-jib/target/docker/maven/eclipse-jkube-sample-spring-boot-jib/latest/build/deployments8s: 
JIB> 		/home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot-with-jib/target/docker/maven/eclipse-jkube-sample-spring-boot-jib/latest/build/deployments/eclipse-jkube-sample-spring-boot-jib-1.0.0-SNAPSHOT.jar
JIB> Getting manifest for base image quay.io/jkube/jkube-java:0.0.13...                                            
JIB> Building  layer...                                                                                                      
JIB> Building  layer...                                                                                                      
JIB> Using base image with digest: sha256:0bdf76ac67d0dc03e8d474edce515ea839810d561b9f1c799ebda1d6e6b7789e                   
JIB> Container program arguments set to [/usr/local/s2i/run] (inherited from base image)                                     
JIB> Building image to tar file...                                                                                           
JIB> [==============================] 100.0% complete                                                                        
[INFO] k8s:  /home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot-with-jib/target/docker/maven/eclipse-jkube-sample-spring-boot-jib/latest/tmp/docker-build.tar successfully built
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  8.483 s
[INFO] Finished at: 2020-06-23T19:00:33+05:30
[INFO] ------------------------------------------------------------------------
~/work/repos/jkube/quickstarts/maven/spring-boot-with-jib : $ 
```
JIB build creates a tarball as image output. You can then load this image into minikube's docker daemon like this:
```
~/work/repos/jkube/quickstarts/maven/spring-boot-with-jib : $ docker load -i /home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot-with-jib/target/docker/maven/eclipse-jkube-sample-spring-boot-jib/latest/tmp/docker-build.tar
aad3d0949943: Loading layer [==================================================>]     470B/470B
73025b9c9f1c: Loading layer [==================================================>]  14.81MB/14.81MB
Loaded image: maven/eclipse-jkube-sample-spring-boot-jib:latest
~/work/repos/jkube/quickstarts/maven/spring-boot-with-jib : $ docker images
REPOSITORY                                                       TAG                 IMAGE ID            CREATED             SIZE
maven/eclipse-jkube-sample-spring-boot-jib                       latest              2379085d2763        2 minutes ago       460MB
```
### Zero Configuration Push
In order to push image to a registry, you would need to have your image name configured with respect to your registry. Previously default image name was `maven/eclipse-jkube.sample-spring-boot-jib`. Let's add a property to override default image name:
```
<jkube.generator.name>docker.io/rohankanojia/spring-boot-with-jib:${project.version}</jkube.generator.name>
```
Once we add this property we need to do `mvn k8s:build -PJib-Zero-Config` again in order to modify image tarball. Then we can do a `mvn k8s:push -PJib-Zero-Config` to push image:
```
~/work/repos/jkube/quickstarts/maven/spring-boot-with-jib : $ mvn k8s:build -PJib-Zero-Config
[INFO] Scanning for projects...
[INFO] 
[INFO] --< org.eclipse.jkube.quickstarts.maven:eclipse-jkube-sample-spring-boot-jib >--
[INFO] Building Eclipse JKube :: Quickstarts :: Maven :: Spring Boot JIB 1.0.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.0.0-SNAPSHOT:build (default-cli) @ eclipse-jkube-sample-spring-boot-jib ---
[WARNING] k8s: Cannot access cluster for detecting mode: Unknown host kubernetes.default.svc: Name or service not known
[INFO] k8s: Running in Kubernetes mode
[INFO] k8s: Building Docker image in Kubernetes mode
[INFO] k8s: Running generator spring-boot
[INFO] k8s: spring-boot: Using Docker image quay.io/jkube/jkube-java:0.0.13 as base / builder
[INFO] k8s: JIB image build started
JIB> Base image 'quay.io/jkube/jkube-java:0.0.13' does not use a specific image digest - build may not be reproducible
JIB> Containerizing application with the following files:                                                                    
JIB> 	:                                                                                                                      
JIB> 		/home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot-with-jib/target/docker/docker.io/rohankanojia/spring-boot-with-jib/1.0.0-SNAPSHOT/build/Dockerfile
JIB> 	:                                                                                                                      
JIB> 		/home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot-with-jib/target/docker/docker.io/rohankanojia/spring-boot-with-jib/1.0.0-SNAPSHOT/build/deployments
JIB> 		/home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot-with-jib/target/docker/docker.io/rohankanojia/spring-boot-with-jib/1.0.0-SNAPSHOT/build/deployments/eclipse-jkube-sample-spring-boot-jib-1.0.0-SNAPSHOT.jar
JIB> Getting manifest for base image quay.io/jkube/jkube-java:0.0.13...                                            
JIB> Building  layer...                                                                                                      
JIB> Building  layer...                                                                                                      
JIB> Using base image with digest: sha256:c834f2b076488a81bd4f59397712940b1187681e190f008ffe2c91ae4787290f                   
JIB> Container program arguments set to [/usr/local/s2i/run] (inherited from base image)                                     
JIB> Building image to tar file...                                                                                           
JIB> [========================      ] 80.0% complete > writing to tar file
JIB> [==============================] 100.0% complete
[INFO] k8s:  /home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot-with-jib/target/docker/docker.io/rohankanojia/spring-boot-with-jib/1.0.0-SNAPSHOT/tmp/docker-build.tar successfully built
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  6.384 s
[INFO] Finished at: 2020-07-08T20:52:35+05:30
[INFO] ------------------------------------------------------------------------
~/work/repos/jkube/quickstarts/maven/spring-boot-with-jib : $ mvn k8s:push -PJib-Zero-Config
[INFO] Scanning for projects...
[INFO] 
[INFO] --< org.eclipse.jkube.quickstarts.maven:eclipse-jkube-sample-spring-boot-jib >--
[INFO] Building Eclipse JKube :: Quickstarts :: Maven :: Spring Boot JIB 1.0.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.0.0-SNAPSHOT:push (default-cli) @ eclipse-jkube-sample-spring-boot-jib ---
[WARNING] k8s: Cannot access cluster for detecting mode: Unknown host kubernetes.default.svc: Name or service not known
[INFO] k8s: Running in Kubernetes mode
[INFO] k8s: Building Docker image in Kubernetes mode
[INFO] k8s: Running generator spring-boot
[INFO] k8s: spring-boot: Using Docker image quay.io/jkube/jkube-java:0.0.13 as base / builder
[INFO] k8s: This push refers to: docker.io/rohankanojia/spring-boot-with-jib:1.0.0-SNAPSHOT
JIB> Containerizing application with the following files:                                                                    
JIB> Retrieving registry credentials for registry-1.docker.io...                                                             
JIB> Container program arguments set to [/usr/local/s2i/run] (inherited from base image)                                     
JIB> Skipping push; BLOB already exists on target registry : digest: sha256:cf0f3ebe9f536c782ab3835049cfbd9a663761ded9370791ef6ea3965c823aad, size: 1472
JIB> Skipping push; BLOB already exists on target registry : digest: sha256:57de4da701b511cba33bbdc424757f7f3b408bea741ca714ace265da9b59191a, size: 34109126
JIB> Pushing manifest for latest...                                                                                          

JIB> [==============================] 100.0% complete
JIB> Containerizing application with the following files:                                                                    
JIB> Retrieving registry credentials for registry-1.docker.io...                                                             
JIB> Container program arguments set to [/usr/local/s2i/run] (inherited from base image)                                     
JIB> Skipping push; BLOB already exists on target registry : digest: sha256:cf0f3ebe9f536c782ab3835049cfbd9a663761ded9370791ef6ea3965c823aad, size: 1472
JIB> Skipping push; BLOB already exists on target registry : digest: sha256:57de4da701b511cba33bbdc424757f7f3b408bea741ca714ace265da9b59191a, size: 34109126
JIB> Skipping push; BLOB already exists on target registry : digest: sha256:1e299a277a630ff9e854d4abec9bc47c613cd9033a4d598cb5d0c5ebcdeaef5a, size: 4423
JIB> Skipping push; BLOB already exists on target registry : digest: sha256:8926043aee9f78bc460861e512b93ad0049cf7dc936a6572c1033f8558d84853, size: 473
JIB> Skipping push; BLOB already exists on target registry : digest: sha256:f4b3ba8fc8e4f78503c2099c32630e4df3ca950ddf9ac515316fe8ab0e70baee, size: 14807952
JIB> Skipping push; BLOB already exists on target registry : digest: sha256:f320f94d91a064281f5127d5f49954b481062c7d56cce3b09910e471cf849050, size: 120717854
JIB> Pushing manifest for 1.0.0-SNAPSHOT...                                                                                  

JIB> [==============================] 100.0% complete
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  40.131 s
[INFO] Finished at: 2020-07-08T20:53:20+05:30
[INFO] ------------------------------------------------------------------------
```

### Zero Configuration Resource-Apply
Once image has been loaded in your docker daemon. You can generate and apply manifests using Eclipse JKube:
>  mvn k8s:resource k8s:apply -PJib-Zero-Config
```
~/work/repos/jkube/quickstarts/maven/spring-boot-with-jib : $ mvn k8s:resource k8s:apply -PJib-Zero-Config
[INFO] Scanning for projects...
[INFO] 
[INFO] --< org.eclipse.jkube.quickstarts.maven:eclipse-jkube-sample-spring-boot-jib >--
[INFO] Building Eclipse JKube Maven :: Sample :: Spring Boot Web with JIB 1.0.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.0.0-SNAPSHOT:resource (default-cli) @ eclipse-jkube-sample-spring-boot-jib ---
[INFO] k8s: Running generator spring-boot
[INFO] k8s: spring-boot: Using Docker image quay.io/jkube/jkube-java:0.0.13 as base / builder
[INFO] k8s: jkube-controller: Adding a default Deployment
[INFO] k8s: jkube-service: Adding a default service 'eclipse-jkube-sample-spring-boot-jib' with ports [8080]
[INFO] k8s: jkube-healthcheck-spring-boot: Adding readiness probe on port 8080, path='/health', scheme='HTTP', with initial delay 10 seconds
[INFO] k8s: jkube-healthcheck-spring-boot: Adding liveness probe on port 8080, path='/health', scheme='HTTP', with initial delay 180 seconds
[INFO] k8s: jkube-revision-history: Adding revision history limit to 2
[INFO] k8s: validating /home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot-with-jib/target/classes/META-INF/jkube/kubernetes/eclipse-jkube-sample-spring-boot-jib-deployment.yml resource
[INFO] k8s: validating /home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot-with-jib/target/classes/META-INF/jkube/kubernetes/eclipse-jkube-sample-spring-boot-jib-service.yml resource
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.0.0-SNAPSHOT:apply (default-cli) @ eclipse-jkube-sample-spring-boot-jib ---
[INFO] k8s: Using Kubernetes at https://192.168.39.153:8443/ in namespace default with manifest /home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot-with-jib/target/classes/META-INF/jkube/kubernetes.yml 
[INFO] k8s: Using namespace: default
[INFO] k8s: Creating a Service from kubernetes.yml namespace default name eclipse-jkube-sample-spring-boot-jib
[INFO] k8s: Created Service: target/jkube/applyJson/default/service-eclipse-jkube-sample-spring-boot-jib-1.json
[INFO] k8s: Creating a Deployment from kubernetes.yml namespace default name eclipse-jkube-sample-spring-boot-jib
[INFO] k8s: Created Deployment: target/jkube/applyJson/default/deployment-eclipse-jkube-sample-spring-boot-jib-1.json
[INFO] k8s: HINT: Use the command `kubectl get pods -w` to watch your pods start up
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  8.853 s
[INFO] Finished at: 2020-06-23T19:04:12+05:30
[INFO] ------------------------------------------------------------------------
~/work/repos/jkube/quickstarts/maven/spring-boot-with-jib : $ kubectl get pods
NAME                                                    READY   STATUS    RESTARTS   AGE
eclipse-jkube-sample-spring-boot-jib-6d7f95cc7b-5j7z2   0/1     Running   0          9s
~/work/repos/jkube/quickstarts/maven/spring-boot-with-jib : $ kubectl get pods -w
NAME                                                    READY   STATUS    RESTARTS   AGE
eclipse-jkube-sample-spring-boot-jib-6d7f95cc7b-5j7z2   0/1     Running   0          11s
eclipse-jkube-sample-spring-boot-jib-6d7f95cc7b-5j7z2   1/1     Running   0          16s
```
Eclipse JKube generates a default `Deployment` and `Service`, by default it creates a `Service` of type
`ClusterIP`. So you would need to do a `minikube ssh` to access your application:
```
~/work/repos/jkube/quickstarts/maven/spring-boot-with-jib : $ kubectl get svc
NAME                                   TYPE        CLUSTER-IP     EXTERNAL-IP   PORT(S)    AGE
eclipse-jkube-sample-spring-boot-jib   ClusterIP   10.96.74.212   <none>        8080/TCP   110s
kubernetes                             ClusterIP   10.96.0.1      <none>        443/TCP    45m
~/work/repos/jkube/quickstarts/maven/spring-boot-with-jib : $ minikube ssh
                         _             _            
            _         _ ( )           ( )           
  ___ ___  (_)  ___  (_)| |/')  _   _ | |_      __  
/' _ ` _ `\| |/' _ `\| || , <  ( ) ( )| '_`\  /'__`\
| ( ) ( ) || || ( ) || || |\`\ | (_) || |_) )(  ___/
(_) (_) (_)(_)(_) (_)(_)(_) (_)`\___/'(_,__/'`\____)

$ curl 10.96.74.212:8080/
Greetings from Spring Boot(Powered by JIB)!!$
```

Once everything is set up, you can do cleanup using undeploy goal:
>  mvn k8s:undeploy -PJib-Zero-Config
```
~/work/repos/jkube/quickstarts/maven/spring-boot-with-jib : $ mvn k8s:undeploy -PJib-Zero-Config
[INFO] Scanning for projects...
[INFO] 
[INFO] --< org.eclipse.jkube.quickstarts.maven:eclipse-jkube-sample-spring-boot-jib >--
[INFO] Building Eclipse JKube Maven :: Sample :: Spring Boot Web with JIB 1.0.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.0.0-SNAPSHOT:undeploy (default-cli) @ eclipse-jkube-sample-spring-boot-jib ---
[INFO] k8s: Using Kubernetes at https://192.168.39.153:8443/ in namespace default with manifest /home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot-with-jib/target/classes/META-INF/jkube/kubernetes.yml 
[INFO] k8s: Using namespace: default
[INFO] k8s: Deleting resource Deployment default/eclipse-jkube-sample-spring-boot-jib
[INFO] k8s: Deleting resource Service default/eclipse-jkube-sample-spring-boot-jib
[INFO] k8s: HINT: Use the command `kubectl get pods -w` to watch your pods start up
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  4.132 s
[INFO] Finished at: 2020-06-23T19:07:24+05:30
[INFO] ------------------------------------------------------------------------
~/work/repos/jkube/quickstarts/maven/spring-boot-with-jib : $
```

## JIB with Customized Assembly
This profile tries to add some extra files inside the image. If you see there is an extra directory `tempDir` in project base directory. It is the copied to target image.

### JIB with Customized Assembly Build
Now to build you need to issue same build goal but with different profile, build goal generates a tarball which needs to be loaded into your docker daemon afterwards. Or maybe you can push it to some registry:
>  mvn package k8s:build -PJib-With-Assembly
```
~/work/repos/jkube/quickstarts/maven/spring-boot-with-jib : $ mvn package k8s:build -PJib-With-Assembly
[INFO] Scanning for projects...
[INFO] 
[INFO] --< org.eclipse.jkube.quickstarts.maven:eclipse-jkube-sample-spring-boot-jib >--
[INFO] Building Eclipse JKube Maven :: Sample :: Spring Boot Web with JIB 1.0.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.0.0-SNAPSHOT:build (default-cli) @ eclipse-jkube-sample-spring-boot-jib ---
[INFO] k8s: Running in Kubernetes mode
[INFO] k8s: Building Docker image in Kubernetes mode
[INFO] k8s: JIB image build started
JIB> Base image 'fabric8/java-centos-openjdk8-jdk:1.5.6' does not use a specific image digest - build may not be reproducible
JIB> Containerizing application with the following files:                                                                    
JIB> 	:                                                                                                                      
JIB> 		/home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot-with-jib/target/docker/rohankanojia/spring-boot-sample/build/Dockerfile
JIB> 	:                                                                                                                      
JIB> 		/home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot-with-jib/target/docker/rohankanojia/spring-boot-sample/build/deployments
JIB> 		/home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot-with-jib/target/docker/rohankanojia/spring-boot-sample/build/deployments/tempDir
JIB> 		/home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot-with-jib/target/docker/rohankanojia/spring-boot-sample/build/deployments/tempDir/testFile.txt 
JIB> 		/home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot-with-jib/target/docker/rohankanojia/spring-boot-sample/build/deployments/eclipse-jkube-sample-spring-boot-jib-1.0.0-SNAPSHOT.jar
JIB> Getting manifest for base image fabric8/java-centos-openjdk8-jdk:1.5.6...                                               
JIB> Building  layer...                                                                                                      
JIB> Building  layer...                                                                                                      
JIB> The base image requires auth. Trying again for fabric8/java-centos-openjdk8-jdk:1.5.6...                                
JIB> Retrieving registry credentials for registry-1.docker.io...                                                             
JIB> No credentials could be retrieved for registry registry-1.docker.io                                                     
JIB> Using base image with digest: sha256:92530aa1eb4c49e3b1d033f94e9cd4dc891d49922459e13f84e59c9d68d800eb                   
JIB> Container program arguments set to [java, -jar, /deployments/eclipse-jkube-sample-spring-boot-jib-1.0.0-SNAPSHOT.jar]   
JIB> Building image to tar file...                                                                                           
JIB> [==============================] 100.0% complete                                                                        
[INFO] k8s:  /home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot-with-jib/target/docker/rohankanojia/spring-boot-sample/tmp/docker-build.tar successfully built
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  17.594 s
[INFO] Finished at: 2020-06-23T19:15:10+05:30
[INFO] ------------------------------------------------------------------------
~/work/repos/jkube/quickstarts/maven/spring-boot-with-jib : $ docker  load -i /home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot-with-jib/target/docker/rohankanojia/spring-boot-sample/tmp/docker-build.tar
e4fdafa32b74: Loading layer [==================================================>]     229B/229B
b9880563a19a: Loading layer [==================================================>]  14.81MB/14.81MB
Loaded image: rohankanojia/spring-boot-sample:latest
~/work/repos/jkube/quickstarts/maven/spring-boot-with-jib : $ 
```

### Jib With Customized Assembly Push
Pushing image to docker hub in this case, you can provide registry credentials in plugin XML config, in `~/.m2/settings.xml` or in `~/.docker/config.json`:
> mvn k8s:push -PJib-With-Assembly 
```
~/work/repos/jkube/quickstarts/maven/spring-boot-with-jib : $ mvn k8s:push -PJib-With-Assembly
[INFO] Scanning for projects...
[INFO] 
[INFO] --< org.eclipse.jkube.quickstarts.maven:eclipse-jkube-sample-spring-boot-jib >--
[INFO] Building Eclipse JKube Maven :: Sample :: Spring Boot Web with JIB 1.0.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.0.0-SNAPSHOT:push (default-cli) @ eclipse-jkube-sample-spring-boot-jib ---
[INFO] k8s: Running in Kubernetes mode
JIB> Containerizing application with the following files:                                                                    
JIB> Retrieving registry credentials for registry-1.docker.io...                                                             
JIB> Container program arguments set to [java, -jar, /deployments/eclipse-jkube-sample-spring-boot-jib-1.0.0-SNAPSHOT.jar] (inherited from base image)
JIB> Pushing manifest for latest...                                                                                          
JIB> [==============================] 100.0% complete                                                                        
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:37 min
[INFO] Finished at: 2020-06-23T19:23:06+05:30
[INFO] ------------------------------------------------------------------------
```

### JIB With Customized Assembly Resource-Apply
Once image is loaded you can issue resource and apply goals:
>  mvn k8s:resource k8s:apply -PJib-With-Assembly
```
~/work/repos/jkube/quickstarts/maven/spring-boot-with-jib : $ mvn k8s:resource k8s:apply -PJib-With-Assembly
[INFO] Scanning for projects...
[INFO] 
[INFO] --< org.eclipse.jkube.quickstarts.maven:eclipse-jkube-sample-spring-boot-jib >--
[INFO] Building Eclipse JKube Maven :: Sample :: Spring Boot Web with JIB 1.0.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.0.0-SNAPSHOT:resource (default-cli) @ eclipse-jkube-sample-spring-boot-jib ---
[INFO] k8s: jkube-controller: Adding a default Deployment
[INFO] k8s: jkube-service: Adding a default service 'eclipse-jkube-sample-spring-boot-jib' with ports [8080]
[INFO] k8s: jkube-healthcheck-spring-boot: Adding readiness probe on port 8080, path='/health', scheme='HTTP', with initial delay 10 seconds
[INFO] k8s: jkube-healthcheck-spring-boot: Adding liveness probe on port 8080, path='/health', scheme='HTTP', with initial delay 180 seconds
[INFO] k8s: jkube-revision-history: Adding revision history limit to 2
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.0.0-SNAPSHOT:apply (default-cli) @ eclipse-jkube-sample-spring-boot-jib ---
[INFO] k8s: Using Kubernetes at https://192.168.39.153:8443/ in namespace default with manifest /home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot-with-jib/target/classes/META-INF/jkube/kubernetes.yml 
[INFO] k8s: Using namespace: default
[INFO] k8s: Creating a Service from kubernetes.yml namespace default name eclipse-jkube-sample-spring-boot-jib
[INFO] k8s: Created Service: target/jkube/applyJson/default/service-eclipse-jkube-sample-spring-boot-jib.json
[INFO] k8s: Creating a Deployment from kubernetes.yml namespace default name eclipse-jkube-sample-spring-boot-jib
[INFO] k8s: Created Deployment: target/jkube/applyJson/default/deployment-eclipse-jkube-sample-spring-boot-jib.json
[INFO] k8s: HINT: Use the command `kubectl get pods -w` to watch your pods start up
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  8.140 s
[INFO] Finished at: 2020-06-23T19:18:42+05:30
[INFO] ------------------------------------------------------------------------
~/work/repos/jkube/quickstarts/maven/spring-boot-with-jib : $ kubectl get pods
NAME                                                    READY   STATUS    RESTARTS   AGE
eclipse-jkube-sample-spring-boot-jib-78f854d858-rzqfw   0/1     Running   0          9s
~/work/repos/jkube/quickstarts/maven/spring-boot-with-jib : $ kubectl get pods -w
NAME                                                    READY   STATUS    RESTARTS   AGE
eclipse-jkube-sample-spring-boot-jib-78f854d858-rzqfw   0/1     Running   0          11s
eclipse-jkube-sample-spring-boot-jib-78f854d858-rzqfw   1/1     Running   0          13s
^C~/work/repos/jkube/quickstarts/maven/spring-boot-with-jib : $ kubectl get svc
NAME                                   TYPE        CLUSTER-IP     EXTERNAL-IP   PORT(S)    AGE
eclipse-jkube-sample-spring-boot-jib   ClusterIP   10.96.54.251   <none>        8080/TCP   17s
kubernetes                             ClusterIP   10.96.0.1      <none>        443/TCP    58m
~/work/repos/jkube/quickstarts/maven/spring-boot-with-jib : $ minikube ssh
                         _             _            
            _         _ ( )           ( )           
  ___ ___  (_)  ___  (_)| |/')  _   _ | |_      __  
/' _ ` _ `\| |/' _ `\| || , <  ( ) ( )| '_`\  /'__`\
| ( ) ( ) || || ( ) || || |\`\ | (_) || |_) )(  ___/
(_) (_) (_)(_)(_) (_)(_)(_) (_)`\___/'(_,__/'`\____)

$ curl 10.96.54.251:8080/
Greetings from Spring Boot(Powered by JIB)!!
```

### How to deploy on Kubernetes
To deploy project, make sure you are connected to Kubernetes Cluster. In this video we would be using minikube. Make sure
you have minikube's docker daemon exposed via `eval $(minikube docker-env)` command. There are two profiles in this project 
right now. In order to use built image inside minikube docker daemon. You need to use `docker load -i /path/to/image.tar` 
command to load image into Minikube's docker daemon. Here is an example 
```
~/work/repos/jkube/quickstarts/maven/spring-boot-with-jib : $ docker load -i /home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot-with-jib/target/docker/maven/eclipse-jkube-sample-spring-boot-jib/latest/tmp/docker-build.tar
aad3d0949943: Loading layer [==================================================>]     470B/470B
73025b9c9f1c: Loading layer [==================================================>]  14.81MB/14.81MB
Loaded image: maven/eclipse-jkube-sample-spring-boot-jib:latest
~/work/repos/jkube/quickstarts/maven/spring-boot-with-jib : $ docker images
REPOSITORY                                                       TAG                 IMAGE ID            CREATED             SIZE
maven/eclipse-jkube-sample-spring-boot-jib                       latest              2379085d2763        2 minutes ago       460MB
```

### Registry Configuration
You can provide registry credentials in 3 formats:
- You can do a `docker login`(for Docker Hub) or `docker login <your-registry` and plugin would read your `~/.docker/config.json` file
- You can provide registry credentials in your `~/.m2/settings.xml` file like this and plugin would read it from there:
```
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">

  <servers>
    <server>
      <id>https://index.docker.io/v1</id>
      <username>testuser</username>
      <password>testpassword</password>
    </server>
    <server>
      <id>quay.io</id>
      <username>testuser</username>
      <password>testpassword</password>
    </server>
  </servers>

</settings>

```

-  You can provide registry credentials as part of XML configuration:
```
<plugin>
    <groupId>org.eclipse.jkube</groupId>
    <artifactId>kubernetes-maven-plugin</artifactId>
    <version>${project.version}</version>
    <configuration>
        <images>
            <!-- Your Image Configuration -->  
        </images>
        <authConfig>
          <username>testuser</username>
          <password>testpassword</password>
        </authConfig>
    </configuration>
</plugin>

```
