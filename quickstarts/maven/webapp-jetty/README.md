---
name: "Maven :: Webapp Jetty"
description: |
  Java Web Application with a static index.html resource.
  Demonstrates how to create a container image with an embedded Eclipse Jetty server using Eclipse JKube.
  Jetty is used instead of Apache Tomcat because there is a Jetty specific configuration file (jetty-logging.properties).
  Eclipse JKube detects this file and chooses a Jetty specific base container image.
---