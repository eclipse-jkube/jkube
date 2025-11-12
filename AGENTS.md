# Eclipse JKube - AI Agents Instructions

Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.

This file provides guidance to AI coding agents (GitHub Copilot, Claude Code, etc.) when working with code in this repository.

## Project Overview

Eclipse JKube is a collection of Maven and Gradle plugins that provide tools for building container images and deploying Java applications to Kubernetes and OpenShift. The project consists of:

- **Kubernetes Maven Plugin** - Maven plugin for Kubernetes deployments
- **OpenShift Maven Plugin** - Maven plugin for OpenShift deployments
- **Kubernetes Gradle Plugin** - Gradle plugin for Kubernetes deployments
- **OpenShift Gradle Plugin** - Gradle plugin for OpenShift deployments
- **JKube Kit** - The core engine that powers all plugins, providing reusable functionality

This is a refactored and rebranded version of the Fabric8 Maven Plugin and Docker Maven Plugin, with the core functionality extracted into JKube Kit to enable reuse across different build systems.

## Build & Development Commands

### Build Commands

The project uses Maven wrapper (`./mvnw`). Common commands:

```bash
# Full build (compile, test)
./mvnw clean install

# Build without tests
./mvnw clean install -DskipTests

# Run tests only
./mvnw test

# Run specific test
./mvnw test -Dtest=TestClassName

# Compile only
./mvnw clean compile

# Generate Javadocs
./mvnw clean install -Pjavadoc

# Build with JaCoCo test coverage
./mvnw clean install -Pjacoco
```

### Running JKube Plugin Commands

Example Maven plugin usage:

```bash
# Kubernetes Maven Plugin
mvn package k8s:build k8s:push k8s:resource k8s:apply

# OpenShift Maven Plugin
mvn package oc:build oc:resource oc:apply
```

Example Gradle plugin usage:

```bash
# Kubernetes Gradle Plugin
gradle build k8sBuild k8sPush k8sResource k8sApply

# OpenShift Gradle Plugin
gradle build ocBuild ocResource ocApply
```

## Technical Architecture

### High-Level Structure

The repository is organized into 4 main modules:

