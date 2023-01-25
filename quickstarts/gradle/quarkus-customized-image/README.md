---
name: "Gradle :: Quarkus Customized Image"
description: |
  Quarkus application with a single JAX-RS endpoint.
  Demonstrates how to build a Quarkus container image based on a Red Hat container image private registry.
  Uses a pull secret from Red Hat Registry Service Accounts to authenticate.
---
# Eclipse JKube Quarkus with Customized Image Quickstart

A simple REST application demonstrating usage of Eclipse JKube with Quarkus
customized to use an official
[Red Hat Container Image](https://catalog.redhat.com/software/containers/search).

## Requirements:

- JDK 8 or 11+
- OpenShift Cluster (OpenShift, CRC, etc.)
- Registered pull secret in your cluster ([Registry Service Accounts](https://access.redhat.com/terms-based-registry/#/accounts))


## How to run

**Note:**
> To be able to retrieve the image from the catalog you'll have to modify the
configuration field `openshiftPullSecret = '12819530-ocp42-exposed-env-pull-secret-pull-secret'`
`openshift` section in your `build.gradle`.

```shell script
$ gradle clean build ocBuild
```

