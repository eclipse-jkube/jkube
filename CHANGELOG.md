# Changes

This document main purpose is to list changes which might affect backwards compatibility. It will not list all releases as Eclipse Jkube is build in a continous delivery fashion.

We use semantic versioning in some slight variation until our feature set has stabilized and the missing pieces has been filled in:

* The `MAJOR_VERSION` is kept to `0`
* The `MINOR_VERSION` changes when there is an API or configuration change which is not fully backward compatible.
* The `PATCH_LEVEL` is used for regular CD releases which add new features and bug fixes.

After this we will switch probably to real [Semantic Versioning 2.0.0](http://semver.org/)

### 0.2-SNAPSHOT

### 0.1.1 (14-02-2020)
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

### 0.1.0 (19-12-2019)
* Initial release