1. **jkube-kit/** - Core engine containing all reusable functionality
2. **kubernetes-maven-plugin/** - Kubernetes Maven plugin wrapper
3. **openshift-maven-plugin/** - OpenShift Maven plugin wrapper
4. **gradle-plugin/** - Gradle plugins for both Kubernetes and OpenShift

### JKube Kit Architecture

JKube Kit (`jkube-kit/`) is the heart of the project and contains:

#### Core Components

- **api/** - Core API and data models
- **common/** - Common utilities shared across all modules
- **common-maven/** - Maven-specific utilities
- **config/** - Configuration models
  - `config/image/` - Docker image configuration models
  - `config/resource/` - Kubernetes/OpenShift resource configuration models
  - `config/service/` - Service configuration models

#### Build Strategies

- **build/** - Build-related functionality
  - `build/api/` - Build API
  - `build/service/docker/` - Docker build strategy implementation
  - `build/service/jib/` - JIB build strategy implementation
  - `build/service/buildpacks/` - Buildpacks build strategy implementation

#### Extension Points

The project uses a plugin-based architecture with two main extension points:

1. **Generator Framework** (`generator/`)
   - Automatically generates Docker images by examining project information
   - Implementations: `java-exec/`, `karaf/`, `webapp/`, `dockerfile-simple/`
   - Each generator detects specific project types and generates appropriate configurations

2. **Enricher Framework** (`enricher/`)
   - Creates and enhances Kubernetes/OpenShift resources
   - `enricher/api/` - Enricher API
   - `enricher/generic/` - Generic enrichers (work for all project types)
   - `enricher/specific/` - Framework-specific enrichers

#### Framework Support Modules

JKube provides specialized support for popular Java frameworks:

- **jkube-kit-spring-boot/** - Spring Boot detection and configuration
- **jkube-kit-quarkus/** - Quarkus support
- **jkube-kit-micronaut/** - Micronaut support
- **jkube-kit-microprofile/** - MicroProfile support
- **jkube-kit-helidon/** - Helidon support
- **jkube-kit-openliberty/** - Open Liberty support
- **jkube-kit-vertx/** - Vert.x support
- **jkube-kit-wildfly-jar/** - WildFly JAR support
- **jkube-kit-smallrye/** - SmallRye support
- **jkube-kit-thorntail-v2/** - Thorntail v2 support

#### Additional Features

- **profile/** - Profiles combining generator and enricher configurations
- **helm/** - Helm chart generation support
- **resource/service/** - Resource management services
- **watcher/** - Watch mode for live updates
  - `watcher/api/` - Watcher API
  - `watcher/standard/` - Standard watcher implementations
- **remote-dev/** - Remote development support

### Plugin Architecture

The Maven and Gradle plugins are thin wrappers around JKube Kit:

- Plugins expose JKube Kit functionality through build system-specific tasks/goals
- Maven plugins use `common-maven` module for Maven-specific integration
- Gradle plugins use custom Gradle API adapters

### Integration Testing

- **kubernetes-maven-plugin/it/** - Maven plugin integration tests (Java 11+)
- **openshift-maven-plugin/it/** - OpenShift Maven plugin integration tests
- **gradle-plugin/it/** - Gradle plugin integration tests
- **jkube-kit/common-it/** - Common integration test utilities (Java 11+)
- Separate repository at https://github.com/eclipse-jkube/jkube-integration-tests for E2E tests

## Development Workflow

### Code Style

- Uses Lombok for reducing boilerplate - source requires delombok before Javadoc generation and releasing
- License headers required on all source files (EPL-2.0)
- Use `./mvnw com.mycila:license-maven-plugin:format` to add/update license headers

### Testing

- Unit tests run with `./mvnw test`
- Integration tests require Java 11+ and are in modules with `java-11` profile
- Test coverage measured with JaCoCo when using `-Pjacoco` profile

### Documentation

- Plugin documentation in `kubernetes-maven-plugin/doc/`, `openshift-maven-plugin/doc/`, and `gradle-plugin/doc/`
- Documentation source in AsciiDoc format (`src/main/asciidoc`)
- Generate docs with AsciiDoctor Maven plugin

### Key Dependencies

- Kubernetes Client: 6.14.0 (fabric8io kubernetes-client)
- Jackson: 2.20.1
- Helm Java: 0.0.16
- JUnit 5: 5.11.4
- AssertJ: 3.27.6
- Mockito: 4.6.1

### Java Version

- Target: Java 8 (jdk.version=1.8)
- Build requires Java 11+ for integration tests
- Current development uses Java 11

## Important Notes

### Windows Line Endings

The project handles YAML literal blocks with Windows line endings (see recent commit d768fb8fb). Be careful when modifying serialization code.

### Version Dependencies

Some dependencies must be kept in sync with Kubernetes Client:
- commons-compress (currently 1.28.0)
- commons-io (currently 2.19.0)

### Security

Always check for security vulnerabilities when modifying:
- Image build code (potential command injection)
- Resource templating (potential injection attacks)
- File handling (path traversal)

## Quickstarts

Example projects are available in:
- `quickstarts/maven/` - Maven-based examples
- `quickstarts/gradle/` - Gradle-based examples
- `quickstarts/kit/` - Custom generator/enricher examples

These are useful for testing changes and understanding plugin usage patterns.

## Release Process

Documented in BUILDING.md. Key points:
- Releases done from project fork
- Version set with `mvn versions:set`
- Quickstarts updated with `./scripts/quickstarts.sh version`
- Requires signing with GPG (`-Prelease` profile)
- Published to Maven Central via Sonatype Central Portal
