---
name: "Maven :: Spring Boot Web"
description: |
  Spring Boot Web application with a single @RestController.
  Shows how to deploy Spring Boot applications to Kubernetes (-Pkubernetes) and OpenShift (-Popenshift) using Eclipse JKube.
---
# Spring Boot Sample

This is an example of a Spring Boot Web application demonstrating the use of Eclipse JKube Maven Plugin to deploy Spring Boot applications to Kubernetes and OpenShift.

> [!NOTE]
> k8s:watch / oc:watch goal won't work as Spring devtools automatically ignores projects named `spring-boot`, `spring-boot-devtools`, `spring-boot-autoconfigure`, `spring-boot-actuator`, `and spring-boot-starter`.
> 
> To learn how to use Spring Devtools with Eclipse JKube, please refer to the [`spring-boot-watch` quickstart](../spring-boot-watch/README.md).

This example consists of a single `@RestController` that returns a simple greeting message.

It is built to showcase how to deploy Spring Boot applications, both native and JVM, to Kubernetes and OpenShift using Eclipse JKube.
Let's start by checking the JVM build mode.

## JVM build

To build the application in JVM mode, run the following command:

```bash
mvn clean package
```

This command will build the application and create a JAR file in the `target` directory.

To deploy the application to Kubernetes, run the following command:

> [!NOTE]
> In case it's a Minikube cluster you want to share the Docker registry first:
> 
> `eval $(minikube docker-env)`

```bash
mvn -Pkubernetes k8s:build k8s:resource k8s:apply
```

Once the application is deployed and ready, the list of pods should display something like:

```bash
$ kubectl get pods
NAME                            READY   STATUS    RESTARTS   AGE
spring-boot-5987c95fd6-phwv4    1/1     Running   0          22s
```

You can now access your application using cURL with the following command:

```bash
$ curl $(minikube ip):$(kubectl get svc spring-boot -n default -o jsonpath='{.spec.ports[].nodePort}')
Greetings from Spring Boot!!
```

## Native Build

To build the application in JVM mode, run the following command:

```bash
mvn -Pnative clean native:compile
```

This command will build the application and create a JAR file in the `target` directory.

To deploy the application to Kubernetes, run the following command:

> [!NOTE]
> In case it's a Minikube cluster you want to share the Docker registry first:
>
> `eval $(minikube docker-env)`

```bash
mvn -Pkubernetes k8s:build k8s:resource k8s:apply
```

Once the application is deployed and ready, the list of pods should display something like:

```bash
$ kubectl get pods
NAME                            READY   STATUS    RESTARTS   AGE
spring-boot-5987c95fd6-phwv4    1/1     Running   0          22s
```

You can now access your application using cURL with the following command:

```bash
$ curl $(minikube ip):$(kubectl get svc spring-boot -n default -o jsonpath='{.spec.ports[].nodePort}')
Greetings from Spring Boot!!
```

## For OpenShift

```
minishift start
```
Below command will create your OpenShift resource descriptors.
```
mvn clean oc:resource -Popenshift
```

Now start S2I build  by hitting the build goal.
```
mvn package oc:build -Popenshift
```

Below command will deploy your application on OpenShift cluster.
```
mvn oc:deploy -Popenshift
```

#### Generating Ingress for your generated Service
Eclipse JKube is also able to generate default `Ingress` object based upon your `Service`. At the moment `Ingress` generation is only allowed for `Service` objects of type `LoadBalancer` and is disabled by default. In order to do it, you need to follow the following steps:

- Make sure you have an `Ingress` controller running inside your Kubernetes cluster, for the sake of this example; we're using [Ngnix Controller](https://kubernetes.io/docs/tasks/access-application-cluster/ingress-minikube/) bundled as an addon in Minikube:
```
# Enable Ingress Controller addon
minikube addons enable ingress

# Make sure it's running
~/work/repos/jkube/quickstarts/maven/spring-boot : $ kubectl get pods -nkube-system | grep nginx
nginx-ingress-controller-6fc5bcc8c9-gt4mg   1/1     Running   3          4h5m
```

- In order to generate `Ingress` object you need to set `jkube.createExternalUrls` property to `true` and `jkube.domain` property to your desired hostname suffix. You can also provide `routeDomain` in XML config like listed below. You can find full example in this project with profile `kubernetes-with-ingress`.
```
<configuration>
  <resources>
    <routeDomain>org.eclipse.jkube</routeDomain>
  </resources>
</configuration>
```

