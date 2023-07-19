---
name: "Maven :: Karaf Camel 2 Log"
description: |
  Simple Apache Camel 2 application on top of Apache Karaf that logs a series of messages to the Server log.
  The application also generates and processes random orders using Camel routes.
---
# Eclipse JKube Karaf Quickstart - Camel Log

This quickstart shows a simple Apache Camel 2 application that logs a series of messages to the Server log and
generates and processes random orders.

## Features

### Camel Log

A Camel route defined in [camel-log.xml](src/main/resources/OSGI-INF/blueprint/camel-log.xml) (`<route id="log-route">`)
will log some greeting messages to the Server log every second.

The format and contents of the log messages is controlled in [org.ops4j.pax.logging.cfg
](src/main/resources/assembly/etc/org.ops4j.pax.logging.cfg) (`log4j2.appender.hello.name=hello-stdout`).


### Order generation and processing

Two Camel route defined in [camel-log.xml](src/main/resources/OSGI-INF/blueprint/camel-log.xml)
(`<route id="generate-order">` & `<route id="process-order">`),  generates and processes some fake orders
every 5 seconds.
- Orders will be generated in `${KARAF_HOME}/work/orders/input`
- Orders will be processed and moved to `${KARAF_HOME}/work/orders/processed`
- Both actions will be logged to the Server output using a different logger.


## Building

The quickstart can be built with `mvn clean install`.

This will generate an Apache Karaf assembly ready to be deployed or run locally ( `./target/assembly/bin/karaf run`).

## Deploying to Kubernetes

Make sure you have access to a K8s cluster.

Deploy the application:
```
mvn k8s:build k8s:resource k8s:apply
``` 

Once the process is finished, you can retrieve the logs with `mvn k8s:log`. This should print the log messages 
from the previously described Camel routes.

## Deploying to OpenShift

Make sure you have access to an OpenShift cluster.

Deploy the application:
```
mvn oc:build oc:resource oc:apply
``` 

Once the process is finished, you can retrieve the logs with `mvn oc:log`. This should print the log messages 
from the previously described Camel routes.
