# JKube Hello World Sample

This is a demo project for getting started with Eclipse JKube. It just prints "Hello World" on command
line and exits. We would be using Eclipse JKube for building a docker image and deploying to Kubernetes
in single command.

1. Make sure you've minikube up and running.
2. Run the following command to run helloworld sample: 
```
~/work/repos/jkube/quickstarts/maven/hello-world : $ mvn clean install k8s:build k8s:resource k8s:apply
[INFO] Scanning for projects...
[INFO] 
[INFO] ---------< org.eclipse.jkube.samples:jkube-sample-helloworld >----------
[INFO] Building jkube-sample-helloworld 0.1.1-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- maven-clean-plugin:2.5:clean (default-clean) @ jkube-sample-helloworld ---
[INFO] 
[INFO] --- maven-resources-plugin:2.6:resources (default-resources) @ jkube-sample-helloworld ---
[WARNING] Using platform encoding (UTF-8 actually) to copy filtered resources, i.e. build is platform dependent!
[INFO] skip non existing resourceDirectory /home/rohaan/work/repos/jkube/quickstarts/maven/hello-world/src/main/resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.6.1:compile (default-compile) @ jkube-sample-helloworld ---
[INFO] Changes detected - recompiling the module!
[WARNING] File encoding has not been set, using platform encoding UTF-8, i.e. build is platform dependent!
[INFO] Compiling 1 source file to /home/rohaan/work/repos/jkube/quickstarts/maven/hello-world/target/classes
[INFO] 
[INFO] --- maven-resources-plugin:2.6:testResources (default-testResources) @ jkube-sample-helloworld ---
[WARNING] Using platform encoding (UTF-8 actually) to copy filtered resources, i.e. build is platform dependent!
[INFO] skip non existing resourceDirectory /home/rohaan/work/repos/jkube/quickstarts/maven/hello-world/src/test/resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.6.1:testCompile (default-testCompile) @ jkube-sample-helloworld ---
[INFO] Changes detected - recompiling the module!
[WARNING] File encoding has not been set, using platform encoding UTF-8, i.e. build is platform dependent!
[INFO] Compiling 1 source file to /home/rohaan/work/repos/jkube/quickstarts/maven/hello-world/target/test-classes
[INFO] 
[INFO] --- maven-surefire-plugin:2.12.4:test (default-test) @ jkube-sample-helloworld ---
[INFO] Surefire report directory: /home/rohaan/work/repos/jkube/quickstarts/maven/hello-world/target/surefire-reports

-------------------------------------------------------
 T E S T S
-------------------------------------------------------
Running org.eclipse.jkube.sample.helloworld.AppTest
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.044 sec

Results :

Tests run: 1, Failures: 0, Errors: 0, Skipped: 0

[INFO] 
[INFO] --- maven-jar-plugin:3.0.2:jar (default-jar) @ jkube-sample-helloworld ---
[INFO] Building jar: /home/rohaan/work/repos/jkube/quickstarts/maven/hello-world/target/jkube-sample-helloworld-0.1.1-SNAPSHOT.jar
[INFO] 
[INFO] --- maven-install-plugin:2.4:install (default-install) @ jkube-sample-helloworld ---
[INFO] Installing /home/rohaan/work/repos/jkube/quickstarts/maven/hello-world/target/jkube-sample-helloworld-0.1.1-SNAPSHOT.jar to /home/rohaan/.m2/repository/org/eclipse/jkube/samples/jkube-sample-helloworld/0.1.1-SNAPSHOT/jkube-sample-helloworld-0.1.1-SNAPSHOT.jar
[INFO] Installing /home/rohaan/work/repos/jkube/quickstarts/maven/hello-world/pom.xml to /home/rohaan/.m2/repository/org/eclipse/jkube/samples/jkube-sample-helloworld/0.1.1-SNAPSHOT/jkube-sample-helloworld-0.1.1-SNAPSHOT.pom
[INFO] 
[INFO] --- kubernetes-maven-plugin:0.1.1-SNAPSHOT:build (default-cli) @ jkube-sample-helloworld ---
[INFO] k8s: Running in Kubernetes mode
[INFO] k8s: Building Docker image in Kubernetes mode
[INFO] k8s: [helloworld-java:0.1.1-SNAPSHOT] "hello-world": Created docker-build.tar in 34 milliseconds
[INFO] k8s: [helloworld-java:0.1.1-SNAPSHOT] "hello-world": Built image sha256:9baee
[INFO] 
[INFO] --- kubernetes-maven-plugin:0.1.1-SNAPSHOT:resource (default-cli) @ jkube-sample-helloworld ---
[INFO] k8s: jkube-controller: Adding a default Deployment
[INFO] k8s: jkube-revision-history: Adding revision history limit to 2
[INFO] k8s: validating /home/rohaan/work/repos/jkube/quickstarts/maven/hello-world/target/classes/META-INF/jkube/kubernetes/jkube-sample-helloworld-deployment.yml resource
[INFO] 
[INFO] --- kubernetes-maven-plugin:0.1.1-SNAPSHOT:apply (default-cli) @ jkube-sample-helloworld ---
[INFO] k8s: Using Kubernetes at https://192.168.39.149:8443/ in namespace default with manifest /home/rohaan/work/repos/jkube/quickstarts/maven/hello-world/target/classes/META-INF/jkube/kubernetes.yml 
[INFO] k8s: Using namespace: default
[INFO] k8s: Creating a Deployment from kubernetes.yml namespace default name jkube-sample-helloworld
[INFO] k8s: Created Deployment: target/jkube/applyJson/default/deployment-jkube-sample-helloworld.json
[INFO] k8s: HINT: Use the command `kubectl get pods -w` to watch your pods start up
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  7.407 s
[INFO] Finished at: 2020-02-10T21:44:54+05:30
[INFO] ------------------------------------------------------------------------
``` 

3. Check logs of Created Pod:
```
~/work/repos/jkube/quickstarts/maven/hello-world : $ kubectl get pods
NAME                                       READY   STATUS        RESTARTS   AGE
jkube-sample-helloworld-7c4665f464-xwskj   0/1     Completed     2          27s
~/work/repos/jkube/quickstarts/maven/hello-world : $ kubectl logs jkube-sample-helloworld-7c4665f464-xwskj
Hello World!
```
