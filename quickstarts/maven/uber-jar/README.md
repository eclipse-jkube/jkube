---
name: "Maven :: Uber Jar"
description: |
  Demo project for getting started with Eclipse JKube and packaging the result in a uber-jar.
  It runs a picocli application that would greet the user.
---
# JKube Uber Jar Sample

This is a demo project for getting started with Eclipse JKube. It runs a picocli application that would
greet the user. We would be using Eclipse JKube for building a docker image and deploying to Kubernetes
in single command.

1. Make sure you've minikube up and running.
2. Run the following command to run uber-jar sample: 
```
~/work/repos/jkube/quickstarts/maven/uber-jar : $ mvn clean install k8s:build k8s:resource k8s:apply
[INFO] Scanning for projects...
[WARNING] 
[WARNING] Some problems were encountered while building the effective model for org.eclipse.jkube.quickstarts.maven:uberjar:jar:1.13.1
[WARNING] 'build.plugins.plugin.version' for org.apache.maven.plugins:maven-shade-plugin is missing. @ line 76, column 15
[WARNING] 
[WARNING] It is highly recommended to fix these problems because they threaten the stability of your build.
[WARNING] 
[WARNING] For this reason, future Maven versions might no longer support building such malformed projects.
[WARNING] 
[INFO] 
[INFO] ------------< org.eclipse.jkube.quickstarts.maven:uberjar >-------------
[INFO] Building Eclipse JKube :: Quickstarts :: Maven :: Uber Jar 1.13.1
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- maven-clean-plugin:2.5:clean (default-clean) @ uberjar ---
[INFO] Deleting /Users/aperuffo/workspace/jkube/quickstarts/maven/uber-jar/target
[INFO] 
[INFO] --- maven-resources-plugin:2.6:resources (default-resources) @ uberjar ---
[WARNING] Using platform encoding (UTF-8 actually) to copy filtered resources, i.e. build is platform dependent!
[INFO] skip non existing resourceDirectory /Users/aperuffo/workspace/jkube/quickstarts/maven/uber-jar/src/main/resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.6.1:compile (default-compile) @ uberjar ---
[INFO] Changes detected - recompiling the module!
[WARNING] File encoding has not been set, using platform encoding UTF-8, i.e. build is platform dependent!
[INFO] Compiling 1 source file to /Users/aperuffo/workspace/jkube/quickstarts/maven/uber-jar/target/classes
[INFO] 
[INFO] --- maven-resources-plugin:2.6:testResources (default-testResources) @ uberjar ---
[WARNING] Using platform encoding (UTF-8 actually) to copy filtered resources, i.e. build is platform dependent!
[INFO] skip non existing resourceDirectory /Users/aperuffo/workspace/jkube/quickstarts/maven/uber-jar/src/test/resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.6.1:testCompile (default-testCompile) @ uberjar ---
[INFO] No sources to compile
[INFO] 
[INFO] --- maven-surefire-plugin:2.12.4:test (default-test) @ uberjar ---
[INFO] No tests to run.
[INFO] 
[INFO] --- maven-jar-plugin:3.0.2:jar (default-jar) @ uberjar ---
[INFO] Building jar: /Users/aperuffo/workspace/jkube/quickstarts/maven/uber-jar/target/uberjar-1.13.1.jar
[INFO] 
[INFO] --- maven-shade-plugin:3.5.0:shade (default) @ uberjar ---
[INFO] Including info.picocli:picocli:jar:4.7.4 in the shaded jar.
[INFO] Dependency-reduced POM written at: /Users/aperuffo/workspace/jkube/quickstarts/maven/uber-jar/dependency-reduced-pom.xml
[WARNING] picocli-4.7.4.jar, uberjar-1.13.1.jar define 1 overlapping resource: 
[WARNING]   - META-INF/MANIFEST.MF
[WARNING] maven-shade-plugin has detected that some files are
[WARNING] present in two or more JARs. When this happens, only one
[WARNING] single version of the file is copied to the uber jar.
[WARNING] Usually this is not harmful and you can skip these warnings,
[WARNING] otherwise try to manually exclude artifacts based on
[WARNING] mvn dependency:tree -Ddetail=true and the above output.
[WARNING] See https://maven.apache.org/plugins/maven-shade-plugin/
[INFO] Replacing original artifact with shaded artifact.
[INFO] Replacing /Users/aperuffo/workspace/jkube/quickstarts/maven/uber-jar/target/uberjar-1.13.1.jar with /Users/aperuffo/workspace/jkube/quickstarts/maven/uber-jar/target/uberjar-1.13.1-shaded.jar
[INFO] 
[INFO] --- maven-install-plugin:2.4:install (default-install) @ uberjar ---
[INFO] Installing /Users/aperuffo/workspace/jkube/quickstarts/maven/uber-jar/target/uberjar-1.13.1.jar to /Users/aperuffo/.m2/repository/org/eclipse/jkube/quickstarts/maven/uberjar/1.13.1/uberjar-1.13.1.jar
[INFO] Installing /Users/aperuffo/workspace/jkube/quickstarts/maven/uber-jar/dependency-reduced-pom.xml to /Users/aperuffo/.m2/repository/org/eclipse/jkube/quickstarts/maven/uberjar/1.13.1/uberjar-1.13.1.pom
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.13.1:build (default-cli) @ uberjar ---
[INFO] k8s: Building Docker image
[INFO] k8s: Running generator java-exec
[INFO] k8s: java-exec: Using Docker image quay.io/jkube/jkube-java:0.0.19 as base / builder
[INFO] k8s: [uberjar-java:1.13.1] : Skipped building (Image configuration has no build settings)
[INFO] k8s: [maven/uberjar:1.13.1] "java-exec": Created docker-build.tar in 79 milliseconds
[INFO] k8s: [maven/uberjar:1.13.1] "java-exec": Built image sha256:102cf
[INFO] k8s: [maven/uberjar:1.13.1] "java-exec": Removed old image sha256:89983
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.13.1:resource (default-cli) @ uberjar ---
[INFO] k8s: Running generator java-exec
[INFO] k8s: java-exec: Using Docker image quay.io/jkube/jkube-java:0.0.19 as base / builder
[INFO] k8s: Using resource templates from /Users/aperuffo/workspace/jkube/quickstarts/maven/uber-jar/src/main/jkube
[INFO] k8s: jkube-controller: Adding a default Deployment
[INFO] k8s: jkube-service: Adding a default service 'uberjar' with ports [8080]
[INFO] k8s: jkube-service-discovery: Using first mentioned service port '8080' 
[INFO] k8s: jkube-revision-history: Adding revision history limit to 2
[INFO] k8s: validating /Users/aperuffo/workspace/jkube/quickstarts/maven/uber-jar/target/classes/META-INF/jkube/kubernetes/uberjar-service.yml resource
[INFO] k8s: validating /Users/aperuffo/workspace/jkube/quickstarts/maven/uber-jar/target/classes/META-INF/jkube/kubernetes/uberjar-deployment.yml resource
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.13.1:apply (default-cli) @ uberjar ---
[INFO] k8s: Using Kubernetes at https://127.0.0.1:57922/ in namespace null with manifest /Users/aperuffo/workspace/jkube/quickstarts/maven/uber-jar/target/classes/META-INF/jkube/kubernetes.yml 
[INFO] k8s: Updating Service from kubernetes.yml
[INFO] k8s: Updated Service: target/jkube/applyJson/default/service-uberjar.json
[INFO] k8s: Updating Deployment from kubernetes.yml
[INFO] k8s: Updated Deployment: target/jkube/applyJson/default/deployment-uberjar.json
[INFO] k8s: HINT: Use the command `kubectl get pods -w` to watch your pods start up
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  11.424 s
[INFO] Finished at: 2023-06-27T17:21:08+01:00
[INFO] ------------------------------------------------------------------------
``` 

3. Check logs of Created Pod:
```
~/work/repos/jkube/quickstarts/maven/hello-world : $ kubectl get pods
NAME                                       READY   STATUS        RESTARTS   AGE
uberjar-759d5f579-b662s                    0/1     Completed     2          110s
~/work/repos/jkube/quickstarts/maven/hello-world : $ kubectl logs uberjar-759d5f579-b662s
Starting the Java application using /opt/jboss/container/java/run/run-java.sh ...
INFO exec  java -javaagent:/usr/share/java/jolokia-jvm-agent/jolokia-jvm.jar=config=/opt/jboss/container/jolokia/etc/jolokia.properties -javaagent:/usr/share/java/prometheus-jmx-exporter/jmx_prometheus_javaagent.jar=9779:/opt/jboss/container/prometheus/etc/jmx-exporter-config.yaml -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20 -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -XX:+ExitOnOutOfMemoryError -cp "." -jar /deployments/uberjar-1.13.1.jar  
Hello picocli, go go commando!
```
