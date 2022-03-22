# Changes

This document main purpose is to list changes which might affect backwards compatibility. It will not list all releases as Eclipse JKube is build in a continous delivery fashion.

We use semantic versioning in some slight variation until our feature set has stabilized and the missing pieces has been filled in:

* The `MAJOR_VERSION` is kept to `0`
* The `MINOR_VERSION` changes when there is an API or configuration change which is not fully backward compatible.
* The `PATCH_LEVEL` is used for regular CD releases which add new features and bug fixes.

After this we will switch probably to real [Semantic Versioning 2.0.0](http://semver.org/)

## Extracting changelog portions
We provide a script to extract changelog portions and automatic link building to send notifications
(i.e. e-mail) about new releases
([scripts/extract-changelog-for-version.sh](https://github.com/eclipse/jkube/blob/master/scripts/extract-changelog-for-version.sh))

Usage:
```
# ./scripts/changelog.sh semanticVersionNumber [linkLabelStartNumber]
./scripts/extract-changelog-for-version.sh 1.3.37 5
```
### 1.8.0-SNAPSHOT
* Fix #1201: ThorntailV2Generator works with Gradle Plugins
* Fix #1251: Generate a preview of jkube documentation for PR if needed
* Fix #1260: Add documentation for PodAnnotationEnricher
* Fix #1261: Add documentation for PortNameEnricher
* Fix #1262: Add docs + gradle integration test for ProjectLabelEnricher
* Fix #1284: webapp custom generator should not require to set a CMD configuration
* Fix #1295: Spring Boot actuator endpoints failed to generate automatically if `deployment.yml` resource fragment is used
* Fix #1297: ReplicaCountEnricher documentation ported to Gradle plugins
* Fix #1308: Add documentation for DeploymentConfigEnricher
* Fix #1310: Add documentation + gradle integration test for ImageChangeTriggerEnricher
* Fix #1325: `jkube.enricher.jkube-name.name` doesn't modify `.metadata.name` for generated manifests

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
* Fix #245: Debug goals work on webapp (Tomcat & Jetty) > See https://github.com/jkubeio/jkube-images/releases/tag/v0.0.7
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
