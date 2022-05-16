---
name: "Maven :: Spring Boot - Helm"
description: |
  Spring Boot application with a single REST endpoint.
  Demonstrates how to generate Helm chart (YAML) files using Eclipse JKube's k8s:helm Maven goal.
---
# Eclipse JKube Spring Boot with YAML Quickstart

This is a quickstart project to use Eclipse JKube plugin with customized yaml configurations.

The project is structured to be used with the following profiles:
- [Developer (Inner Loop)](#Development-environment-(Inner-Loop))
- [Production with Helm charts (Outer Loop)](#Helm-chart-generation-(Outer-Loop))

## Requirements:

- JDK 8 or 11+
- Kubernetes Cluster (Minikube, OpenShift, CRC, etc.)

## Development environment (Inner Loop)

This quickstart assumes the scenario where you have a project that will be deployed to production (Outer loop) using
Helm charts. For this purpose you would run the previously mentioned goals (`k8s:resource k8s:helm`) to generate the
charts. You might even want to configure your CI pipeline to publish the charts for you (`k8s:helm-push`)

However, developer might want to use JKube to test and deploy your project to a development environment
cluster (Inner loop). For this purpose, the placeholders defined for your Helm variables need to be completed with some
values. This showcases the power of JKube, because the same fragments and generated YAMLs can be used for those purposes.

In this example you'll find an additional Maven profile `dev` that includes the values for those placeholders, so in case
you are using this in a development environment, you can also take advantage of JKube.

```shell
$ mvn clean package k8s:apply -Pdev -Pkubernetes
```

## Helm chart generation (Outer Loop)

To generate the helm chart with defaults run:
```shell script
mvn clean package -Pkubernetes
```

### Customize Chart via Maven properties

Helm chart properties can be overridden through command line:
```shell script
mvn clean package k8s:helm -Pkubernetes -Djkube.helm.chart="Custom Chart Name"  -Djkube.helm.home=https://custom.home
```

### Customize Chart using configuration

Helm chart can also be customized through `pom.xml` plugin configuration:
```xml
<plugin>
    <groupId>org.eclipse.jkube</groupId>
    <artifactId>kubernetes-maven-plugin</artifactId>
    <configuration>
        <helm>
          <chart>Custom chart name</chart>
          <type>kubernetes</type>
          <icon>This is the icon</icon>
          <description>A customized description for the chart</description>
          <sources>
            <source>https://different.source/yaml.yml</source>
          </sources>
          <maintainers>
            <maintainer>
                <name>John</name>
                <email>john.doe@example.com</email>
            </maintainer>
          </maintainers>
        </helm>
    </configuration>
</plugin>
```

## How to test

### Docker build strategy (default)
With Minikube running, perform the following commands:
```shell script
$ eval $(minikube docker-env)
$ mvn -Pkubernetes clean package
$ helm install spring-boot-helm target/jkube/helm/spring-boot-helm/kubernetes/
$ minikube service spring-boot-helm
```

Or if you want to override the replicas value:
```shell script
$ eval $(minikube docker-env)
$ mvn -Pkubernetes clean package
$ helm install spring-boot-helm target/jkube/helm/spring-boot-helm/kubernetes/ --set replicas=2
$ minikube service spring-boot-helm
```

### JIB build strategy
With Minikube running, perform the following commands:
```shell script
$ mvn -Pkubernetes clean package -Djkube.build.strategy=jib
$ eval $(minikube docker-env)
$ docker load -i target/docker/maven/spring-boot-helm/latest/tmp/docker-build.tar
$ helm install spring-boot-helm target/jkube/helm/spring-boot-helm/kubernetes/
$ minikube service spring-boot-helm
```

### OpenShift (S2I build)
With a valid OpenShift cluster, perform the following commands:
```shell script
$ mvn -Popenshift clean package
$ helm install spring-boot-helm target/jkube/helm/spring-boot-helm/openshift/
$ curl ${route-url}
```
