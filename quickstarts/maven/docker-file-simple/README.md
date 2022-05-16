---
name: "Maven :: Docker File Provided Context and Assembly"
description: |
  Spring Boot Web application with a single @RestController.
  Shows how to deploy Spring Boot applications to Kubernetes using existing Dockerfile with Eclipse JKube.
---
# Eclipse JKube sample with simple Dockerfile mode

You can build your Docker images with just providing one `Dockerfile` in your project root directory and Eclipse JKube
will pick it up for docker build. 
For simple `Dockerfile` mode, your project's current directory is provided as docker context directory for build.
If you want to copy some files from your current directory(`target/docker-file-simple.jar` in this case), you need
to prefix project directory contents with default assembly name(i.e `maven`). So your `Dockerfile` would look like this:
```
FROM openjdk:latest
COPY maven/target/docker-file-simple.jar /deployments/docker-file-simple.jar
COPY maven/static-dir-in-project-root/my-file.txt /deployments/my-file.txt
CMD ["java", "-jar", "/deployments/docker-file-simple.jar"]
```

In order to provide a valid Kubernetes service YAML manifest, the following properties are added to [pom.xml](pom.xml)
to make the Pod port accessible:
```xml
<jkube.enricher.jkube-service.port>8080</jkube.enricher.jkube-service.port>
<jkube.enricher.jkube-service.type>NodePort</jkube.enricher.jkube-service.type>
```

# Building Docker image
```shell script
$ mvn package k8s:build
[INFO] Scanning for projects...
[INFO] 
[INFO] ----------------< org.eclipse.jkube:docker-file-simple >----------------
[INFO] Building Eclipse JKube :: Quickstarts :: Maven :: Dockerfile :: Simple 1.0.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.0.0-SNAPSHOT:build (default-cli) @ docker-file-simple ---
[INFO] k8s: Running in Kubernetes mode
[INFO] k8s: Building Docker image in Kubernetes mode
[INFO] k8s: [jkube/docker-file-simple:latest]: Created docker-build.tar in 261 milliseconds
[INFO] k8s: [jkube/docker-file-simple:latest]: Built image sha256:52d46
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  2.977 s
[INFO] Finished at: 2020-06-11T07:59:44+02:00
[INFO] ------------------------------------------------------------------------
$ docker images | grep docker-file-simple
jkube/docker-file-simple                  1.0                         c8df6da232b3        8 seconds ago       516MB
```

# Generating Kubernetes Manifests and Deploying to Kubernetes
```shell script
$ mvn k8s:resource k8s:apply
[INFO] Scanning for projects...
[INFO] 
[INFO] ----------------< org.eclipse.jkube:docker-file-simple >----------------
[INFO] Building Eclipse JKube :: Quickstarts :: Maven :: Dockerfile :: Simple 1.0.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.0.0-SNAPSHOT:resource (default-cli) @ docker-file-simple ---
[INFO] k8s: jkube-controller: Adding a default Deployment
[INFO] k8s: jkube-service: Adding a default service 'docker-file-simple' with ports [8080]
[INFO] k8s: jkube-healthcheck-spring-boot: Adding readiness probe on port 8080, path='/actuator/health', scheme='HTTP', with initial delay 10 seconds
[INFO] k8s: jkube-healthcheck-spring-boot: Adding liveness probe on port 8080, path='/actuator/health', scheme='HTTP', with initial delay 180 seconds
[INFO] k8s: jkube-revision-history: Adding revision history limit to 2
[INFO] k8s: validating /home/user/00-MN/projects/forks/jkube/quickstarts/maven/docker-file-simple/target/classes/META-INF/jkube/kubernetes/docker-file-simple-deployment.yml resource
[INFO] k8s: validating /home/user/00-MN/projects/forks/jkube/quickstarts/maven/docker-file-simple/target/classes/META-INF/jkube/kubernetes/docker-file-simple-service.yml resource
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.0.0-SNAPSHOT:apply (default-cli) @ docker-file-simple ---
[INFO] k8s: Using Kubernetes at https://172.17.0.3:8443/ in namespace default with manifest /home/user/00-MN/projects/forks/jkube/quickstarts/maven/docker-file-simple/target/classes/META-INF/jkube/kubernetes.yml 
[INFO] k8s: Using namespace: default
[INFO] k8s: Updating a Service from kubernetes.yml
[INFO] k8s: Updated Service: target/jkube/applyJson/default/service-docker-file-simple-1.json
[INFO] k8s: Updating Deployment from kubernetes.yml
[INFO] k8s: Updated Deployment: target/jkube/applyJson/default/deployment-docker-file-simple-2.json
[INFO] k8s: HINT: Use the command `kubectl get pods -w` to watch your pods start up
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  7.688 s
[INFO] Finished at: 2020-06-11T08:01:21+02:00
[INFO] ------------------------------------------------------------------------
$ kubectl get pods
NAME                                      READY   STATUS             RESTARTS   AGE
docker-file-simple-686cddc6b9-lvgtv       0/1     Running            0          84s
$ kubectl logs pod/zero-config-dockerfile-686cddc6b9-lvgtv
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.2.7.RELEASE)

2020-06-11 05:51:57.509  INFO 1 --- [           main] o.e.j.quickstart.maven.helloworld.App    : Starting App v1.0.0-SNAPSHOT on docker-file-simple-9dbf5f98c-wmhh5 with PID 1 (/deployments/docker-file-simple.jar started by root in /)
2020-06-11 05:51:57.516  INFO 1 --- [           main] o.e.j.quickstart.maven.helloworld.App    : No active profile set, falling back to default profiles: default
2020-06-11 05:51:59.793  INFO 1 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 8080 (http)
2020-06-11 05:51:59.807  INFO 1 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2020-06-11 05:51:59.808  INFO 1 --- [           main] org.apache.catalina.core.StandardEngine  : Starting Servlet engine: [Apache Tomcat/9.0.34]
2020-06-11 05:51:59.926  INFO 1 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2020-06-11 05:51:59.926  INFO 1 --- [           main] o.s.web.context.ContextLoader            : Root WebApplicationContext: initialization completed in 2241 ms
2020-06-11 05:52:00.931  INFO 1 --- [           main] o.s.s.concurrent.ThreadPoolTaskExecutor  : Initializing ExecutorService 'applicationTaskExecutor'
2020-06-11 05:52:01.296  INFO 1 --- [           main] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 2 endpoint(s) beneath base path '/actuator'
2020-06-11 05:52:01.407  INFO 1 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http) with context path ''
2020-06-11 05:52:01.411  INFO 1 --- [           main] o.e.j.quickstart.maven.helloworld.App    : Started App in 4.751 seconds (JVM running for 5.453)
2020-06-11 05:52:13.373  INFO 1 --- [nio-8080-exec-1] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring DispatcherServlet 'dispatcherServlet'
2020-06-11 05:52:13.374  INFO 1 --- [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Initializing Servlet 'dispatcherServlet'
2020-06-11 05:52:13.384  INFO 1 --- [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Completed initialization in 9 ms
$ curl $(minikube ip):$(kg svc docker-file-simple -n default -o jsonpath='{.spec.ports[].nodePort}')/static-file
This is a dummy file which should be copied inside Dockerfile
```
