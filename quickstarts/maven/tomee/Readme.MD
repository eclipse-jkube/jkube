---
name: "Maven :: Tomee"
description: |
  Example of using [Apache Tomee](https://tomee.apache.com)
---
# Eclipse JKube Apache Tomee Quickstart

This example shows how to integrate Eclipse JKube in an Apache Tomee application

The result is a deployed Tomee microprofile instance which simply presents Hello World on request.
More advanced Tomee configurations can be done inline according to the Tomee Maven plugin, and overrides as shown in the repository

## Requirements

- JDK 11
- Kubernetes Cluster, with LetsEncrypt support
  - kubectl logged in to cluster
- Docker repository `$docker.host`
  - docker login to repository done on command line.
- DNS entry for your hostname on the Kubernetes Cluster

## Building and deploying the application


```shell script
$ mvn clean package k8s:deploy
```

## Expected output

Once you've deployed the application _(please allow 1 minute for the application to start)_, you should be able to go `https://$proj.hostname` and be greeted with:

```
Hello World
```