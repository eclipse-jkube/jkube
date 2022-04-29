# JKube Kit - Custom Enricher Using Eclipse JKube Enricher API in Gradle

This example demonstrates how you can extend Eclipse JKube Kit's Enricher API to make your own enricher and use it to enrich or generate manifests as per your requirements.

This is a basic application which would be showcasing how user can integrate its own enricher with Eclipse JKube
Kubernetes Gradle Plugin. It's a multi-module project which contains the following directories:

- `buildSrc`: This is not a module but a specific Gradle directory which is treated as a dependency for the rest of build
  scripts.
  It contains an `IstioEnricher` implementation which generates a dummy `networking.istio.io/v1alpha3` Gateway manifest
- `compile-time-enricher` : A module that provides an Enricher implementation that can be installed in the mavenLocal
  repository.
- `app`: A basic Spring Boot application which uses this enricher with Eclipse JKube.

# How to Build

You can build the project by executing:
```shell
./gradlew clean build
```

# How to Run

Once the project is built, you can generate the cluster manifests by executing the following command:
```shell
./gradlew k8sResource
```

If everything is successful, you should see the following output:
```
> Task :app:k8sResource
k8s: Running in Kubernetes mode
k8s: istio-enricher: Added dummy networking.istio.io/v1alpha3 Gateway
k8s: istio-enricher: Exiting Istio Enricher
k8s: compile-time-enricher: This is the compile-time-enricher running
```
It includes log messages from both enrichers: `compile-time-enricher` and `istio-enricher`.

The Istio Enricher should have also created a dummy `Gateway` manifest, you check it by running:
```shell
cat app/build/classes/java/main/META-INF/jkube/kubernetes/app-cr.yml 
```

The execution should output the file contents:
```yaml
---
apiVersion: networking.istio.io/v1alpha3
kind: Gateway
metadata:
  labels:
    app: app
    provider: jkube
    version: 0.0.1-SNAPSHOT
    group: org.eclipse.jkube.quickstarts.kit.enricher.istio
  name: app
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

## Adding the customized enricher

This project provides 2 options to declare your Enricher. Via a module or via a `buildSrc`.

You can provide the customized Enricher to the Kubernetes Gradle Plugin via the `buildSrc` directory approach.
In this case, following the suggested project structure nothing else is needed from your side.

In case you wanted to share this Enricher with other projects, you would need to publish the enricher as a Maven artifact
to a repository (at least you Maven local). This project also contains a module `compile-time-enricher` which can be
installed to a local Maven repository or consumed directly as a dependency:

### Adding the `compile-time-enricher` Enricher as Compile time dependency

This is the approach followed in this quickstart.
You can configure Eclipse JKube Kubernetes Gradle Plugin to look into project dependencies for potential enrichers.
This can be done via setting `useProjectClassPath` flag to `true`. You would also need to reference the dependent module:

```groovy
dependencies {
  // Enricher Dependency
  implementation project(':compile-time-enricher')
}

kubernetes {
  // Also look into project class path for enrichers/generators
  useProjectClassPath = true
  // ...
}
```

### Adding the `compile-time-enricher` Enricher from Maven

This is the approach you would follow if you wanted to share the Enrich

You need to first install the Module or publish it to a Maven repo:
```shell
./gradlew publishToMavenLocal
```

Then, you need to declare the dependency in the `buildscript` section:
```groovy
buildscript {
    repositories {
        mavenLocal()
    }
    dependencies {
        classpath('org.eclipse.jkube.quickstarts.kit.enricher.istio:istio-enricher:0.0.0-SNAPSHOT')
    }
}
```