- Once all setup, you can run the resource goal like this:
```
~/work/repos/jkube/quickstarts/maven/spring-boot : $ mvn k8s:resource -Pkubernetes-with-ingress
[INFO] Scanning for projects...
[INFO] 
[INFO] ----------< org.eclipse.jkube.quickstarts.maven:spring-boot >-----------
[INFO] Building Eclipse JKube :: Quickstarts :: Maven :: Spring Boot Web 1.0.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.0.0-SNAPSHOT:resource (default-cli) @ spring-boot ---
[INFO] k8s: Running generator spring-boot
[INFO] k8s: spring-boot: Using Docker image quay.io/jkube/jkube-java:0.0.13 as base / builder
[INFO] k8s: jkube-controller: Adding a default Deployment
[INFO] k8s: jkube-service: Adding a default service 'spring-boot' with ports [8080]
[INFO] k8s: jkube-healthcheck-spring-boot: Adding readiness probe on port 8080, path='/actuator/health', scheme='HTTP', with initial delay 10 seconds
[INFO] k8s: jkube-healthcheck-spring-boot: Adding liveness probe on port 8080, path='/actuator/health', scheme='HTTP', with initial delay 180 seconds
[INFO] k8s: jkube-revision-history: Adding revision history limit to 2
[INFO] k8s: validating /home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot/target/classes/META-INF/jkube/kubernetes/spring-boot-deployment.yml resource
[INFO] k8s: validating /home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot/target/classes/META-INF/jkube/kubernetes/spring-boot-ingress.yml resource
[INFO] k8s: validating /home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot/target/classes/META-INF/jkube/kubernetes/spring-boot-service.yml resource
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  6.320 s
[INFO] Finished at: 2020-06-04T15:57:19+05:30
[INFO] ------------------------------------------------------------------------
~/work/repos/jkube/quickstarts/maven/spring-boot : $ ls target/classes/META-INF/jkube/kubernetes/
spring-boot-deployment.yml  spring-boot-ingress.yml  spring-boot-service.yml
~/work/repos/jkube/quickstarts/maven/spring-boot : $ 
```
- Now try to apply them onto Kubernetes cluster:
```
~/work/repos/jkube/quickstarts/maven/spring-boot : $ mvn k8s:apply -Pkubernetes-with-ingress
[INFO] Scanning for projects...
[INFO] 
[INFO] ----------< org.eclipse.jkube.quickstarts.maven:spring-boot >-----------
[INFO] Building Eclipse JKube :: Quickstarts :: Maven :: Spring Boot Web 1.0.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.0.0-SNAPSHOT:apply (default-cli) @ spring-boot ---
[INFO] k8s: Using Kubernetes at https://192.168.39.76:8443/ in namespace default with manifest /home/rohaan/work/repos/jkube/quickstarts/maven/spring-boot/target/classes/META-INF/jkube/kubernetes.yml 
[INFO] k8s: Using namespace: default
[INFO] k8s: Creating a Service from kubernetes.yml namespace default name spring-boot
[INFO] k8s: Created Service: target/jkube/applyJson/default/service-spring-boot-2.json
[INFO] k8s: Creating a Deployment from kubernetes.yml namespace default name spring-boot
[INFO] k8s: Created Deployment: target/jkube/applyJson/default/deployment-spring-boot-2.json
[WARNING] The client is using resource type 'ingresses' with unstable version 'v1beta1'
[INFO] k8s: Creating a Ingress from kubernetes.yml namespace default name spring-boot
[INFO] k8s: Created Ingress: target/jkube/applyJson/default/ingress-spring-boot-2.json
[INFO] k8s: HINT: Use the command `kubectl get pods -w` to watch your pods start up
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  9.480 s
[INFO] Finished at: 2020-06-04T16:01:23+05:30
[INFO] ------------------------------------------------------------------------
~/work/repos/jkube/quickstarts/maven/spring-boot : $ kubectl get ing
NAME          HOSTS                           ADDRESS   PORTS   AGE
spring-boot   spring-boot.org.eclipse.jkube             80      12s
~/work/repos/jkube/quickstarts/maven/spring-boot : $ kubectl get svc
NAME          TYPE           CLUSTER-IP    EXTERNAL-IP   PORT(S)          AGE
kubernetes    ClusterIP      10.96.0.1     <none>        443/TCP          33m
spring-boot   LoadBalancer   10.96.99.20   <pending>     8080:32648/TCP   14s
```
Make sure you have this entry in your `/etc/hosts` file like this:
```
192.168.39.76(cluster ip)  spring-boot.org.eclipse.jkube
```
Once done, you can try curling with your host URL:
```
~/work/repos/jkube/quickstarts/maven/spring-boot : $ curl spring-boot.org.eclipse.jkube/
Greetings from Spring Boot!!
```
