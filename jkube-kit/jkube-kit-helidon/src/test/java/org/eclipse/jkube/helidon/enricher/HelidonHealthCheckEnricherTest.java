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
package org.eclipse.jkube.helidon.enricher;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class HelidonHealthCheckEnricherTest {
  private JKubeEnricherContext context;
  private JavaProject javaProject;
  private Properties properties;
  private KubernetesListBuilder klb;

  @BeforeEach
  void setup() {
    properties = new Properties();
    klb = new KubernetesListBuilder();
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
  void create_withNoMicroprofileDependency_shouldNotAddProbes() {
    // Given
    HelidonHealthCheckEnricher helidonHealthCheckEnricher = new HelidonHealthCheckEnricher(context);

    // When
    helidonHealthCheckEnricher.create(PlatformMode.kubernetes, klb);

    // Then
    assertNoProbesAdded();
  }

  @Test
  void create_withMicroprofileDependencyAndMicroprofileFeatureDisabled_shouldNotAddProbes() {
    // Given
    withMicroprofileDependency("5.0");
    HelidonHealthCheckEnricher helidonHealthCheckEnricher = new HelidonHealthCheckEnricher(context);

    // When
    helidonHealthCheckEnricher.create(PlatformMode.kubernetes, klb);

    // Then
    assertNoProbesAdded();
  }

  @Test
  void create_withMicroprofileDependencyAndMicroprofileFeatureEnabled_shouldAddProbes() {
    // Given
    withHelidonHealth();
    withMicroprofileDependency("5.0");
    HelidonHealthCheckEnricher helidonHealthCheckEnricher = new HelidonHealthCheckEnricher(context);
    // When
    helidonHealthCheckEnricher.create(PlatformMode.kubernetes, klb);
    // Then
    assertProbesAdded("HTTP", "/health/live", "HTTP", "/health/ready", "HTTP", "/health/started");
  }

  @Test
  void create_withLegacyMicroprofileDependencyAndMicroprofileFeatureEnabled_shouldOnlyAddLivenessReadinessProbes() {
    // Given
    withHelidonHealth();
    withMicroprofileDependency("2.2");
    HelidonHealthCheckEnricher helidonHealthCheckEnricher = new HelidonHealthCheckEnricher(context);
    // When
    helidonHealthCheckEnricher.create(PlatformMode.kubernetes, klb);
    // Then
    assertProbesAdded("HTTP", "/health/live", "HTTP", "/health/ready", null, null);
    assertThat(getFirstContainerFromDeployment())
      .extracting(Container::getStartupProbe)
      .isNull();
  }

  @Test
  void create_withMicroprofileEnabledAndEnricherConfiguration_shouldProbesAsConfigured() {
    // Given
    withHelidonHealth();
    withMicroprofileDependency("5.0");
    properties.put("jkube.enricher.jkube-healthcheck-helidon.scheme", "HTTPS");
    properties.put("jkube.enricher.jkube-healthcheck-helidon.port", "8080");
    properties.put("jkube.enricher.jkube-healthcheck-helidon.livenessPath", "/custom/health/live");
    properties.put("jkube.enricher.jkube-healthcheck-helidon.livenessFailureThreshold", "5");
    properties.put("jkube.enricher.jkube-healthcheck-helidon.livenessSuccessThreshold", "5");
    properties.put("jkube.enricher.jkube-healthcheck-helidon.livenessInitialDelay", "5");
    properties.put("jkube.enricher.jkube-healthcheck-helidon.livenessPeriodSeconds", "5");
    properties.put("jkube.enricher.jkube-healthcheck-helidon.readinessPath", "/custom/health/ready");
    properties.put("jkube.enricher.jkube-healthcheck-helidon.readinessFailureThreshold", "5");
    properties.put("jkube.enricher.jkube-healthcheck-helidon.readinessSuccessThreshold", "5");
    properties.put("jkube.enricher.jkube-healthcheck-helidon.readinessInitialDelay", "5");
    properties.put("jkube.enricher.jkube-healthcheck-helidon.readinessPeriodSeconds", "5");
    properties.put("jkube.enricher.jkube-healthcheck-helidon.startupPath", "/custom/health/startup");
    properties.put("jkube.enricher.jkube-healthcheck-helidon.startupFailureThreshold", "5");
    properties.put("jkube.enricher.jkube-healthcheck-helidon.startupSuccessThreshold", "5");
    properties.put("jkube.enricher.jkube-healthcheck-helidon.startupInitialDelay", "5");
    properties.put("jkube.enricher.jkube-healthcheck-helidon.startupPeriodSeconds", "5");
    HelidonHealthCheckEnricher helidonHealthCheckEnricher = new HelidonHealthCheckEnricher(context);
    // When
    helidonHealthCheckEnricher.create(PlatformMode.kubernetes, klb);
    // Then
    assertProbesAdded("HTTPS", "/custom/health/live", "HTTPS", "/custom/health/ready", "HTTPS", "/custom/health/startup");
    assertThat(getFirstContainerFromDeployment())
      .hasFieldOrPropertyWithValue("livenessProbe.failureThreshold", 5)
      .hasFieldOrPropertyWithValue("livenessProbe.successThreshold", 5)
      .hasFieldOrPropertyWithValue("livenessProbe.initialDelaySeconds", 5)
      .hasFieldOrPropertyWithValue("livenessProbe.periodSeconds", 5)
      .hasFieldOrPropertyWithValue("livenessProbe.httpGet.port.IntVal", 8080)
      .hasFieldOrPropertyWithValue("readinessProbe.failureThreshold", 5)
      .hasFieldOrPropertyWithValue("readinessProbe.successThreshold", 5)
      .hasFieldOrPropertyWithValue("readinessProbe.initialDelaySeconds", 5)
      .hasFieldOrPropertyWithValue("readinessProbe.periodSeconds", 5)
      .hasFieldOrPropertyWithValue("readinessProbe.httpGet.port.IntVal", 8080)
      .hasFieldOrPropertyWithValue("startupProbe.failureThreshold", 5)
      .hasFieldOrPropertyWithValue("startupProbe.successThreshold", 5)
      .hasFieldOrPropertyWithValue("startupProbe.initialDelaySeconds", 5)
      .hasFieldOrPropertyWithValue("startupProbe.periodSeconds", 5)
      .hasFieldOrPropertyWithValue("startupProbe.httpGet.port.IntVal", 8080);
  }

  @Test
  void create_whenPortConfiguredViaApplicationYaml_thenUseThatPort() {
    // Given
    withHelidonHealth();
    withMicroprofileDependency("5.0");
    context = context.toBuilder()
        .project(context.getProject().toBuilder()
            .compileClassPathElement(Objects.requireNonNull(getClass().getResource("/custom-port-configuration")).getPath())
            .build())
        .build();
    HelidonHealthCheckEnricher helidonHealthCheckEnricher = new HelidonHealthCheckEnricher(context);

    // When
    helidonHealthCheckEnricher.create(PlatformMode.kubernetes, klb);

    // Then
    assertThat(getFirstContainerFromDeployment())
        .hasFieldOrPropertyWithValue("livenessProbe.httpGet.port.IntVal", 1337)
        .hasFieldOrPropertyWithValue("readinessProbe.httpGet.port.IntVal", 1337)
        .hasFieldOrPropertyWithValue("startupProbe.httpGet.port.IntVal", 1337);
  }

  @Test
  void create_whenPortConfiguredViaMicroprofileConfigProperties_thenUseThatPort() {
    // Given
    withHelidonHealth();
    withMicroprofileDependency("5.0");
    context = context.toBuilder()
        .project(context.getProject().toBuilder()
            .compileClassPathElement(Objects.requireNonNull(getClass().getResource("/custom-port-microprofile-configuration")).getPath())
            .build())
        .build();
    HelidonHealthCheckEnricher helidonHealthCheckEnricher = new HelidonHealthCheckEnricher(context);

    // When
    helidonHealthCheckEnricher.create(PlatformMode.kubernetes, klb);

    // Then
    assertThat(getFirstContainerFromDeployment())
        .hasFieldOrPropertyWithValue("livenessProbe.httpGet.port.IntVal", 1337)
        .hasFieldOrPropertyWithValue("readinessProbe.httpGet.port.IntVal", 1337)
        .hasFieldOrPropertyWithValue("startupProbe.httpGet.port.IntVal", 1337);
  }

  private void withMicroprofileDependency(String microProfileVersion) {
    final List<Dependency> deps = new ArrayList<>(javaProject.getDependencies());
    deps.add(Dependency.builder()
      .groupId("org.eclipse.microprofile").artifactId("microprofile").version(microProfileVersion).build());
    javaProject.setDependencies(deps);
    javaProject.setDependenciesWithTransitive(deps);
  }

  private void withHelidonHealth() {
    final List<Dependency> deps = new ArrayList<>(javaProject.getDependencies());
    deps.add(Dependency.builder()
      .groupId("io.helidon.health").artifactId("helidon-health").build());
    javaProject.setDependencies(deps);
    javaProject.setDependenciesWithTransitive(deps);
  }

  private void assertProbesAdded(String livenessScheme, String livenessPath, String readyScheme, String readyPath, String startedScheme, String startedPath) {
    Container container = getFirstContainerFromDeployment();
    assertProbe(container::getLivenessProbe, livenessScheme, livenessPath);
    assertProbe(container::getReadinessProbe, readyScheme, readyPath);
    assertProbe(container::getStartupProbe, startedScheme, startedPath);
  }

  private void assertNoProbesAdded() {
    Container container = getFirstContainerFromDeployment();
    assertThat(container.getLivenessProbe()).isNull();
    assertThat(container.getReadinessProbe()).isNull();
    assertThat(container.getStartupProbe()).isNull();
  }

  private void assertProbe(Supplier<Probe> probeSupplier, String scheme, String path) {
    if (scheme != null && path != null) {
      assertThat(probeSupplier.get())
        .hasFieldOrPropertyWithValue("httpGet.scheme", scheme)
        .hasFieldOrPropertyWithValue("httpGet.path", path);
    }
  }

  private Container getFirstContainerFromDeployment() {
    Deployment deployment = (Deployment) klb.buildFirstItem();
    AssertionsForInterfaceTypes.assertThat(deployment.getSpec().getTemplate().getSpec().getContainers()).hasSize(1);
    return deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
  }
}
