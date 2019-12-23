# Eclipse Jkube

Eclipse Jkube is a collection of plugins and libraries that are used for generating and deploying Kubernetes/Openshift manifests at compile
time. It brings your Java applications on to Kubernetes and OpenShift. It provides a tight integration into Maven and benefits from 
the build configuration already provided. This project focus on two tasks: Building Docker images and creating Kubernetes and 
OpenShift resource descriptors.

## Features
 - **Jkube-kit**
   - **Generator** framework for automatically generating Docker images by examining project information.
   - **Enricher** framework for creating and enhancing Kubernetes/Openshift resource descriptors.
   - **Profile** combining configuration for generators and enrichers.
   - **Resource Configuration** model objects for a simplified configuration of Kubernetes/Openshift resource.
   - **Image Configuration** model objects for modeling Docker image configuration.

 - **Kubernetes Maven Plugin**
   - Generates docker images with flexible and powerful configuration.
   - Supports generating Kubernetes descriptors
   - Provides **Zero Configuration** for a quick ramp-up where opinionated defaults will be pre-selected.
   - Provides **Inline Configuration** within the plugin configuration in an XML syntax.
   - Provides **External Configuration** templates of real deployment descriptors which are enriched by plugin.
 
 - **Openshift Maven Plugin**
   - Dealing with S2I images and hence inherits its flexible and powerful configuration
   - Supports generating Openshift descriptors
   - Provides **Zero Configuration** for a quick ramp-up where opinionated defaults will be pre-selected.
   - Provides **Inline Configuration** within the plugin configuration in an XML syntax.
   - Provides **External Configuration** templates of real deployment descriptors which are enriched by plugin.
 
## Getting Started
 - Check out our asciicasts for:
   - [Kubernetes Maven Plugin](https://asciinema.org/a/253747)
      ![Sample Demo](https://raw.githubusercontent.com/eclipse/jkube/kubernetes-maven-plugin/master/k8s-maven-plugin-demo.gif)
   - [Openshift Maven Plugin](https://asciinema.org/a/253742)
      ![Sample Demo](https://raw.githubusercontent.com/eclipse/jkube/openshift-maven-plugin/master/oc-maven-plugin-demo.gif)

 - Visit our [quickstarts samples](https://github.com/eclipse/quickstarts) on github.

## Documentation
 - Check out our documentation for for:
   - [Kubernetes Maven Plugin](./kubernetes-maven-plugin/doc/index.html)
   - [Openshift Maven Plugin](./openshift-maven-plugin/doc/index.html)


## Getting Involved
  - Follow us on [Twitter](https://twitter.com/jkubeio)
  - Contribute via bug fixes or issues on [Github](https://github.com/eclipse/jkube)
  - Our mailing list: jkube-dev@eclipse.org
  - Reach out to us on [Gitter](https://gitter.im/eclipse/jkube#)

