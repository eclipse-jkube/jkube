---
name: "Maven :: Uber Jar"
description: |
  Demo project for getting started with Eclipse JKube and packaging the result in a uber-jar.
  It runs a picocli application that would greet the user.
---
# JKube Uber Jar Sample

This is a demo project for getting started with Eclipse JKube. It runs a picocli application that would
greet the user. We would be using Eclipse JKube for building a docker image and deploying to Kubernetes
in single command.

1. Make sure you've minikube up and running.
2. Run the following command to run uber-jar sample: 
```
$ mvn clean install k8s:build k8s:resource k8s:apply
``` 

3. Check logs of Created Pod:
```
$ kubectl get pods
NAME                                       READY   STATUS        RESTARTS   AGE
uberjar-759d5f579-b662s                    0/1     Completed     2          110s
$ mvn k8s:log
[INFO] k8s:  [NEW] uberjar-cfdcbc5d9-4l2bk status: Running
[INFO] k8s:  [NEW] Tailing log of pod: uberjar-cfdcbc5d9-4l2bk
[INFO] k8s:  [NEW] Press Ctrl-C to stop tailing the log
[INFO] k8s:  [NEW]
Starting the Java application using /opt/jboss/container/java/run/run-java.sh ...
INFO exec  java -javaagent:/usr/share/java/jolokia-jvm-agent/jolokia-jvm.jar=config=/opt/jboss/container/jolokia/etc/jolokia.properties -javaagent:/usr/share/java/prometheus-jmx-exporter/jmx_prometheus_javaagent.jar=9779:/opt/jboss/container/prometheus/etc/jmx-exporter-config.yaml -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20 -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -XX:+ExitOnOutOfMemoryError -cp "." -jar /deployments/uberjar-1.13.1.jar  
Hello picocli, go go commando!
```
