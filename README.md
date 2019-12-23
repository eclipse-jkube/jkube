# Jkube

[![Circle CI](https://circleci.com/gh/eclipse/jkube/tree/master.svg?style=shield)](https://circleci.com/gh/eclipse/jkube/tree/master)
[![License](https://img.shields.io/badge/License-EPL%202.0-red.svg?label=license&logo=eclipse)](https://www.eclipse.org/legal/epl-2.0/)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=jkubeio_jkube&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=jkubeio_jkube)
[![Gitter](https://badges.gitter.im/eclipse/jkube.svg)](https://gitter.im/eclipse/jkube?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=jkubeio_jkube&metric=coverage)](https://sonarcloud.io/dashboard?id=jkubeio_jkube)

This project contains various building blocks for the jkube developer toolbox.

Actually it contains the following abstractions which has been extracted from both projects:

* **Kubernetes Maven Plugin** <br/>
[![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.jkube/k8s-maven-plugin.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.eclipse.jkube%22%20AND%20a:%22k8s-maven-plugin%22)
![Sample Demo](kubernetes-maven-plugin/k8s-maven-plugin-demo.gif)

* **Openshift Maven Plugin** <br/> 
[![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.jkube/oc-maven-plugin.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.eclipse.jkube%22%20AND%20a:%22oc-maven-plugin%22)
![Sample Demo](openshift-maven-plugin/oc-maven-plugin-demo.gif)

* **Jkube-kit**, which consists of the following:

  * **Generator** framework for automatically generating Docker images by examining project information.<br />
  [![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.jkube/jkube-maven-generator-api.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.eclipse.jkube%22%20AND%20a:%22jkube-maven-generator-api%22) [![Javadocs](http://www.javadoc.io/badge/org.eclipse.jkube/jkube-maven-generator-api.svg?color=blue)](http://www.javadoc.io/doc/org.eclipse.jkube/jkube-maven-generator-api)
  * **Enricher** framework for creating and enhancing Kubernetes and OpenShift resources.<br />
  [![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.jkube/jkube-maven-enricher-api.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.eclipse.jkube%22%20AND%20a:%22jkube-maven-enricher-api%22) [![Javadocs](http://www.javadoc.io/badge/org.eclipse.jkube/jkube-maven-enricher-api.svg?color=blue)](http://www.javadoc.io/doc/org.eclipse.jkube/jkube-maven-enricher-api)
  * **Profile** combining the configuration for generators and enrichers.<br />
  [![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.jkube/jkube-maven-profiles.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.eclipse.jkube%22%20AND%20a:%22jkube-maven-profiles%22) [![Javadocs](http://www.javadoc.io/badge/org.eclipse.jkube/jkube-maven-profiles.svg?color=blue)](http://www.javadoc.io/doc/org.eclipse.jkube/jkube-maven-profiles)
  * **Resource configuration** model objects for a simplified configuration of Kubernetes and OpenShift resources.<br />
  [![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.jkube/jkube-kit-config-resource.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.eclipse.jkube%22%20AND%20a:%22jkube-kit-config-resource%22) [![Javadocs](http://www.javadoc.io/badge/org.eclipse.jkube/jkube-kit-config-resource.svg?color=blue)](http://www.javadoc.io/doc/org.eclipse.jkube/jkube-kit-config-resource)
  * **Image configuration** model objects for modeling Docker image configuration as used in docker-maven-plugin.<br />
  [![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.jkube/jkube-kit-config-image.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.eclipse.jkube%22%20AND%20a:%22jkube-kit-config-image%22) [![Javadocs](http://www.javadoc.io/badge/org.eclipse.jkube/jkube-kit-config-image.svg?color=blue)](http://www.javadoc.io/doc/org.eclipse.jkube/jkube-kit-config-image)

One intention of extracting these parts from the originating plugins is also to separate Maven related and non-Maven related functionality so that the non-Maven parts can be reused for other build systems and IDEs like Gradle or Eclipse. Some thin adapter Maven specific modules like [jkube-kit-enricher-maven](enricher/maven/pom.xml)  and [jkube-kit-generator-maven](generator/maven/pom.xml) are provided as glue to get to the Maven specific build information like the project's coordinates.


By moving out common parts it will be now also be possible for the [docker-maven-plugin](https://github.com/fabric8io/docker-maven-plugin) to benefit from the generator framework for zero-config creation of Docker images.


<div style="text-align:center"><img src ="https://i.imgur.com/DF5bnD2.jpg" /></div>
