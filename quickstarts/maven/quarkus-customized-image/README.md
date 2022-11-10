---
name: "Maven :: Quarkus customized Image"
description: |
  Quarkus application with a single JAX-RS endpoint.
  Demonstrates how to build a Quarkus container image based on a Red Hat container image private registry.
  Uses a pull secret from Red Hat Registry Service Accounts to authenticate.
---
# Eclipse JKube Quarkus with Customized Image Quickstart

A simple REST application demonstrating usage of Eclipse JKube with Quarkus
customized to use an official
[Red Hat Container Image](https://catalog.redhat.com/software/containers/search).

## Requirements:

- JDK 11+
- OpenShift Cluster (OpenShift, CRC, etc.)
- Registered pull secret in your cluster ([Registry Service Accounts](https://access.redhat.com/terms-based-registry/#/accounts))


## How to run

**Note:**
> To be able to retrieve the image from the catalog you'll have to modify the 
property `<jkube.build.pullSecret>12819530-ocp42-exposed-env-pull-secret-pull-secret</jkube.build.pullSecret>`
in your `pom.xml`, or specify it in you mvn invocation `-Dkube.build.pullSecret=...`

```shell script
$ mvn clean package oc:build oc:resource oc:apply

[INFO] Scanning for projects...
[INFO] 
[INFO] ----< org.eclipse.jkube.quickstarts.maven:quarkus-customized-image >----
[INFO] Building Eclipse JKube :: Quickstarts :: Maven :: Quarkus customized Image 1.0.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- maven-clean-plugin:2.5:clean (default-clean) @ quarkus-customized-image ---
[INFO] Deleting /home/user/00-MN/projects/forks/jkube/quickstarts/maven/quarkus-customized-image/target
[INFO] 
[INFO] --- maven-resources-plugin:2.6:resources (default-resources) @ quarkus-customized-image ---
[WARNING] Using platform encoding (UTF-8 actually) to copy filtered resources, i.e. build is platform dependent!
[INFO] skip non existing resourceDirectory /home/user/00-MN/projects/forks/jkube/quickstarts/maven/quarkus-customized-image/src/main/resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.1:compile (default-compile) @ quarkus-customized-image ---
[INFO] Changes detected - recompiling the module!
[WARNING] File encoding has not been set, using platform encoding UTF-8, i.e. build is platform dependent!
[INFO] Compiling 3 source files to /home/user/00-MN/projects/forks/jkube/quickstarts/maven/quarkus-customized-image/target/classes
[INFO] 
[INFO] --- maven-resources-plugin:2.6:testResources (default-testResources) @ quarkus-customized-image ---
[WARNING] Using platform encoding (UTF-8 actually) to copy filtered resources, i.e. build is platform dependent!
[INFO] skip non existing resourceDirectory /home/user/00-MN/projects/forks/jkube/quickstarts/maven/quarkus-customized-image/src/test/resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.1:testCompile (default-testCompile) @ quarkus-customized-image ---
[INFO] No sources to compile
[INFO] 
[INFO] --- maven-surefire-plugin:2.12.4:test (default-test) @ quarkus-customized-image ---
[INFO] No tests to run.
[INFO] 
[INFO] --- maven-jar-plugin:2.4:jar (default-jar) @ quarkus-customized-image ---
[INFO] Building jar: /home/user/00-MN/projects/forks/jkube/quickstarts/maven/quarkus-customized-image/target/quarkus-customized-image-1.0.0-SNAPSHOT.jar
[INFO] 
[INFO] --- quarkus-maven-plugin:1.4.1.Final:build (default) @ quarkus-customized-image ---
[WARNING] [io.quarkus.deployment.QuarkusAugmentor] Using Java versions older than 11 to build Quarkus applications is deprecated and will be disallowed in a future release!
[INFO] [org.jboss.threads] JBoss Threads version 3.1.1.Final
[INFO] [io.quarkus.deployment.pkg.steps.JarResultBuildStep] Building thin jar: /home/user/00-MN/projects/forks/jkube/quickstarts/maven/quarkus-customized-image/target/quarkus-customized-image-1.0.0-SNAPSHOT-runner.jar
[INFO] [io.quarkus.deployment.QuarkusAugmentor] Quarkus augmentation completed in 1183ms
[INFO] 
[INFO] --- openshift-maven-plugin:1.0.0-SNAPSHOT:build (default-cli) @ quarkus-customized-image ---
[INFO] oc: Using OpenShift build with strategy S2I
[INFO] oc: [org.eclipse.jkube.quickstarts.maven/quarkus-customized-image:latest]: Created docker source tar /home/user/00-MN/projects/forks/jkube/quickstarts/maven/quarkus-customized-image/target/docker/org.eclipse.jkube.quickstarts.maven/quarkus-customized-image/tmp/docker-build.tar
[INFO] oc: Adding to Secret 12819530-ocp42-exposed-env-pull-secret-pull-secret
[INFO] oc: Using Secret 12819530-ocp42-exposed-env-pull-secret-pull-secret
[INFO] oc: Updating BuildServiceConfig quarkus-customized-image-s2i for Source strategy
[INFO] oc: Adding to ImageStream quarkus-customized-image
[INFO] oc: Starting Build quarkus-customized-image-s2i
[INFO] oc: Waiting for build quarkus-customized-image-s2i-5 to complete...
[INFO] oc: Using registry.redhat.io/openjdk/openjdk-11-rhel8:1.2-3.1587486933 as the s2i builder image
[INFO] oc: INFO S2I source build with plain binaries detected
[INFO] oc: INFO S2I binary build from fabric8-maven-plugin detected
[INFO] oc: INFO Copying binaries from /tmp/src/maven to /deployments ...
[INFO] oc: quarkus-customized-image-1.0.0-SNAPSHOT-runner.jar
[INFO] oc: quarkus-customized-image-1.0.0-SNAPSHOT.jar
[INFO] oc: lib/
[INFO] oc: lib/com.fasterxml.jackson.core.jackson-annotations-2.10.1.jar
[INFO] oc: lib/com.fasterxml.jackson.core.jackson-core-2.10.1.jar
[INFO] oc: lib/com.fasterxml.jackson.core.jackson-databind-2.10.3.jar
[INFO] oc: lib/com.fasterxml.jackson.datatype.jackson-datatype-jdk8-2.10.3.jar
[INFO] oc: lib/com.fasterxml.jackson.datatype.jackson-datatype-jsr310-2.10.3.jar
[INFO] oc: lib/com.fasterxml.jackson.jaxrs.jackson-jaxrs-base-2.10.1.jar
[INFO] oc: lib/com.fasterxml.jackson.jaxrs.jackson-jaxrs-json-provider-2.10.1.jar
[INFO] oc: lib/com.fasterxml.jackson.module.jackson-module-jaxb-annotations-2.10.1.jar
[INFO] oc: lib/com.fasterxml.jackson.module.jackson-module-parameter-names-2.10.3.jar
[INFO] oc: lib/com.github.fge.btf-1.2.jar
[INFO] oc: lib/com.github.fge.jackson-coreutils-1.6.jar
[INFO] oc: lib/com.github.fge.json-patch-1.9.jar
[INFO] oc: lib/com.github.fge.msg-simple-1.1.jar
[INFO] oc: lib/com.google.guava.failureaccess-1.0.1.jar
[INFO] oc: lib/com.google.guava.guava-28.1-jre.jar
[INFO] oc: lib/com.google.guava.listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar
[INFO] oc: lib/com.sun.activation.jakarta.activation-1.2.1.jar
[INFO] oc: lib/com.sun.istack.istack-commons-runtime-3.0.10.jar
[INFO] oc: lib/io.netty.netty-buffer-4.1.48.Final.jar
[INFO] oc: lib/io.netty.netty-codec-4.1.45.Final.jar
[INFO] oc: lib/io.netty.netty-codec-dns-4.1.42.Final.jar
[INFO] oc: lib/io.netty.netty-codec-http-4.1.42.Final.jar
[INFO] oc: lib/io.netty.netty-codec-http2-4.1.42.Final.jar
[INFO] oc: lib/io.netty.netty-codec-socks-4.1.42.Final.jar
[INFO] oc: lib/io.netty.netty-common-4.1.48.Final.jar
[INFO] oc: lib/io.netty.netty-handler-4.1.45.Final.jar
[INFO] oc: lib/io.netty.netty-handler-proxy-4.1.42.Final.jar
[INFO] oc: lib/io.netty.netty-resolver-4.1.42.Final.jar
[INFO] oc: lib/io.netty.netty-resolver-dns-4.1.42.Final.jar
[INFO] oc: lib/io.netty.netty-transport-4.1.42.Final.jar
[INFO] oc: lib/io.quarkus.arc.arc-1.4.1.Final.jar
[INFO] oc: lib/io.quarkus.quarkus-arc-1.4.1.Final.jar
[INFO] oc: lib/io.quarkus.quarkus-core-1.4.1.Final.jar
[INFO] oc: lib/io.quarkus.quarkus-development-mode-spi-1.4.1.Final.jar
[INFO] oc: lib/io.quarkus.quarkus-jackson-1.4.1.Final.jar
[INFO] oc: lib/io.quarkus.quarkus-netty-1.4.1.Final.jar
[INFO] oc: lib/io.quarkus.quarkus-resteasy-1.4.1.Final.jar
[INFO] oc: lib/io.quarkus.quarkus-resteasy-common-1.4.1.Final.jar
[INFO] oc: lib/io.quarkus.quarkus-resteasy-jackson-1.4.1.Final.jar
[INFO] oc: lib/io.quarkus.quarkus-resteasy-server-common-1.4.1.Final.jar
[INFO] oc: lib/io.quarkus.quarkus-vertx-core-1.4.1.Final.jar
[INFO] oc: lib/io.quarkus.quarkus-vertx-http-1.4.1.Final.jar
[INFO] oc: lib/io.quarkus.security.quarkus-security-1.1.0.Final.jar
[INFO] oc: lib/io.smallrye.config.smallrye-config-1.7.0.jar
[INFO] oc: lib/io.smallrye.config.smallrye-config-common-1.7.0.jar
[INFO] oc: lib/io.smallrye.reactive.mutiny-0.4.3.jar
[INFO] oc: lib/io.vertx.vertx-auth-common-3.8.5.jar
[INFO] oc: lib/io.vertx.vertx-bridge-common-3.8.5.jar
[INFO] oc: lib/io.vertx.vertx-core-3.8.5.jar
[INFO] oc: lib/io.vertx.vertx-web-3.8.5.jar
[INFO] oc: lib/io.vertx.vertx-web-common-3.8.5.jar
[INFO] oc: lib/jakarta.activation.jakarta.activation-api-1.2.1.jar
[INFO] oc: lib/jakarta.annotation.jakarta.annotation-api-1.3.5.jar
[INFO] oc: lib/jakarta.ejb.jakarta.ejb-api-3.2.6.jar
[INFO] oc: lib/jakarta.el.jakarta.el-api-3.0.3.jar
[INFO] oc: lib/jakarta.enterprise.jakarta.enterprise.cdi-api-2.0.2.jar
[INFO] oc: lib/jakarta.inject.jakarta.inject-api-1.0.jar
[INFO] oc: lib/jakarta.interceptor.jakarta.interceptor-api-1.2.5.jar
[INFO] oc: lib/jakarta.transaction.jakarta.transaction-api-1.3.3.jar
[INFO] oc: lib/jakarta.validation.jakarta.validation-api-2.0.2.jar
[INFO] oc: lib/org.eclipse.microprofile.config.microprofile-config-api-1.4.jar
[INFO] oc: lib/org.eclipse.microprofile.context-propagation.microprofile-context-propagation-api-1.0.1.jar
[INFO] oc: lib/org.glassfish.jaxb.jaxb-runtime-2.3.3-b02.jar
[INFO] oc: lib/org.glassfish.jaxb.txw2-2.3.3-b02.jar
[INFO] oc: lib/org.graalvm.sdk.graal-sdk-19.3.1.jar
[INFO] oc: lib/org.jboss.logging.jboss-logging-3.3.2.Final.jar
[INFO] oc: lib/org.jboss.logging.jboss-logging-annotations-2.1.0.Final.jar
[INFO] oc: lib/org.jboss.logmanager.jboss-logmanager-embedded-1.0.4.jar
[INFO] oc: lib/org.jboss.resteasy.resteasy-core-4.5.3.Final.jar
[INFO] oc: lib/org.jboss.resteasy.resteasy-core-spi-4.5.3.Final.jar
[INFO] oc: lib/org.jboss.resteasy.resteasy-jackson2-provider-4.5.3.Final.jar
[INFO] oc: lib/org.jboss.resteasy.resteasy-jaxb-provider-4.5.3.Final.jar
[INFO] oc: lib/org.jboss.slf4j.slf4j-jboss-logging-1.2.0.Final.jar
[INFO] oc: lib/org.jboss.spec.javax.ws.rs.jboss-jaxrs-api_2.1_spec-2.0.1.Final.jar
[INFO] oc: lib/org.jboss.spec.javax.xml.bind.jboss-jaxb-api_2.3_spec-2.0.0.Final.jar
[INFO] oc: lib/org.jboss.threads.jboss-threads-3.1.1.Final.jar
[INFO] oc: lib/org.reactivestreams.reactive-streams-1.0.3.jar
[INFO] oc: lib/org.slf4j.slf4j-api-1.7.30.jar
[INFO] oc: lib/org.wildfly.common.wildfly-common-1.5.4.Final-format-001.jar
[INFO] oc: 
[INFO] oc: Pushing image docker-registry.default.svc:5000/jkube/quarkus-customized-image:latest ...
[INFO] oc: Pushed 3/4 layers, 78% complete
[INFO] oc: Pushed 4/4 layers, 100% complete
[INFO] oc: Push successful
[INFO] oc: Build quarkus-customized-image-s2i-5 in status Complete
[INFO] oc: Found tag on ImageStream quarkus-customized-image tag: sha256:e9aa9682e622d8ed390358d30f3341995c248d9276f8a046a2b43c2fe48c6174
[INFO] oc: ImageStream quarkus-customized-image written to /home/user/00-MN/projects/forks/jkube/quickstarts/maven/quarkus-customized-image/target/quarkus-customized-image-is.yml
[INFO] 
[INFO] --- openshift-maven-plugin:1.0.0-SNAPSHOT:resource (default-cli) @ quarkus-customized-image ---
[INFO] oc: Using docker image name of namespace: jkube
[INFO] oc: jkube-controller: Adding a default DeploymentConfig
[INFO] oc: jkube-service: Adding a default service 'quarkus-customized-image' with ports [8080]
[INFO] oc: jkube-healthcheck-vertx: HTTP health check disabled (path not set)
[INFO] oc: jkube-healthcheck-vertx: HTTP health check disabled (path not set)
[INFO] oc: jkube-revision-history: Adding revision history limit to 2
[INFO] 
[INFO] --- openshift-maven-plugin:1.0.0-SNAPSHOT:apply (default-cli) @ quarkus-customized-image ---
[WARNING] oc: OpenShift cluster detected, using Kubernetes manifests
[WARNING] oc: Switch to openshift-maven-plugin in case there are any problems
[INFO] oc: Using OpenShift at https://openshift.sharedocp311cns.lab.rdu2.cee.redhat.com:443/ in namespace jkube with manifest /home/user/00-MN/projects/forks/jkube/quickstarts/maven/quarkus-customized-image/target/classes/META-INF/jkube/openshift.yml 
[INFO] OpenShift platform detected
[INFO] oc: Using project: jkube
[INFO] oc: Updating a Service from openshift.yml
[INFO] oc: Updated Service: target/jkube/applyJson/jkube/service-quarkus-customized-image.json
[INFO] oc: Updating DeploymentConfig from openshift.yml
[INFO] oc: Updated DeploymentConfig: target/jkube/applyJson/jkube/deploymentconfig-quarkus-customized-image.json
[INFO] oc: HINT: Use the command `oc get pods -w` to watch your pods start up
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  04:38 min
[INFO] Finished at: 2020-04-29T15:07:49+02:00
[INFO] ------------------------------------------------------------------------
```

In order to clean up the resources in the cluster:
```shell script
$ mvn oc:undeploy
```
