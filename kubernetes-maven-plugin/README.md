## Kubernetes Maven Plugin

[![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.jkube/kubernetes-maven-plugin.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.eclipse.jkube%22%20AND%20a:%22kubernetes-maven-plugin%22)

[![Sample Demo](kmp.png)](https://asciinema.org/a/335724)

### Introduction
This Maven plugin is a one-stop-shop for building and deploying Java applications for Docker, Kubernetes. It brings your Java applications on to Kubernetes. It provides a tight integration into maven and benefits from the build configuration already provided. It focuses on three tasks:
+ Building Docker images
+ Creating Kubernetes resources
+ Deploy applications

### Usage
To enable kubernetes maven plugin on your project just add this to the plugins sections of your pom.xml:

```
      <plugin>
        <groupId>org.eclipse.jkube</groupId>
        <artifactId>kubernetes-maven-plugin</artifactId>
        <version>${jkube.kubernetes.version}</version>
      </plugin>
```

| Goal                                                                                            | Description                                                                                    |
|-------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| [`k8s:resource`](https://www.eclipse.dev/jkube/docs/kubernetes-maven-plugin#jkube:resource)     | Create Kubernetes resource descriptors                                                         |
| [`k8s:build`](https://www.eclipse.dev/jkube/docs/kubernetes-maven-plugin#jkube:build)           | Build Docker images                                                                            |
| [`k8s:push`](https://www.eclipse.dev/jkube/docs/kubernetes-maven-plugin#jkube:push)             | Push Docker images to a registry                                                               |
| [`k8s:deploy`](https://www.eclipse.dev/jkube/docs/kubernetes-maven-plugin#jkube:deploy)         | Deploy Kubernetes resource objects to a cluster                                                |
| [`k8s:helm`](https://www.eclipse.dev/jkube/docs/kubernetes-maven-plugin#jkube:helm)             | Generate Helm charts for your application                                                      |
| [`k8s:helm-push`](https://www.eclipse.dev/jkube/docs/kubernetes-maven-plugin#jkube:helm-push)   | Push generated Helm Charts to remote repository                                                |
| [`k8s:remote-dev`](https://www.eclipse.dev/jkube/docs/kubernetes-maven-plugin#jkube:remote-dev) | Run and debug code in your local machine while connected to services available in your cluster |
| [`k8s:watch`](https://www.eclipse.dev/jkube/docs/kubernetes-maven-plugin#jkube:watch)           | Watch for doing rebuilds and restarts                                                          |

### Features

* Dealing with Docker images and hence inherits its flexible and powerful configuration.
* Supports Kubernetes descriptors
* Various configuration styles:
  * **Zero Configuration** for a quick ramp-up where opinionated defaults will be pre-selected.
  * **Inline Configuration** within the plugin configuration in an XML syntax.
  * **External Configuration** templates of the real deployment descriptors which are enriched by the plugin.
* Flexible customization:
  * **Generators** analyze the Maven build and generated automatic Docker image configurations for certain systems (spring-boot, plain java, karaf ...)
  * **Enrichers** extend the Kubernetes resource descriptors by extra information like SCM labels and can add default objects like Services.
  * Generators and Enrichers can be individually configured and combined into *profiles*
