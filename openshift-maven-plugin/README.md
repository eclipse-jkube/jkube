## Openshift Maven Plugin

[![Maven Central](https://img.shields.io/maven-central/v/io.jkube/oc-maven-plugin.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.jkube%22%20AND%20a:%22oc-maven-plugin%22)

![Sample Demo](oc-maven-plugin-demo.gif)

### Introduction
This Maven plugin is a one-stop-shop for building and deploying Java applications for OpenShift. It brings your Java applications on to OpenShift. It provides a tight integration into maven and benefits from the build configuration already provided. It focuses on three tasks:
+ Building S2I images
+ Creating OpenShift resources
+ Deploy application on OpenShift

### Usage
To enable Openshift maven plugin on your project just add this to the plugins sections of your pom.xml:

```
      <plugin>
        <groupId>io.jkube</groupId>
        <artifactId>oc-maven-plugin</artifactId>
        <version>${jkube.openshift.version}</version>
      </plugin>
```

| Goal                                          | Description                           |
| --------------------------------------------- | ------------------------------------- |
| [`oc:resource`](https://fabric8io.github.io/fabric8-maven-plugin/#fabric8:resource) | Create OpenShift resource descriptors |
| [`oc:build`](https://fabric8io.github.io/fabric8-maven-plugin/#fabric8:build) | Build Docker images |
| [`oc:push`](https://fabric8io.github.io/fabric8-maven-plugin/#fabric8:push) | Push Docker images to a registry  |
| [`oc:deploy`](https://fabric8io.github.io/fabric8-maven-plugin/#fabric8:deploy) | Deploy OpenShift resource objects to a cluster  |
| [`oc:watch`](https://fabric8io.github.io/fabric8-maven-plugin/#fabric8:watch) | Watch for doing rebuilds and restarts |

### Features

* Dealing with S2I images and hence inherits its flexible and powerful configuration.
* Supports both OpenShift descriptors
* OpenShift Docker builds with a binary source (as an alternative to a direct image build against a Docker daemon)
* Various configuration styles:
  * **Zero Configuration** for a quick ramp-up where opinionated defaults will be pre-selected.
  * **Inline Configuration** within the plugin configuration in an XML syntax.
  * **External Configuration** templates of the real deployment descriptors which are enriched by the plugin.
* Flexible customization:
  * **Generators** analyze the Maven build and generated automatic Docker image configurations for certain systems (spring-boot, plain java, karaf ...)
  * **Enrichers** extend the  OpenShift resource descriptors by extra information like SCM labels and can add default objects like Services.
  * Generators and Enrichers can be individually configured and combined into *profiles*

