# Eclipse JKube sample with zero config Dockerfile mode

You can build your Docker images with just providing one `Dockerfile` in your project root directory and Eclipse JKube would pick it up for docker build. This sample demonstrates this zero config use case. For simple `Dockerfile` mode, your project's current directory is provided as docker context directory for build. If you want to copy some files from your current directory(`target/zero-config-dockerfile.jar` in this case), you need to prefix project directory contents with default assembly name(i.e `maven`). So your `Dockerfile` would look like this:
```
FROM openjdk:latest
# Your context directory(project basedirectory in this case) is copied to maven/
COPY maven/target/zero-config-dockerfile.jar /tmp/zero-config-dockerfile.jar
CMD ["java", "-jar", "/tmp/zero-config-dockerfile.jar"]
```

# Building Docker image
```
~/work/repos/jkube-dockerfile-sample : $ mvn k8s:build
[INFO] Scanning for projects...
[INFO] 
[INFO] --------------< org.eclipse.jkube:zero-config-dockerfile >--------------
[INFO] Building zero-config-dockerfile 1.0
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.0.0-SNAPSHOT:build (default-cli) @ zero-config-dockerfile ---
[INFO] k8s: Running in Kubernetes mode
[INFO] k8s: Building Docker image in Kubernetes mode
[INFO] k8s: [jkube/zero-config-dockerfile:1.0]: Created docker-build.tar in 236 milliseconds
[INFO] k8s: [jkube/zero-config-dockerfile:1.0]: Built image sha256:c8df6
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  6.022 s
[INFO] Finished at: 2020-05-22T19:52:26+05:30
[INFO] ------------------------------------------------------------------------
~/work/repos/jkube-dockerfile-sample : $ docker images | grep zero-config-dockerfile
jkube/zero-config-dockerfile                    1.0                         c8df6da232b3        8 seconds ago       544MB
~/work/repos/jkube-dockerfile-sample : $ 
```

# Generating Kubernetes Manifests and Deploying to Kubernetes
```
~/work/repos/jkube-dockerfile-sample : $ mvn k8s:resource k8s:apply
[INFO] Scanning for projects...
[INFO] 
[INFO] --------------< org.eclipse.jkube:zero-config-dockerfile >--------------
[INFO] Building zero-config-dockerfile 1.0
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.0.0-SNAPSHOT:resource (default-cli) @ zero-config-dockerfile ---
[INFO] k8s: jkube-controller: Adding a default Deployment
[INFO] k8s: jkube-healthcheck-spring-boot: Adding readiness probe on port 8080, path='/actuator/health', scheme='HTTP', with initial delay 10 seconds
[INFO] k8s: jkube-healthcheck-spring-boot: Adding liveness probe on port 8080, path='/actuator/health', scheme='HTTP', with initial delay 180 seconds
[INFO] k8s: jkube-revision-history: Adding revision history limit to 2
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.0.0-SNAPSHOT:apply (default-cli) @ zero-config-dockerfile ---
[INFO] k8s: Using Kubernetes at https://192.168.39.124:8443/ in namespace default with manifest /home/rohaan/work/repos/jkube-dockerfile-sample/target/classes/META-INF/jkube/kubernetes.yml 
[INFO] k8s: Using namespace: default
[INFO] k8s: Creating a Deployment from kubernetes.yml namespace default name zero-config-dockerfile
[INFO] k8s: Created Deployment: target/jkube/applyJson/default/deployment-zero-config-dockerfile.json
[INFO] k8s: HINT: Use the command `kubectl get pods -w` to watch your pods start up
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  4.539 s
[INFO] Finished at: 2020-05-22T19:53:12+05:30
[INFO] ------------------------------------------------------------------------
~/work/repos/jkube-dockerfile-sample : $ kubectl get pods
NAME                                      READY   STATUS             RESTARTS   AGE
zero-config-dockerfile-686cddc6b9-lvgtv   0/1     CrashLoopBackOff   3          84s
~/work/repos/jkube-dockerfile-sample : $ kubectl logs pod/zero-config-dockerfile-686cddc6b9-lvgtv
Hello World!
~/work/repos/jkube-dockerfile-sample : $ 
```
Since this application just prints "Hello World!" message, it keeps on restarting(`CrashLoopBackOff`) state.
