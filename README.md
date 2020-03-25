# JKube

[![Circle CI](https://circleci.com/gh/eclipse/jkube/tree/master.svg?style=shield)](https://circleci.com/gh/eclipse/jkube/tree/master)
[![Integration Tests](https://github.com/eclipse/jkube/workflows/Integration%20Tests/badge.svg?branch=master)](https://github.com/eclipse/jkube/actions?query=branch%3Amaster)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=jkubeio_jkube&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=jkubeio_jkube)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=jkubeio_jkube&metric=coverage)](https://sonarcloud.io/dashboard?id=jkubeio_jkube)
[![Gitter](https://badges.gitter.im/eclipse/jkube.svg)](https://gitter.im/eclipse/jkube?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Twitter](https://img.shields.io/twitter/follow/jkubeio?style=social)](https://twitter.com/jkubeio)

<p align="center">
  <a href="https://www.eclipse.org/jkube/">
  	<img src="https://i.imgur.com/EWL66xC.png" width="350" alt="Eclipse JKube"/>
  </a>
</p>

This project contains various building blocks for the JKube developer toolbox.

Actually it contains the following abstractions which has been extracted from both projects:

* **Kubernetes Maven Plugin** <br/>
[![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.jkube/k8s-maven-plugin.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.eclipse.jkube%22%20AND%20a:%22k8s-maven-plugin%22)
[![Documentation](https://img.shields.io/badge/plugin-documentation-lightgrey)](https://www.eclipse.org/jkube/docs/kubernetes-maven-plugin)
![Sample Demo](kubernetes-maven-plugin/k8s-maven-plugin-demo.gif)

* **OpenShift Maven Plugin** <br/> 
[![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.jkube/oc-maven-plugin.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.eclipse.jkube%22%20AND%20a:%22oc-maven-plugin%22)
[![Documentation](https://img.shields.io/badge/plugin-documentation-lightgrey)](https://www.eclipse.org/jkube/docs/openshift-maven-plugin)
![Sample Demo](openshift-maven-plugin/oc-maven-plugin-demo.gif)

* **JKube-kit**, which consists of the following:

  * **Generator** framework for automatically generating Docker images by examining project information.<br />
  [![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.jkube/jkube-kit-generator-api.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.eclipse.jkube%22%20AND%20a:%22jkube-kit-generator-api%22) [![Javadocs](http://www.javadoc.io/badge/org.eclipse.jkube/jkube-kit-generator-api.svg?color=blue)](http://www.javadoc.io/doc/org.eclipse.jkube/jkube-kit-generator-api)
  * **Enricher** framework for creating and enhancing Kubernetes and OpenShift resources.<br />
  [![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.jkube/jkube-kit-enricher-api.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.eclipse.jkube%22%20AND%20a:%22jkube-kit-enricher-api%22) [![Javadocs](http://www.javadoc.io/badge/org.eclipse.jkube/jkube-kit-enricher-api.svg?color=blue)](http://www.javadoc.io/doc/org.eclipse.jkube/jkube-kit-enricher-api)
  * **Profile** combining the configuration for generators and enrichers.<br />
  [![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.jkube/jkube-kit-profiles.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.eclipse.jkube%22%20AND%20a:%22jkube-kit-profiles%22) [![Javadocs](http://www.javadoc.io/badge/org.eclipse.jkube/jkube-kit-profiles.svg?color=blue)](http://www.javadoc.io/doc/org.eclipse.jkube/jkube-kit-profiles)
  * **Resource configuration** model objects for a simplified configuration of Kubernetes and OpenShift resources.<br />
  [![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.jkube/jkube-kit-config-resource.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.eclipse.jkube%22%20AND%20a:%22jkube-kit-config-resource%22) [![Javadocs](http://www.javadoc.io/badge/org.eclipse.jkube/jkube-kit-config-resource.svg?color=blue)](http://www.javadoc.io/doc/org.eclipse.jkube/jkube-kit-config-resource)
  * **Image configuration** model objects for modeling Docker image configuration as used in docker-maven-plugin.<br />
  [![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.jkube/jkube-kit-config-image.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.eclipse.jkube%22%20AND%20a:%22jkube-kit-config-image%22) [![Javadocs](http://www.javadoc.io/badge/org.eclipse.jkube/jkube-kit-config-image.svg?color=blue)](http://www.javadoc.io/doc/org.eclipse.jkube/jkube-kit-config-image)

One intention of extracting these parts from the originating plugins is also to separate Maven related and non-Maven related functionality so that the non-Maven parts can be reused for other build systems and IDEs like Gradle or Eclipse. Some thin adapter Maven specific modules like [jkube-kit-enricher-maven](enricher/maven/pom.xml)  and [jkube-kit-generator-maven](generator/maven/pom.xml) are provided as glue to get to the Maven specific build information like the project's coordinates.


By moving out common parts it will be now also be possible for the [docker-maven-plugin](https://github.com/fabric8io/docker-maven-plugin) to benefit from the generator framework for zero-config creation of Docker images.


<div style="text-align:center"><img src ="https://i.imgur.com/DF5bnD2.jpg" /></div>

## Hello World using Eclipse JKube

- Clone repository and move to quickstart [helloworld](https://github.com/eclipse/jkube/tree/master/quickstarts/maven/hello-world) sample, build project and run JKube goals:
```
# 1. Clone repository and move to Hello World Quickstart
git clone git@github.com:eclipse/jkube.git && cd jkube/quickstarts/maven/hello-world

# 2. Build Project and run JKube goals
mvn clean install \
  k8s:build       \ # Build Docker Image
  k8s:resource     \ # Generate Kubernetes Manifests
  k8s:apply         # Apply generated Kubernetes Manifests onto Kubernetes
```
- Check created pod logs:
```
~/work/repos/jkube/quickstarts/maven/hello-world : $ kubectl get pods
NAME                                       READY   STATUS        RESTARTS   AGE
jkube-sample-helloworld-7c4665f464-xwskj   0/1     Completed     2          27s
~/work/repos/jkube/quickstarts/maven/hello-world : $ kubectl logs jkube-sample-helloworld-7c4665f464-xwskj
Hello World!
```
