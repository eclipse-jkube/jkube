---
name: "Maven :: Spring Boot - Helm"
description: |
  Spring Boot application with a single REST endpoint.
  Demonstrates how to generate Helm chart (YAML) files using Eclipse JKube's k8s:helm Maven goal.
---
# Eclipse JKube, Spring Boot, and Helm Quickstart

This is a quickstart project to use Eclipse JKube's Kubernetes Maven Plugin with customized YAML configurations.

The project is structured to be used with the following profiles:
- [Development environment (Inner Loop)](#development-environment-(inner-loop))
- [Production environment with Helm charts (Outer Loop)](#production-environment-with-helm-charts-(outer-loop))

This example assumes that you are working in a project that will be deployed to production (Outer loop) using Helm charts.

The project is also set up to be deployed to a development environment cluster (Inner loop) using JKube functionality.

## Requirements:

- JDK 17+
- Kubernetes Cluster (Minikube, OpenShift, CRC, etc.)

## YAML fragments

This project contains multiple YAML fragments with placeholders that can be used to set different values depending on the target environment.

For example, the `deployment.yaml` fragment contains (among others) a placeholder for the number of replicas:

```yaml
spec:
  replicas: ${deployment.replicas}
```

## Development environment (Inner Loop)

<a id="development-environment-(inner-loop)"></a>

Let's start by analyzing the project structure for this use case.
The project `pom.xml` contains the plugin dependency in the generic `<build>` `<plugins>` section.
The plugin is not yet configured since all the default values are suitable for the inner loop scenario.

If we were to generate the Kubernetes resources (`k8s:resource`) without providing further configuration,
JKube would warn us with messages indicating that the generated resources are invalid.
This is mainly because the placeholders defined in the YAML fragments are not yet replaced with actual values.

To overcome this issue, we define a `dev` Maven profile that is active by default (unless another profile is specified).
This profile contains the required properties to replace the placeholders in the YAML fragments for this scenario.

```xml
<profiles>
  <profile>
    <id>dev</id>
    <activation>
      <activeByDefault>true</activeByDefault>
    </activation>
    <properties>
      <helm_namespace>default</helm_namespace>
      <!-- ... -->
    </properties>
  </profile>
</profiles>
```

With these properties configured, invoking `k8s:resource` won't log any warnings and the generated resources will be valid.

If you are running Minikube, you can test this by running the following commands:

```shell
eval $(minikube docker-env)
mvn clean package k8s:build k8s:resource k8s:apply
```

This should deploy the application with a Kubernetes Service exposed as a NodePort.

## Production environment with Helm charts (Outer Loop) 

<a id="production-environment-with-helm-charts-(outer-loop)"></a>

For the production environment, we want to generate Helm charts instead of raw Kubernetes resources.
This way, our application can be deployed to a Kubernetes cluster using Helm.

To be able to configure the Helm chart template placeholders, we need to define a `prod` Maven profile.
This profile contains the configured Helm parameters that will be used to interpolate the Helm chart templates and generate the `values.yaml` file.

In addition, some of the default values are defined using a `values.helm.yaml` file that you can find in the `src/main/jkube` directory. The values in this file will be merged with those defined in the `pom.xml` file.

In order to generate the Helm chart, we need to invoke the `k8s:helm` goal.

```shell
mvn -Pprod clean package k8s:build k8s:resource k8s:helm
```

The resulting Helm chart will be located in the `target/jkube/helm/spring-boot-helm` directory.

If you're testing this in Minikube, you can deploy the Helm chart using the following command:

```shell
helm install spring-boot-helm target/jkube/helm/spring-boot-helm/kubernetes/
```

You can also override some of the values defined in the `values.yaml` file using the `--set` flag:

```shell
helm install spring-boot-helm target/jkube/helm/spring-boot-helm/kubernetes/ --set deployment.replicas=5 --set service.spec.type=NodePort
```

## Customizing Chart via Maven properties

Helm chart properties can be overridden through command line:
```shell script
mvn k8s:resource k8s:helm -Pprod -Djkube.helm.chart="Custom Chart Name"  -Djkube.helm.home=https://custom.home
```

## Customizing the Chart using configuration

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
