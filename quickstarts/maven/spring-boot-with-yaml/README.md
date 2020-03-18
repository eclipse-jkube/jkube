# Eclipse JKube Spring Boot with YAML Quickstart

This is a quickstart project to use Eclipse JKube plugin with customized yaml configurations.

## Requirements:

- JDK 8 or 11+
- Kubernetes Cluster (Minikube, OpenShift, CRC, etc.)

## Helm chart generation

To generate the helm chart with defaults run:
```shell script
mvn k8s:resource k8s:helm -Pkubernetes
```

### Customize Chart via Maven properties

Helm chart properties can be overridden through command line:
```shell script
mvn k8s:resource k8s:helm -Pkubernetes -Djkube.helm.chart="Custom Chart Name"  -Djkube.helm.home=https://custom.home
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