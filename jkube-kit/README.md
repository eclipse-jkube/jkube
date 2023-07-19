## JKube-Kit

JKube Kit is the core engine powering our Maven plugins.

The main difference between Fabric8 Maven Plugin and JKube is the extraction and decoupling of the core components from
Maven. This enables other projects and frameworks to reuse all of JKube's functionality by exposing it through a public API.

Some thin adapter Maven specific modules like [jkube-kit-enricher-maven](enricher/maven/pom.xml) and
[jkube-kit-generator-maven](generator/maven/pom.xml) are provided as glue to get to the Maven specific build information
like the project's coordinates.
This project contains various building blocks for the JKube developer toolbox.

Actually it contains the following abstractions which has been extracted from both projects:

* **Generator** framework for automatically generating Docker images by examining project information.<br />
  [![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.jkube/jkube-kit-generator-api.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.eclipse.jkube%22%20AND%20a:%22jkube-kit-generator-api%22) [![Javadocs](http://www.javadoc.io/badge/org.eclipse.jkube/jkube-kit-generator-api.svg?color=blue)](http://www.javadoc.io/doc/org.eclipse.jkube/jkube-kit-generator-api)
* **Enricher** framework for creating and enhancing Kubernetes and OpenShift resources.<br />
  [![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.jkube/jkube-kit-enricher-api.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.eclipse.jkube%22%20AND%20a:%22jkube-kit-enricher-api%22) [![Javadocs](http://www.javadoc.io/badge/org.eclipse.jkube/jkube-kit-enricher-api.svg?color=blue)](http://www.javadoc.io/doc/org.eclipse.jkube/jkube-maven-enricher-api)
* **Profile** combining the configuration for generators and enrichers.<br />
  [![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.jkube/jkube-kit-profiles.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.eclipse.jkube%22%20AND%20a:%22jkube-kit-profiles%22) [![Javadocs](http://www.javadoc.io/badge/org.eclipse.jkube/jkube-kit-profiles.svg?color=blue)](http://www.javadoc.io/doc/org.eclipse.jkube/jkube-kit-profiles)
* **Resource configuration** model objects for a simplified configuration of Kubernetes and OpenShift resources.<br />
  [![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.jkube/jkube-kit-config-resource.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.eclipse.jkube%22%20AND%20a:%22jkube-kit-config-resource%22) [![Javadocs](http://www.javadoc.io/badge/org.eclipse.jkube/jkube-kit-config-resource.svg?color=blue)](http://www.javadoc.io/doc/org.eclipse.jkube/jkube-kit-config-resource)
* **Image configuration** model objects for modeling Docker image configuration as used in docker-maven-plugin.<br />
  [![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.jkube/jkube-kit-config-image.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.eclipse.jkube%22%20AND%20a:%22jkube-kit-config-image%22) [![Javadocs](http://www.javadoc.io/badge/org.eclipse.jkube/jkube-kit-config-image.svg?color=blue)](http://www.javadoc.io/doc/org.eclipse.jkube/jkube-kit-config-image)

One intention of extracting these parts from the originating plugins is also to separate Maven related and non-Maven related functionality so that the non-Maven parts can be reused for other build systems and IDEs like Gradle or Eclipse. Some thin adapter Maven specific modules like [jkube-kit-enricher-maven](enricher/maven/pom.xml)  and [jkube-kit-generator-maven](generator/maven/pom.xml) are provided as glue to get to the Maven specific build information like the project's coordinates.


By moving out common parts it will be now also be possible for the [docker-maven-plugin](https://github.com/fabric8io/docker-maven-plugin) to benefit from the generator framework for zero-config creation of Docker images.


<div style="text-align:center"><img src ="https://i.imgur.com/ImV4BiR.png" /></div>

