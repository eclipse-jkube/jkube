# JKube Kit - Custom Enricher Using Eclipse JKube Enricher API

This example demonstrates how you can extend Eclipse JKube Kit's Enricher API to make your own enricher and use it to enrich or generate manifests as per your requirements. This is a multi module project which contains these modules:

- istio-enricher : A basic IstioEnricher which generates a dummy `networking.istio.io/v1alpha3` Gateway manifest
- app : A basic spring boot application which uses this enricher with Eclipse JKube

# How to Build:
Just need to run:
```bash
mvn clean install
```

# How to Run:
This project demonstrates use of Custom Enricher. You can check `pom.xml` of `app/` project to see how Custom Enricher is integrated into it. When you would run resource goal, you should be able to see enricher in action:

```
custom-istio-enricher : $ cd app/
app : $ mvn k8s:resource
[INFO] Scanning for projects...
[INFO] 
[INFO] --< org.eclipse.jkube.quickstarts.kit:eclipse-jkube-sample-custom-enricher-app >--
[INFO] Building Eclipse JKube :: Sample :: Custom Enricher :: App 1.1.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- kubernetes-maven-plugin:1.1.0-SNAPSHOT:resource (default-cli) @ eclipse-jkube-sample-custom-enricher-app ---
[WARNING] k8s: Cannot access cluster for detecting mode: No route to host (Host unreachable)
[INFO] k8s: Running generator spring-boot
[INFO] k8s: spring-boot: Using Docker image quay.io/jkube/jkube-java-binary-s2i:0.0.9 as base / builder
[INFO] k8s: Using resource templates from /home/rohaan/work/repos/jkube/quickstarts/kit/custom-istio-enricher/app/src/main/jkube
[INFO] k8s: jkube-service: Adding a default service 'eclipse-jkube-sample-custom-enricher-app' with ports [8080]
[INFO] k8s: jkube-revision-history: Adding revision history limit to 2
[INFO] k8s: istio-enricher: Added dummy networking.istio.io/v1alpha3 Gateway
[INFO] k8s: istio-enricher: Exiting Istio Enricher
[INFO] k8s: validating /home/rohaan/work/repos/jkube/quickstarts/kit/custom-istio-enricher/app/target/classes/META-INF/jkube/kubernetes/eclipse-jkube-sample-custom-enricher-app.yml resource
[WARNING] k8s: Failed to validate resources: null
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  6.108 s
[INFO] Finished at: 2020-10-30T22:25:51+05:30
[INFO] ------------------------------------------------------------------------
```
After running resource goal, you should be able to see a dummy `Gateway` manifest in target directory:
```
app : $ cat target/classes/META-INF/jkube/kubernetes/eclipse-jkube-sample-custom-enricher-app.yml 
---
apiVersion: networking.istio.io/v1alpha3
kind: Gateway
metadata:
  labels:
    app: eclipse-jkube-sample-custom-enricher-app
    provider: jkube
    version: 1.1.0-SNAPSHOT
    group: org.eclipse.jkube.quickstarts.kit
  name: eclipse-jkube-sample-custom-enricher-app
spec:
  selector:
    app: test-app
  servers:
  - hosts:
    - uk.bookinfo.com
    - in.bookinfo.com
    port:
      name: http
      number: 80
      protocol: HTTP
    tls:
      httpsRedirect: true

```
