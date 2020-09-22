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
### 1.0.1-SNAPSHOT
* Fix #381: Remove root as default user in AssemblyConfigurationUtils#getAssemblyConfigurationOrCreateDefault
* Fix #358: Prometheus is enabled by default, opt-out via AB_PROMETHEUS_OFF required to disable (like in FMP)
* Fix #384: Enricher defined Container environment variables get merged with vars defined in Image Build Configuration

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
