## OpenShift Maven Plugin

[![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.jkube/openshift-maven-plugin.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.eclipse.jkube%22%20AND%20a:%22openshift-maven-plugin%22)

[![Sample Demo](omp.png)](https://asciinema.org/a/335743)

### Introduction
This Maven plugin is a one-stop-shop for building and deploying Java applications for OpenShift. It brings your Java applications on to OpenShift. It provides a tight integration into maven and benefits from the build configuration already provided. It focuses on three tasks:
+ Building S2I images
+ Creating OpenShift resources
+ Deploy application on OpenShift

### Usage
To enable OpenShift maven plugin on your project just add this to the plugins sections of your pom.xml:

```
      <plugin>
        <groupId>org.eclipse.jkube</groupId>
        <artifactId>oc-maven-plugin</artifactId>
        <version>${jkube.openshift.version}</version>
      </plugin>
```

| Goal                                          | Description                           |
| --------------------------------------------- | ------------------------------------- |
| [`oc:resource`](https://www.eclipse.org/jkube/docs/openshift-maven-plugin#oc:resource) | Create OpenShift resource descriptors |
| [`oc:build`](https://www.eclipse.org/jkube/docs/openshift-maven-plugin#oc:build) | Build Docker images |
| [`oc:push`](https://www.eclipse.org/jkube/docs/openshift-maven-plugin#oc:push) | Push Docker images to a registry  |
| [`oc:deploy`](https://www.eclipse.org/jkube/docs/openshift-maven-plugin#oc:deploy) | Deploy OpenShift resource objects to a cluster  |
| [`oc:watch`](https://www.eclipse.org/jkube/docs/openshift-maven-plugin#oc:watch) | Watch for doing rebuilds and restarts |

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

