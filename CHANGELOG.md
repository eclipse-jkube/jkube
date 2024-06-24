# Changes

This document main purpose is to list changes which might affect backwards compatibility. It will not list all releases as Eclipse JKube is build in a continuous delivery fashion.

We use semantic versioning in some slight variation until our feature set has stabilized and the missing pieces has been filled in:

* The `MAJOR_VERSION` is kept to `0`
* The `MINOR_VERSION` changes when there is an API or configuration change which is not fully backward compatible.
* The `PATCH_LEVEL` is used for regular CD releases which add new features and bug fixes.

After this we will switch probably to real [Semantic Versioning 2.0.0](http://semver.org/)

## Extracting changelog portions
We provide a script to extract changelog portions and automatic link building to send notifications
(i.e. e-mail) about new releases
([scripts/extract-changelog-for-version.sh](https://github.com/eclipse-jkube/jkube/blob/master/scripts/extract-changelog-for-version.sh))

Usage:
```
# ./scripts/changelog.sh semanticVersionNumber [linkLabelStartNumber]
./scripts/extract-changelog-for-version.sh 1.3.37 5
```
### 1.17-SNAPSHOT
* Fix #1989: Remove storageClass related fields from VolumePermissionEnricher
* Fix #3161: JavaExecGenerator should honor %t setting and not unconditionally add `latest` tag
* Fix #2098: Add support for multi-platform container image builds in jib build strategy
* Fix #2335: Add support for configuring nodeSelector spec for controller via xml/groovy DSL configuration
* Fix #2459: Allow configuring Buildpacks build via ImageConfiguration
* Fix #2462: `k8s:debug` throws error when using `buildpacks` build strategy
* Fix #2463: Buildpacks should clear build cache when `nocache` option is enabled
* Fix #2470: Add configuration option for overriding buildpack builder image
* Fix #2662: Sanitize VCS remote URL used in `jkube.eclipse.org/git-url` annotation
* Fix #2665: Added support for explicit path for readiness and liveness probes in SpringBootHealthCheckEnricher
* Fix #2860: Correctly pass Docker build-arg from the build configuration to the Openshift build strategy
* Fix #2885: Provide a way to set labels on images defined by Generators
* Fix #2901: Ensure Docker build arguments from properties are used during images pre-pulling
* Fix #2904: `docker.buildArg.*` properties not taken into account in OpenShift plugins
* Fix #3007: Kubernetes Maven Plugin generating resource manifests with line feeds on Windows
* Fix #3067: Helm Push uses configured docker global and push registries instead of pull 
* Fix #2110: Add new helm dependency update goal task (`k8s:helm-dependency-update` for maven and `k8sHelmDependencyUpdate` for gradle)
* Fix #3122: JKube should also pass project directory in `buildpacks` build strategy
* Fix #2467: Add support for specifying imagePullSecrets via resource configuration

_**Note**_:
- `defaultStorageClass` and `useStorageClassAnnotation` fields have been removed from VolumePermissionEnricher (`jkube-volume-permission`). Users are advised to use these fields from PersistentVolumeClaimStorageClassEnricher (`jkube-persistentvolumeclaim-storageclass`) instead.

### 1.16.2 (2024-03-27)
* Fix #2461: `k8s:watch`/`k8sWatch` should throw error in `buildpacks` build strategy
* Fix #2852: Bump version.kubernetes-client from 6.10.0 to 6.11.0
* Fix #2847: OpenShift Routes use `spec.port.targetPort` instead of `spec.port.port`

### 1.16.1 (2024-02-27)
* Fix #2582: Moved PortMapping class from package `org.eclipse.jkube.kit.build.api.model` to `org.eclipse.jkube.kit.common`
* Fix #2726: helm-lint works seamlessly on MacOS aarch64

### 1.16.0 (2024-02-09)
* Fix #1690: Base images based on ubi9
* Fix #1700: Add [Kubernetes Well Known labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/common-labels/) to generated resources
* Fix #2257: Provide guidance when the final project packaged file is not found in Quarkus projeicts
* Fix #2070: build goals/tasks log warning if user forgets to run package/build goal/task
* Fix #2314: Add chart name validation before doing Helm OCI push
* Fix #2381: Container Images based on Java 21 (Java-exec, Tomcat, Jetty, Karaf)
* Fix #2389: Helm `values.yaml` sorted alphabetically
* Fix #2390: support for all missing Chart.yaml fields
* Fix #2391: Automatically add `values.schema.json` file if detected
* Fix #2423: Change default VolumePermissionEnricher's initContainer image from `busybox` to `quay.io/quay/busybox`
* Fix #2444: Add support for Spring Boot application properties placeholders
* Fix #2456: Add utility class to decompress archive files
* Fix #2465: Add support for `buildpacks` build strategy with opinionated defaults (Zero config)
* Fix #2472: Support for Helm Chart.yaml appVersion field defaulting to project version
* Fix #2474: Remove Docker-related classes providing  unused functionality
* Fix #2477: Support for Spring Boot 3.2.0 (and newer) layered jar format
* Fix #2500: `oc:build` does not add git annotations in Openshift Build resource
* Fix #2503: Container Images Jolokia agent bumped to 2.0.0
* Fix #2532: Bump version.kubernetes-client from 6.9.2 to 6.10.0
* Fix #2613: Added new helm lint goal task (k8s:helm-lint / k8sHelmLint)
* Fix #2541: Container image names can now be set as IPv6 addresses
* Fix #2622: `k8s:helm` generated chart tar archive contains reference to tar archive

_**Note**_:
Kubernetes manifests generated by JKube would now contain the following labels by default:
- `app.kubernetes.io/component`
- `app.kubernetes.io/managed-by`
- `app.kubernetes.io/name`
- `app.kubernetes.io/part-of`
- `app.kubernetes.io/version`

These are added in favor of deprecating and removing the current project labels in future releases:
- `app`
- `group`
- `provider`
- `version`

### 1.15.0 (2023-11-10)
* Fix #2138: Support for Spring Boot Native Image
* Fix #2186: Reuse ` io.fabric8.kubernetes.client.utils.KubernetesResourceUtil` ConfigMap utils methods
* Fix #2200: Support for Helm `values.yaml` fragments
* Fix #2356: Helm values.yaml parameter names preserve case
* Fix #2369: Helm chart apiVersion can be configured
* Fix #2379: Do not flatten image assembly layers in case of OpenShift Docker build strategy. 
* Fix #2386: Helm icon inferred from annotations in independent resource files (not aggregated kubernetes/openshift.yaml)
* Fix #2397: Default JKube work directory (`jkube.workDir`) changed from `${project.build.directory}/jkube` to `${project.build.directory}/jkube-temp`
* Fix #2393: Remove timestamp from `org.label-schema.build-date` LABEL to utilize docker cache
* Fix #2399: Helm no longer generates default function; broadens support for different value types
* Fix #2400: Helm supports complex values in `values.yaml` fragments (such as annotations or arrays)
* Fix #2414: OpenShift Gradle Plugin picks up `jkube.build.pushSecret` property
* Fix #2417: Don't pass Invalid port in host headers for Helm OCI push
* Fix #2419: Fix `Fabric8HttpUtil.extractAuthenticationChallengeIntoMap` www-authenticate header parsing logic
* Fix #2425: Bump JKube Base images to 0.0.20
* Fix #2433: Bump default native base image to `registry.access.redhat.com/ubi9/ubi-minimal:9.3`
* Fix #2436: Debug functionality works with Spring Boot Layered JARs

_**Note**_:
- Container Images generated using jkube opinionated defaults no longer contain full timestamp in `org.label-schema.build-date` label. The label contains the build date in the format `yyyy-MM-dd`.

### 1.14.0 (2023-08-31)
* Fix #1674: SpringBootGenerator utilizes the layered jar if present and use it as Docker layers
* Fix #1713: Add HelidonHealthCheckEnricher to add Kubernetes health checks for Helidon applications
* Fix #1714: Add HelidonGenerator to add opinionated container image for Helidon applications
* Fix #1929: Docker Image Name parsing fix
* Fix #1946: Initial support for Gitpod
* Fix #1985: Update outdated methods in Spring Boot CRD Maven Quickstart
* Fix #2091: Support for pushing Helm charts to OCI registries
* Fix #2116: Remove user field from ImageName class
* Fix #2219: Kind/Filename mappings include optional apiVersion configuration
* Fix #2224: Quarkus native base image read from properties (configurable)
* Fix #2228: Quarkus native base image uses UBI 8.7
* Fix #2239: Quarkus healthcheck enricher infers overridden server port in application.properties
* Fix #2290: JKube is not picking docker credentials from `~/.docker/config.json` file
* Fix #2293: JibServiceUtil pushes separate images for additional tags specified in build configuration
* Fix #2299: Gradle v8.x compatibility
* Fix #2301: Add compatibility with SemVer versions
* Fix #2302: Bump Kubernetes Client version to 6.8.0
* Fix #2324: Update SpringBootConfigurationHelper for Spring Boot 3.x
* Fix #2350: Enrichers with NAME configuration override fragments with default names
* Fix #2353: Add condition and alias to HelmDependency model

### 1.13.1 (2023-06-16)
* Fix #2212: Bump Kubernetes Client version to 6.7.2 (fixes issues when trace-logging OpenShift builds -regression in 6.7.1-)

### 1.13.0 (2023-06-14)
* Fix #1478: Should detect and warn the user if creating ingress and ingress controller not available
* Fix #2092: Support for `Chart.yaml` fragments
* Fix #2150: Bump Kubernetes Client to 6.6.0 (fixes issues when trace-logging OpenShift builds)
* Fix #2162: Bump Kubernetes Client to 6.6.1 (HttpClient with support for PUT + InputStream)
* Fix #2165: Introduce a Kubernetes resource Security Hardening profile (opt-in)
* Fix #2166: Potential command line injection in SpringBootWatcher
* Fix #2170: `internal-microservice` profile prevents Service exposure
* Fix #2174: Profile merge constructor accounts for parentProfile field
* Fix #2187: `serviceAccount` configuration option has stopped working
* Fix #2192: Bump Kubernetes Client to 6.7.1 (use JKube Serialization util to wrap around the Kubernetes Client KubernetesSerialization)
* Fix #2201: Bumps JKube Base images to 0.0.19

### 1.12.0 (2023-04-03)
* Fix #1179: Move storageClass related functionality out of VolumePermissionEnricher to PersistentVolumeClaimStorageClassEnricher
* Fix #1118: refactor ConfigHelper.initImageConfiguration for clarity
* Fix #1273: Deprecate `jkube.io` annotation prefix in favor of `jkube.eclipse.org` for JKubeAnnotations
* Fix #2040: Add support for adding resource limits via controller resource config
* Fix #1954: Add support for CronJob
* Fix #2086: Allow concurrent remote-dev sessions (service selector includes session id)
* Fix #2093: ClassCastException when extracting plugins from pom
* Fix #2100: NullPointerException while verifying assembly references in Dockerfile
* Fix #2108: Bump BouncyCastle to 1.72
* Fix #2108: Use BouncyCastle JDK 1.8 compatible jars
* Fix #2104: Bump kubernetes-client to 6.5.1 (#2079)
* Fix #2101: Resolve dynamic transitive properties when creating Dockerfile
* Fix #2113: Removed unsupported Docker compose utilities inherited from Docker Maven Plugin (DMP)

### 1.11.0 (2023-02-16)
* Fix #1316: Add support for adding InitContainers via plugin configuration
* Fix #1439: Add hint to use `jkube.domain` if `createExternalUrls` is used without domain
* Fix #1459: Route Generation should support `8443` as default web port
* Fix #1546: Migrate to JUnit5 testing framework
* Fix #1829: Add trailing newline for labels/annotations for multiline values to avoid setting block chomping indicator
* Fix #1858: Properties in image name not replaced
* Fix #1934: schema validation warnings during `mvn oc:resource` and `gradle k8sResource`
* Fix #1935: `oc:remote-dev` goal / `ocRemoteDev` task have wrong log prefixes
* Fix #1966: Old reference to fmp in documentation
* Fix #1974: Remove unused methods in KubernetesResourceUtil
* Fix #2003: check local port available on start remote-dev
* Fix #2004: AnsiOutputStream exceptions don't prevent logging or program execution 
* Fix #2008: resources validated after their generation by `k8s:resource`
* Fix #2052: Remote Dev discovers remote ports for local services exposed in the cluster
* Fix #2052: Remote Dev includes a SOCKS 5 proxy
* Fix #1369: `k8s:helm` no longer contains classifier in helm chart archive.

_**Note**_:
- Helm archive generated by `k8s:helm`/`k8sHelm` would no longer contain trailing classifier.However, you can add a classifier using `jkube.helm.tarFileClassifier` configuration field.  For example, In previous releases a helm chart named `foo` with version `0.0.1` had `foo-0.0.1-helm.tar.gz` as archive name. From this release onwards, the name for equivalent archive file would be `foo-0.0.1.tar.gz`
- Default location of Helm archive generated by `k8s:helm`/`k8sHelm` has been changed. In previous releases, it was (`target`/`build`). From this release onwards, Helm Chart archive would be generated in the same directory where Helm Chart is generated, defaulting to the following values:
  - Kubernetes Maven Plugin : `target/jkube/helm/${chartName}/kubernetes`
  - Kubernetes Gradle Plugin : `build/jkube/helm/${chartName}/kubernetes`
  - OpenShift Maven Plugin : `target/jkube/helm/${chartName}/openshift`
  - OpenShift Gradle Plugin : `build/jkube/helm/${chartName}/openshift`

### 1.10.1 (2022-11-16)
* Fix #1915: Maven 3.6.3 is still supported

### 1.10.0 (2022-11-10)
* Fix #443: Add health check enricher for SmallRye Health
* Fix #508: Kubernetes Remote Development (inner-loop)
* Fix #1668: Allow additional services (via fragments) besides the default
* Fix #1684: Podman builds with errors are correctly processed and reported 
* Fix #1704: `k8s:watch` with `jkube.watch.mode=copy` works as expected
* Fix #1888: KubernetesExtension has helper method to add image with builder

### 1.9.1 (2022-09-14)
* Fix #1747: Apply service doesn't attempt to create OpenShift Projects in Kubernetes clusters

### 1.9.0 (2022-09-09)
* Fix #777: `k8s:build` with Dockerfile throws `Connection reset by peer` error on old docker daemons
* Fix #1006: Initial support for JKube Plugins (refactor of DMP BuildPlugins)
* Fix #1137: Add support to add `.resources` in initContainer generated by VolumePermissionEnricher
* Fix #1156: `k8s:build`/`k8sBuild` tries to access Kubernetes Cluster
* Fix #1279: Remove redundant log messages regarding plugin modes
* Fix #1358: Improvement to add skipUndeploy flag for undeploy goal
* Fix #1361: VolumePermissionEnricher : Use `.spec.storageClassName` instead of annotation to set PersistentVolume class
* Fix #1411: Add support for adding additional ImageStreamTags in OpenShift S2I builds
* Fix #1438: Add configuration option in ServiceAccountEnricher to skip creating ServiceAccounts
* Fix #1464: Bump Fabric8 Kubernetes Client to 6.0.0
* Fix #1468: Add startup probe in QuarkusHealthCheckEnricher
* Fix #1473: Add OpenLibertyHealthCheckEnricher to automatically add health checks in OpenLiberty projects
* Fix #1474: Add startup probe in WildflyJARHealthCheckEnricher
* Fix #1532: ImageChangeTriggerEnricher shouldn't generate ImageChange triggers for JIB build strategy
* Fix #1537: Registry not set up in `oc:build`
* Fix #1619: Add `jkube.imagePullPolicy` configuration property to configure pull policy in all enrichers
* Fix #1634: MicronautHealthCheckEnricher should also consider Micronaut Gradle Plugin in isApplicable
* Fix #1649: VertxHealthCheckEnricher nested enricher configuration does not work for Gradle Plugins
* Fix #1654: images with missing build configuration should not be built
* Fix #1670: Bump Quarkus native base image to ubi-minimal:8.6
* Fix #1679: Bump jib-core to 0.21.0
* Fix #1689: Bump JKube maintained base images to 0.0.16
* Fix #1712: Port JKube Plugin Maven quickstart to gradle and add documentation for jkube plugin in asciidocs
* Fix #1736: Bump Fabric8 Kubernetes Client to 6.1.1

_**Note**_:
- Enricher configuration `jkube.enricher.jkube-controller.pullPolicy` has been marked as deprecated, use `jkube.imagePullPolicy` property instead.
- Enricher configuration `jkube.enricher.jkube-controller-from-configuration.pullPolicy` has been marked as deprecated, use `jkube.imagePullPolicy` property instead.


### 1.8.0 (2022-05-24)
* Fix #1188: Add support for specifying multiple tags in Zero configuration mode
* Fix #1194: jkube controller name configuration ignored when using resource fragments
* Fix #1201: ThorntailV2Generator works with Gradle Plugins
* Fix #1208: Allow env variables to be passed to a webapp generator s2i builder
* Fix #1218: Generate yaml files for different environments
* Fix #1251: Generate a preview of jkube documentation for PR if needed
* Fix #1259: Add integration test and docs for NameEnricher
* Fix #1260: Add documentation for PodAnnotationEnricher
* Fix #1261: Add documentation for PortNameEnricher
* Fix #1262: Add docs + gradle integration test for ProjectLabelEnricher
* Fix #1263: Remove RemoveBuildAnnotationsEnricher
* Fix #1268: ProjectLabelEnricher group and version labels should be configurable
* Fix #1284: webapp custom generator should not require to set a CMD configuration
* Fix #1295: Spring Boot actuator endpoints failed to generate automatically if `deployment.yml` resource fragment is used
* Fix #1297: ReplicaCountEnricher documentation ported to Gradle plugins
* Fix #1298: Add integration test + documentation for RevisionHistoryEnricher in gradle plugins
* Fix #1299: Add gradle integration tests and documentation for ServiceAccountEnricher in gradle plugins.
* Fix #1301: Improve documentation and gradle integration test for TriggersAnnotationEnricher
* Fix #1302: Port volume-permission maven integration tests + docs to gradle 
* Fix #1303: Add gradle integration test and docs for AutoTLSEnricher
* Fix #1308: Add documentation for DeploymentConfigEnricher
* Fix #1309: Remove ExposeEnricher from profiles and documentation
* Fix #1310: Add documentation + gradle integration test for ImageChangeTriggerEnricher
* Fix #1311: Add documentation for OpenShift ProjectEnricher
* Fix #1312: Add documentation + gradle integration test for RouteEnricher
* Fix #1322: Support for Startup probes via XML/DSL configuration
* Fix #1325: `jkube.enricher.jkube-name.name` doesn't modify `.metadata.name` for generated manifests
* Fix #1328: Provide guidance when the final project packaged file is not found
* Fix #1336: Add documentation and quickstart for using custom generators with Gradle Plugins
* Fix #1362: VolumePermissionEnricher : Replace iteration with bulk Collection.addAll call
* Fix #1382: Docker Build ARGS not replaced properly
* Fix #1324: Support legacy javaee as well as jakartaee projects in the Tomcat webapp generator
* Fix #1460: Route doesn't use the service "normalizePort"
* Fix #1470: Add support for Apple M1 CPUs
* Fix #1482: Quarkus Generator and Enricher should be compatible with the Red Hat build
* Fix #1483: Assembly files with unnormalized paths don't get fileMode changes
* Fix #1489: Align BaseGenerator's `add` and `tags` config options to work with `jkube.generator.*` properties
* Fix #1512: Gradle plugins are graduated

### 1.7.0 (2022-02-25)
* Fix #1315: Pod Log Service works for Jobs with no selector
* Fix #1126: Liveness and readiness TCP ports are not serialized as numbers when defined as numbers
* Fix #1211: Port `java-options-env-merge` integration test and ContainerEnvJavaOptionsMergeEnricher documentation to gradle
* Fix #1214: Add integration test to verify `jkube.debug.enabled` flag works as expected
* Fix #1236: Add integration tests for DefaultControllerEnricher in gradle plugins
* Fix #1237: Add gradle project integration test for DefaultMetadataEnricher
* Fix #1238: Port DefaultNamespaceEnricher integration test and documentation to gradle plugins
* Fix #1239: Add documentation and integration test for DefaultServiceEnricher in gradle plugins
* Fix #1240: Add documentation for DependencyEnricher
* Fix #1257: Add documentation for ImageEnricher
* Fix #1278: Enricher with type Job does not generate mandatory `spec.template.spec.restartPolicy`
* Fix #1294: Prometheus and Jolokia agents can be disabled by setting their port to `0`

### 1.6.0 (2022-02-03)
* Fix #1047: Gradle image build should use the Quarkus generator for Quarkus projects
* Fix #778: Support deserialization of fragments with mismatched field types of target Java class 
* Fix #802: Update Fabric8 kubernetes Client to v5.10.1
* Fix #887: Incorrect warning about overriding environment variable
* Fix #900: Fix #900: Remove `projectArtifactId` and `projectVersion` from `gradle.properties` in Spring Boot Helm Quickstart
* Fix #961: `k8sBuild`, `k8sResource` and `k8sApply` tasks don't respect skip options
* Fix #1030: Update IngressEnricher's default targetApiVersion to `networking.k8s.io/v1`
* Fix #1054: Log selected Dockerfile in Docker build mode
* Fix #1055: Throw Exception when podman daemon returns `null` for given image post build
* Fix #1106: Container Images based on Java 17 (Java-exec, Tomcat, Jetty, Karaf)
* Fix #1113: `ResourceUtil.deserializeKubernetesListOrTemplate` should also handle YAML manifests with multiple docs
* Fix #1120: OpenShiftBuildService flattens assembly only if necessary
* Fix #1123: Helm supports `.yaml` and `.yml` source files
* Fix #1136: ConcurrentModificationException when running gradle k8sBuild on Quarkus sample
* Fix #1145: Remove redundant ServiceHubFactory
* Fix #1146: VertxGenerator now works with Kubernetes Gradle Plugin
* Fix #1148: Enable MicronautGenerator with Kubernetes Gradle Plugin
* Fix #1150: Enable OpenLibertyGenerator on Kubernetes Gradle Plugin
* Fix #1157: Enable Create Image (pull) HTTP API options
* Fix #1167: Replace apiextensions.k8s.io/v1beta1 by apiextensions.k8s.io/v1
* Fix #1180: `k8s:watch` uses default namespace even if other namespace is configured
* Fix #1190: OpenShiftBuildService doesn't apply resources in configured namespace
* Fix #1195: Push goal doesn't respect skip option in image build configuration
* Fix #1209: Remove WildFly Swarm support
* Fix #1219: Bump kubernetes-client to 5.11.2
* Fix #1213: SnakeYaml dependency from Kubernetes Client + uses SafeConstructor
* Fix #1142: Log informative message whenever a goal/task is skipped
* Fix #1197: Kubernetes Gradle Build tasks don't account for image model configuration skip field
* Fix #1245: Deprecate `maven.jkube.io` annotation prefix used in enrichers in favor of `jkube.eclipse.org`
* Fix #1244: Helm Parameters/Variables/Values.yaml can be configured via XML/DSL
* Fix #1244: Helm parameters can use dotted notation `${helm.parameter.with.dots}`
* Fix #1232: Warning, instead of exception, on temporary image existence
* Fix #1192: Log Docker Build Context Directory while building image in Simple Dockerfile mode

### 1.5.1 (2021-10-28)
* Fix #1084: Gradle dependencies should be test or provided scope

### 1.5.0 (2021-10-28)
* Fix #31: Kubernetes/OpenShift Gradle Plugin initial implementation **(Preview)**
* Fix #971: Rethrow the InterruptException that is caught in ApplyMojo.java
* Fix #823: Converting `StringBuffer` to `StringBuilder` in `jkube/jkube-kit/common/src/main/java/org/eclipse/jkube/kit/common/util/IoUtil.java` and `jkube/jkube-kit/common/src/main/java/org/eclipse/jkube/kit/common/util/TemplateUtil.java`
* Fix #815: `java.lang.ClassCastException` during `oc:build` when OpenShift not present
* Fix #716: Update Spring Boot Quickstarts to the latest version
* Fix #907: Bump JKube base images to 0.0.10
* Fix #832: Bump jib-core to 0.20.0
* Fix #904: DeploymentConfig ImageChange trigger seems to be wrong for custom images
* Fix #904: `%g` should be resolved to namespace in OpenShift Maven Plugin
* Fix #908: OpenShift-Maven-Plugin doesn't remove periods from container name
* Fix #923: QuarkusGenerator not applicable when using `io.quarkus.platform` groupId
* Fix #895: FileUtil#createDirectory works for files with trailing separator (`/`)
* Fix #913: Make provider name configurable
* Fix #877: LogMojo: Change access modifiers to protected for use in XML configuration
* Fix #1036: JKubeTarArchive doesn't load files in memory
* Fix #1040: Remove deprecated `ExpectedException.none()` and `@Rule` and use `assertThrows` instead
* Fix #891: VolumePermissionEnricher has configurable image with `busybox` as default

_**Note**_: Kubernetes and OpenShift Gradle Plugins are a preview feature to get early feedback.
Only the set of documented features are available to users.

### 1.4.0 (2021-07-27)
* Fix #253: Refactor JKubeServiceHub's BuildService election mechanism via ServiceLoader
* Fix #425: Multi-layer support for Container Images
* Fix #548: Define property for skipping cluster autodetect/offline mode
* Fix #551: Add Configuration options to IngressEnricher
* Fix #653: `k8s:watch` port-forward websocket error due to wrong arguments in PortForwardService
* Fix #672: Add missing fields in ProbeConfig for configuring Readiness/Liveness Probes
* Fix #701: Update Fabric8 Kubernetes Client to 5.4.0
* Fix #704: NPE when using Service fragment with no port specified
* Fix #705: JIB assembly works on Windows
* Fix #710: Support DockerImage as output for Openshift builds
* Fix #714: feat: Helm support for Golang expressions
* Fix #718: Port fabric8io/docker-maven-plugin#1318: Update ECR authorization token URL
* Fix #725: Upgrade HttpClient from 4.5.10 to 4.5.13
* Fix #730: Port fabric8io/docker-maven-plugin#1311: Use AWS SDK to fetch AWS credentials
* Fix #741: Private constructor added to Utility classes
* Fix #751: QuarkusGenerator: Multi-layer images for the different Quarkus packaging modes
* Fix #756: Service re-apply error happening during `k8s:watch`
* Fix #758: QuarkusHealthCheckEnricher: default health path value is outdated
* Fix #777: Error processing tar file(archive/tar: missed writing 4096 bytes) on Docker 1.12.1

### 1.3.0 (2021-05-18)
* Fix #497: Assembly descriptor removed but still in documentation
* Fix #576: Add support to publishing helm chart
* Fix #634: Replace occurrences of keySet() with entrySet() when value are needed
* Fix #659: Update Fabric8 Kubernetes Client to v5.3.0
* Fix #602: Add documentation regarding automatic Route generation
* Fix #630: Document usage for `jkube.build.switchToDeployment` flag
* Fix #647: Resource configuration can now add annotations and labels to ServiceAccount
* Fix #632: Add support for Quarkus Fast Jar Packaging
* Fix #578: NullPointerException in ContainerEnvJavaOptionsMergeEnricher on k8s:resource
* Fix #671: ApplyMojo not considering namespace configured from XML configuration
* Fix #677: KarafGenerator includes Jolokia port (8778)
* Fix #682: Update CircleCI image to new version
* Fix #666: Replace deprecated JsonParser
* Fix #673: Remove unused `ports` field in ResourceConfig
* Fix #679: Quarkus package detection improvements
* Fix #622: Corrected documentation for `jkube-healthcheck-karaf`
* Fix #630: DeploymentConfigEnricher and DefaultControllerEnricher refactored and aligned
* Fix #639: Quotas for OpenShift BuildConfig not working
* Fix #688: Multiple Custom Resources with same (different apiGroup) name can be added
* Fix #689: Recreate and update of CustomResource fragments works
* Fix #690: Helm charts can be generated for custom resources, even those with same name (different apiGroup)
* Fix #676: Define Helm Chart dependencies
* Fix #590: Only assembled files are copied to 'Docker' build target directory
* Fix #655: Set image creation time on JIB build (!breaks image reproducibility)

### 1.2.0 (2021-03-31)
* Fix #529: `.maven-dockerignore`, `.maven-dockerexclude`, `.maven-dockerinclude` are no longer supported
* Fix #558: Update CircleCI sonar jobs to run using JDK 11 environment.
* Fix #546: Use `java.util.function.Function` instead of Guava's `com.google.common.base.Function`
* Fix #545: Replace Boolean.valueOf with Boolean.parseBoolean for string to boolean conversion
* Fix #515: Properties now get resolved in CustomResource fragments
* Fix #586: Apply and Undeploy service use configured namespace
* Fix #584: Improved Vert.x 4 support
* Fix #592: Upgrade kubernetes client from 5.0.0 to 5.1.1
* Fix #615: Update Fabric8 Kubernetes Client from v5.1.1 to v5.2.1
* Fix #594: Debug with suspend mode removes Liveness Probe
* Fix #591: Helm linter no longer fails with generated charts
* Fix #587: Helm tar.gz packaged chart can be deployed
* Fix #571: Replace usages of deprecated `org.apache.commons.text.StrSubstitutor`
* Fix #450: Quarkus port is inferred from application.properties/yaml (considers profile too)
* Fix #471: Remove the declaration of thrown runtime exceptions across javadoc
* Fix #620: Added k8s support for NetworkPolicy
* Fix #624: Unable to override Image Name in Simple Dockerfile Mode with `jkube.generator.name` 

### 1.1.1 (2021-02-23)
* Fix #570: Disable namespace creation during k8s:resource with `jkube.namespace` flag
* Fix #579: Fixed quarkus health paths

### 1.1.0 (2021-01-28)
* Fix #455: Use OpenShiftServer with JUnit rule instead of directly instantiating the OpenShiftMockServer
* Fix #467: Upgrade assertj-core to 3.18.0
* Fix #460: Added a Quickstart for implementing and using a Custom Enricher based on Eclipse JKube Kit Enricher API
* Fix #240: No Ports exposed inside Deployment in case of Zero Config Dockerfile Mode
* Fix #480: wildfly-jar doesn't depend on common-maven module
* Fix #268: Generator and HealthCheck enrichers for Micronaut framework (JVM)
* Fix #488: Controller enricher replica count can be set to `0` when ResourceConfig is provided
* Fix #485: Filter with placeholders in Dockerfile is broken 
* Fix #387: Update Fabric8 Kubernetes Client to v4.13.0 to support `networking.k8s.io/v1` `Ingress`
* Fix #473: Debug goals work with QuarkusGenerator generated container images
* Fix #484: cacheFrom configuration parameter is missing
* Fix #181: Refactor PortForwardService to use Kubernetes Client Port Forwarding instead of kubectl binary
* Fix #535: Bump JKube base images to 0.0.9
* Fix #509: Port of ServiceDiscoveryEnricher from FMP
* Fix #510: Update Compatibility matrix for OpenShift 4.5 and 4.6
* Fix #511: Namespace as resource fragment results in NullPointerException
* Fix #521: NPE on Buildconfig#getContextDir if `<dockerFile>` references a file with no directory
* Fix #513: openshift-maven-plugin: service.yml fragment with ports creates service with unnamed port mapping
* Fix #231: IngressEnricher ignores IngressRules defined in XML config
* Fix #523: period in image username omits registry in Deployment manifest

### 1.0.2 (2020-10-30)
* Fix #429: Added quickstart for Micronaut framework
* Fix #370: Replacing anonymous Runnables with lambdas in WatchService
* Fix #440: Added quickstart for MicroProfile running on OpenLiberty
* Fix #442: Valid content type for REST docker API requests with body
* Fix #448: Service.spec.type added from config if existing Service fragment doesn't specify it
* Fix #451: Docker push works with Podman REST API
* Fix #447: Removed misleading watch-postGoal warning + fixed rolling update in DockerImageWatcher
* Fix #452: CustomResource apply logic, should consider Kind too while checking CustomResourceDefinitionContext
* Fix #174: Build failure when trying to use Dockerfile for arguments in FROM, port of fabric8io/docker-maven-plugin#1299


### 1.0.1 (2020-10-05)
* Fix #381: Remove root as default user in AssemblyConfigurationUtils#getAssemblyConfigurationOrCreateDefault
* Fix #382: Add support for merging fragment Route spec with default generated Route
* Fix #358: Prometheus is enabled by default, opt-out via `AB_PROMETHEUS_OFF` required to disable (like in FMP)
* Fix #365: jkube.watch.postGoal property/parameter/configuration is ignored
* Fix #384: Enricher defined Container environment variables get merged with vars defined in Image Build Configuration
* Fix #386: final fat jar is not updated in docker build directory
* Fix #385: WildFly Bootable JAR - Add native support for slim Bootable JAR
* Fix #415: Fixed resource validation for new json-schema-validator version
* Fix #356: Add further support for tls termination and insecureEdgeTerminationPolicy in pom.xml
* Fix #327: k8s:resource replaces template variables in provided fragments, k8s:helm doesn't.
* Fix #364: jkube.watch.postExec property/parameter/configuration is ignored

### 1.0.0 (2020-09-09)
* Fix #351: Fix AutoTLSEnricher - add annotation + volume config to resource
* Fix #344: Fix documentation for `jkube.openshift.imageChangeTriggers`
* Fix #290: Bump Fabric8 Kubernetes Client to v4.10.3
* Fix #273: Added new parameter to allow for custom _app_ name
* Fix #329: Vert.x generator and enricher applicable when `io.vertx` dependencies present
* Fix #334: Build strategy picked up from XML configuration and properties
* Fix #342: Maven Quickstart for Spring-Boot with complete Camel integration
* Fix #336: Fix pull from insecure registries and pull registry authentication for JIB
* Fix #332: OpenShift build resources are deleted (as long as build config manifest is available)
* Fix #350: Prevents default docker configuration overwriting XML assembly configuration
* Fix #340: Exclude the main artifact from Docker build when Fat Jar is detected (JavaExecGenerator)
* Fix #341: JKube doesn't add ImageChange triggers in DC when merging from a deployment fragment
* Fix #326: Add default `/metrics` path for `prometheus.io/path` annotation
  (Sort of [redundant](https://prometheus.io/docs/prometheus/latest/configuration/configuration/#scrape_config), this makes it explicit)
* Fix #339: Https should be considered default while converting tcp urls
* Fix #362: Quarkus supports S2I builds both for JVM and native mode
* Fix #297: Added missing documentation for WatchMojo configuration parameters
* Fix #371: WebAppGenerator path configuration is no longer ignored

### 1.0.0-rc-1 (2020-07-23)
* Fix #252: Replace Quarkus Native Base image with ubi-minimal (same as in `Dockerfile.native`)
* Fix #187: Provided Dockerfile is always skipped in simple Dockerfile mode
* Fix #237: Remove deprecated fields and method calls
* Fix #218: Remove build mode from mojos
* Fix #192: Removed `@Deprecated` fields from ClusterAccess
* Fix #190: Removed `@Deprecated` fields from AssemblyConfiguration
* Fix #189: Removed `@Deprecated` fields from BuildConfiguration
* Fix #73: Jib Support, Port of fabric8io/fabric8-maven-plugin#1766
* Fix #195: Added MigrateMojo for migrating projects from FMP to JKube
* Fix #238: Watch uses same logic as build to monitor changed assembly files
* Fix #245: Debug goals work on webapp (Tomcat & Jetty) > See https://github.com/eclipse-jkube/jkube-images/releases/tag/v0.0.7
* Fix #261: DockerFileBuilder only supports \*nix paths (Dockerfile Linux only), fixed invalid default configs
* Fix #246: Dockerfile custom interpolation is broken
* Fix #259: Cleanup unused properties inside Mojos
* Fix #94: Properly define + document JKubeProjectAssembly behavior
* Fix #248: Properly name and document (Maven/System) configuration properties
* Fix #284: warning message in log goal when no pod is found
* Fix #267: openshift-maven-plugin does not update Routes
* Fix #286: Refactor ImageConfiguration model
* Fix #283: Add support for WildFly Bootable JAR
* Fix #306: Template resolution and helm work in OpenShift-Maven-Plugin

### 1.0.0-alpha-4 (2020-06-08)
* Fix #173: Use OpenShift compliant git/vcs annotations
* Fix #182: Assembly is never null
* Fix #184: IngressEnricher seems to be broken, Port of fabric8io/fabric8-maven-plugin#1730
* Fix #198: Wildfly works in OpenShift with S2I binary build (Docker)
* Fix #199: BaseGenerator retrieves runtime mode from context (not from missing properties)
* Fix #201: Webapp-Wildfly supports S2I source builds too (3 modes Docker, OpenShift-Docker, OpenShift-S2I)
* Fix #205: JavaExecGenerator uses jkube/jkube-java-binary-s2i for Docker and S2I builds (#183)
* Fix #206: WebAppGenerator with "/" path renames artifacts to ROOT.war
* Fix #206: WebAppGenerator\>TomcatAppSeverHandler uses quay.io/jkube/jkube-tomcat9-binary-s2i as base image
* Fix #210: WebAppGenerator\>JettyAppSeverHandler uses quay.io/jkube/jkube-jetty9-binary-s2i as base image
* Fix #211: pom.xml configured runtime mode `<mode>` is considered instead of `<configuredRuntimeMode>`
* Fix #209: WildFlySwarmGenerator includes required env variables + java options
* Fix #214: fix: KarafGenerator update (Created Karaf Quickstart #188, fix FileSet problems, upgraded base images)
* Update Fabric8 Kubernetes Client to v4.10.2
* Fix #220: Remove Red Hat specific image support
* Fix #221: Role Resources Not Supported by Kubernetes Cluster Configurations
* Fix #226: Refactored FileUtil#getRelativeFilePath to use native Java File capabilities
* Fix #224: RoleBinding Resources Not Supported by Kubernetes Cluster Configurations
* Fix #191: Removed `@Deprecated` fields from RunImageConfiguration
* Fix #233: Provided Slf4j delegate KitLogger implementation
* Fix #234: Watch works with complex assemblies using AssemblyFile

### 1.0.0-alpha-3 (2020-05-06)
* Fix #167: Add CMD for wildfly based applications
* Added Webapp Wildfly maven quickstart
* Fix #171: OpenShift pull secret not picked up without registry auth configuration
* Fix #171: Customized Quarkus application quick start
* Fix #101: Removed OpenShift specific functionality from Kubernetes Maven Plugin
* Fix #178: Bump kubernetes-client from 4.9.1 to 4.10.1

### 1.0.0-alpha-2 (2020-04-24)
* Fix #130: Updated HelmMojo documentation
* Fix #138: dockerAccessRequired should be false in case of docker build strategy
* Fix #131: Extra-artifact wrongly generated
* Fix #152: kubernetes namespace is ignored in debug goal
* Ported PR fabric8io/fabric8-maven-plugin#1810, Update Fabric8 Images to latest version
* Fix #136: Refactored configuration model to provide fluent builders and defaults
* Fix #163: Added Quickstart for external Dockerfile

### 1.0.0-alpha-1 (2020-03-27)
* Ported PR fabric8io/fabric8-maven-plugin#1792, NullCheck in FileDataSecretEnricher
* Fix #53: Renamed plugins to openshift/kubernetes-maven-plugin keeping acronym (oc/k8s) for goal
* Fix #97: Port of fabric8io/fabric8-maven-plugin#1794 to fix ImageChange triggers not being set in DeploymentConfig when resource fragments are used
* Ported PR fabric8io/fabric8-maven-plugin#1802, Labels are missing for some objects
* Ported PR fabric8io/fabric8-maven-plugin#1805, NullPointerException in ConfigMapEnricher
* Ported PR fabric8io/fabric8-maven-plugin#1772, Support for setting BuildConfig memory/cpu request and limits
* Fix #112: Fix windows specific path error while splitting file path
* Fix #102: HelmMojo works again
* Fix #120: Critical bugs reported by Sonar
* Fix #88: Unreachable statement in DockerAssemblyManager
* Fix #122: Bug 561261 - jkube-kit - insecure yaml load leading to RCE (CWE-502)

### 0.2.0 (2020-03-05)
* Fix #71: script to extract changelog information for notifications
* Fix #34: Ported OpenLiberty support: https://github.com/fabric8io/fabric8-maven-plugin/pull/1711
* Fix #76: Github actions checkout v2
* Updated Kubernetes-Client Version to 4.7.1 #70
* Fix #30: Decouple JKube-kit from maven
* Fix #87: JKubeAssemblyFile is processed
* Fix #89: Reorganized Quickstarts + Added PR verifications (versionable and compilable)
* Fix #92: NullPointerException in AuthConfigFactory
* Fix #89: Reorganized examples + fixed assembly bug
* Fix #52: Use proper case in JKube brand name
* Doc #43: added default md file and links for CONTRIBUTING guide
* Doc #61: Added a Hello World Quickstart sample
* Fix #95: filtered is for variable interpolation/substitution, not for exclusion
* Doc #91: Quarkus Native Quickstart


### 0.1.1 (2020-02-14)
* Refactor: Add Maven Enforcer Plugin #29
* Fixed broken Quarkus Sample #65
* Doc: Added Migration guide for Fabric8 Maven Plugin users #64
* Fix: Problem with plexus detecting target value type for bean injection #62
* Fix: Disable Jolokia, missing PR port from FMP for ThorntailGenerator #60
* Fix: Report consolidation/artifact generation in GitHub Actions #59
* Fix: Docker builds are supported in oc-maven-plugin #56
* Fix: Generated webapp war has wrong name #54
* Feat: Configurable Dekorate integration #36
* Fix: Missing component declaration in Plexus container #41
* Fix: Close input streams + other fixes #41
* Refactor: removed implementations from config/resource -> resource/service #46
* Refactor: jkube-kit-config-image decoupled from Maven #45
* Refactor: Clean up dependencies + enforce version convergence #39
* Doc: Add documentation #38
* Add Sample Quickstarts to project #33
* Fix: Run tests for current version #35

### 0.1.0 (2019-12-19)
* Initial release

