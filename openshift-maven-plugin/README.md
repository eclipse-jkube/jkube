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
        <artifactId>openshift-maven-plugin</artifactId>
        <version>${jkube.openshift.version}</version>
      </plugin>
```

| Goal                                                                                          | Description                                                                                    |
|-----------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| [`oc:resource`](https://www.eclipse.dev/jkube/docs/openshift-maven-plugin#jkube:resource)     | Create Kubernetes resource descriptors                                                         |
| [`oc:build`](https://www.eclipse.dev/jkube/docs/openshift-maven-plugin#jkube:build)           | Build Docker images                                                                            |
| [`oc:push`](https://www.eclipse.dev/jkube/docs/openshift-maven-plugin#jkube:push)             | Push Docker images to a registry                                                               |
| [`oc:deploy`](https://www.eclipse.dev/jkube/docs/openshift-maven-plugin#jkube:deploy)         | Deploy Kubernetes resource objects to a cluster                                                |
| [`oc:helm`](https://www.eclipse.dev/jkube/docs/openshift-maven-plugin#jkube:helm)             | Generate Helm charts for your application                                                      |
| [`oc:helm-push`](https://www.eclipse.dev/jkube/docs/openshift-maven-plugin#jkube:helm-push)   | Push generated Helm Charts to remote repository                                                |
| [`oc:remote-dev`](https://www.eclipse.dev/jkube/docs/openshift-maven-plugin#jkube:remote-dev) | Run and debug code in your local machine while connected to services available in your cluster |
| [`oc:watch`](https://www.eclipse.dev/jkube/docs/openshift-maven-plugin#jkube:watch)           | Watch for doing rebuilds and restarts                                                          |


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

