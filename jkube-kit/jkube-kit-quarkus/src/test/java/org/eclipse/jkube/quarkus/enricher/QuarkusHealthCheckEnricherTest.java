/*
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 *
 *     https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.jkube.quarkus.enricher;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.groups.Tuple;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class QuarkusHealthCheckEnricherTest {

  private Properties properties;
  private KubernetesListBuilder klb;
  private JavaProject javaProject;
  private JKubeEnricherContext context;

  @BeforeEach
  void setUp() {
    properties = new Properties();
    klb = new KubernetesListBuilder();
    // @formatter:off
    klb.addToItems(new DeploymentBuilder()
        .editOrNewSpec()
          .editOrNewTemplate()
            .editOrNewMetadata()
              .withName("template-name")
            .endMetadata()
            .editOrNewSpec()
              .addNewContainer()
                .withImage("container/image")
              .endContainer()
            .endSpec()
          .endTemplate()
        .endSpec()
        .build());
    javaProject = JavaProject.builder()
      .properties(properties)
      .outputDirectory(new File("/tmp/ignored"))
      .dependenciesWithTransitive(new ArrayList<>())
      .build();
    context = JKubeEnricherContext.builder()
      .log(new KitLogger.SilentLogger())
      .project(javaProject)
      .processorConfig(new ProcessorConfig())
      .build();
  }

  @Test
  void create_withCustomPath_shouldReturnCustomPath() {
    // Given
    withSmallryeExtension("0.0.0-SNAPSHOT");
    properties.put("jkube.enricher.jkube-healthcheck-quarkus.path", "/my-custom-path");
    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertLivenessReadinessStartupProbes(klb, tuple("HTTP", "/my-custom-path/live", "HTTP", "/my-custom-path/ready", null, null));
  }

  @Test
  void create_withQuarkus1_1_0AndAbsoluteHealthPathProperty_shouldIgnoreLeadingSlash() {
    // Given
    withSmallryeExtension("1.1.0.Final");
    withQuarkus("1.1.0.Final");
    properties.put("quarkus.http.non-application-root-path", "/not-ignored");
    properties.put("quarkus.smallrye-health.root-path", "/absolute");
    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertLivenessReadinessStartupProbes(klb, tuple("HTTP", "/not-ignored/absolute/live", "HTTP", "/not-ignored/absolute/ready", null, null));
  }

  @Nested
  @DisplayName("create with Quarkus 1.0.0")
  class Quarkus1 {

    @BeforeEach
    void withQuarkus1() {
      withSmallryeExtension("1.0.0.Final");
      withQuarkus("1.0.0.Final");
    }

    @Test
    @DisplayName("and defaults, should return default liveness and readiness probes")
    void andDefaults_shouldReturnDefaults() {
      // When
      new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
      // Then
      assertLivenessReadinessStartupProbes(klb, tuple("HTTP", "/health/live", "HTTP", "/health/ready", null, null));
    }

    @Test
    @DisplayName("and absolute path properties, should return relative liveness and readiness probes")
    void andAbsolutePathProperties_shouldReturnRelativeProbes() {
      // Given
      properties.put("quarkus.smallrye-health.liveness-path", "/absolute/liveness");
      properties.put("quarkus.smallrye-health.readiness-path", "/absolute/readiness");
      properties.put("quarkus.smallrye-health.startup-path", "/absolute/startup");
      // When
      new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
      // Then
      assertLivenessReadinessStartupProbes(klb,
        tuple("HTTP", "/health/absolute/liveness", "HTTP", "/health/absolute/readiness", null, null));

    }
  }

  @Nested
  @DisplayName("create, with Quarkus 2")
  class WithQuarkus2 {
    @BeforeEach
    void withQuarkus2() {
      withSmallryeExtension("2.0.0.0.Final");
      withQuarkus("2.0.0.0.Final");
    }

    @Test
    @DisplayName("and defaults, should return prefixed defaults")
    void andDefaults_shouldReturnPrefixedDefaults() {
      // When
      new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
      // Then
      assertLivenessReadinessStartupProbes(klb, tuple("HTTP", "/q/health/live", "HTTP", "/q/health/ready", null, null));
    }

    @Test
    @DisplayName("and absolute health path property, should return customized paths")
    void andAbsoluteHealthPathProperty_shouldReturnCustomizedPaths() {
      // Given
      properties.put("quarkus.http.root-path", "/ignored-for-health");
      properties.put("quarkus.http.non-application-root-path", "/ignored-for-health");
      properties.put("quarkus.smallrye-health.root-path", "/absolute/health");
      // When
      new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
      // Then
      assertLivenessReadinessStartupProbes(klb, tuple("HTTP", "/absolute/health/live", "HTTP", "/absolute/health/ready", null, null));
    }

    @Test
    @DisplayName("and application properties, should return customized paths")
    void andApplicationProperties_shouldReturnCustomizedPaths() {
      // Given
      properties.put("quarkus.http.root-path", "/");
      properties.put("quarkus.http.non-application-root-path", "not-app");
      properties.put("quarkus.smallrye-health.root-path", "health");
      properties.put("quarkus.smallrye-health.readiness-path", "im-ready");
      properties.put("quarkus.smallrye-health.liveness-path", "im-alive");
      properties.put("quarkus.smallrye-health.startup-path", "im-started");
      // When
      new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
      // Then
      assertLivenessReadinessStartupProbes(klb, tuple("HTTP", "/not-app/health/im-alive", "HTTP", "/not-app/health/im-ready", null, null));
    }

    @Test
    @DisplayName("and custom root property, should return customized paths")
    void andCustomRootProperty_shouldReturnCustomizedPaths() {
      // Given
      properties.put("quarkus.http.root-path", "/root");
      properties.put("quarkus.http.non-application-root-path", "q");
      properties.put("quarkus.smallrye-health.root-path", "health");
      properties.put("quarkus.smallrye-health.liveness-path", "liveness");
      // When
      new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
      // Then
      assertLivenessReadinessStartupProbes(klb, tuple("HTTP", "/root/q/health/liveness", "HTTP", "/root/q/health/ready", null, null));
    }

  }

  @Test
  @DisplayName("and custom absolute non-root property, should return customized paths")
  void create_withQuarkus2_1AndAbsoluteNonApplicationRootPathProperty_shouldReturnCustomizedPaths() {
    // Given
      withSmallryeExtension("2.1.3.7.Final");
      withQuarkus("2.1.3.7.Final");
    properties.put("quarkus.http.root-path", "/ignored-for-health");
    properties.put("quarkus.http.non-application-root-path", "/absolute");
    properties.put("quarkus.smallrye-health.root-path", "health");
    properties.put("quarkus.smallrye-health.liveness-path", "liveness");
    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertLivenessReadinessStartupProbes(klb, tuple("HTTP", "/absolute/health/liveness", "HTTP", "/absolute/health/ready", "HTTP", "/absolute/health/started"));
  }

  @Nested
  @DisplayName("create with Quarkus 3")
  class Quarkus3 {

    @BeforeEach
    void withQuarkus3() {
      withSmallryeExtension("3.1337.3.Final");
      withQuarkus("3.1337.3.Final");
    }

    @Test
    @DisplayName("and defaults, should return default liveness, readiness, and startup probes")
    void andDefaults_shouldReturnDefaults() {
      // When
      new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
      // Then
      assertLivenessReadinessStartupProbes(klb,
        tuple("HTTP", "/q/health/live", "HTTP", "/q/health/ready", "HTTP", "/q/health/started"));
    }

    @Test
    @DisplayName("and absolute path properties, should return absolute liveness, readiness, and startup probes")
    void andAbsolutePathProperties_shouldReturnAbsoluteProbes() {
      // Given
      properties.put("quarkus.smallrye-health.liveness-path", "/absolute/liveness");
      properties.put("quarkus.smallrye-health.readiness-path", "/absolute/readiness");
      properties.put("quarkus.smallrye-health.startup-path", "/absolute/startup");
      // When
      new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
      // Then
      assertLivenessReadinessStartupProbes(klb,
        tuple("HTTP", "/absolute/liveness", "HTTP", "/absolute/readiness", "HTTP", "/absolute/startup"));

    }
  }

  @Test
  void create_withNoSmallrye_shouldNotAddProbes() {
    // When
    new QuarkusHealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertContainers(klb)
        .extracting(
            "livenessProbe", "readinessProbe", "startupProbe")
        .containsExactly(tuple(null, null, null));
  }

  @Test
  void create_whenPortConfiguredViaApplicationProperties_thenUseThatPort() {
    // Given
    withSmallryeExtension("2.1.3.7.Final");
    context = context.toBuilder()
        .project(context.getProject().toBuilder()
            .compileClassPathElement(Objects.requireNonNull(getClass().getResource("/utils-test/config/properties")).getPath())
            .build())
        .build();
    QuarkusHealthCheckEnricher helidonHealthCheckEnricher = new QuarkusHealthCheckEnricher(context);

    // When
    helidonHealthCheckEnricher.create(PlatformMode.kubernetes, klb);

    // Then
    assertContainers(klb)
        .extracting("livenessProbe.httpGet.port.IntVal", "readinessProbe.httpGet.port.IntVal", "startupProbe.httpGet.port.IntVal")
        .containsExactly(tuple(1337, 1337, 1337));;
  }

  private AbstractListAssert<?, List<? extends Container>, Container, ObjectAssert<Container>> assertContainers(
      KubernetesListBuilder kubernetesListBuilder) {
    return assertThat(kubernetesListBuilder.build().getItems())
        .hasSize(1)
        .extracting("spec", DeploymentSpec.class)
        .extracting("template", PodTemplateSpec.class)
        .extracting("spec", PodSpec.class)
        .flatExtracting(PodSpec::getContainers);
  }

  private void assertLivenessReadinessStartupProbes(KubernetesListBuilder kubernetesListBuilder, Tuple... values) {
    assertContainers(kubernetesListBuilder)
            .extracting(
                    "livenessProbe.httpGet.scheme", "livenessProbe.httpGet.path",
                    "readinessProbe.httpGet.scheme", "readinessProbe.httpGet.path",
                    "startupProbe.httpGet.scheme", "startupProbe.httpGet.path")
            .containsExactly(values);
  }

  private void withQuarkus(String quarkusVersion) {
    final List<Dependency> deps = new ArrayList<>(javaProject.getDependencies());
    deps.add(Dependency.builder()
      .groupId("io.quarkus").artifactId("quarkus-universe-bom").version(quarkusVersion).build());
    javaProject.setDependencies(deps);
    javaProject.setDependenciesWithTransitive(deps);
  }

  private void withSmallryeExtension(String quarkusVersion) {
    final List<Dependency> deps = new ArrayList<>(javaProject.getDependencies());
    deps.add(Dependency.builder()
      .groupId("io.quarkus").artifactId("quarkus-smallrye-health").version(quarkusVersion).build());
    javaProject.setDependencies(deps);
    javaProject.setDependenciesWithTransitive(deps);
  }

}
