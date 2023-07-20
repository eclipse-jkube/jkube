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
package org.eclipse.jkube.openliberty.enricher;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.openliberty.OpenLibertyUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.util.Collections;
import java.util.Properties;
import java.util.function.Supplier;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenLibertyHealthCheckEnricherTest {
  private JKubeEnricherContext context;
  private JavaProject javaProject;
  private Properties properties;
  private KubernetesListBuilder klb;

  @BeforeEach
  void setup() {
    properties = new Properties();
    ProcessorConfig processorConfig = new ProcessorConfig();
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

    context = mock(JKubeEnricherContext.class, RETURNS_DEEP_STUBS);
    javaProject = mock(JavaProject.class, RETURNS_DEEP_STUBS);
    when(context.getProject()).thenReturn(javaProject);
    when(context.getProperties()).thenReturn(properties);
    when(context.getConfiguration().getProcessorConfig()).thenReturn(processorConfig);
    when(javaProject.getProperties()).thenReturn(properties);
    when(javaProject.getBaseDirectory()).thenReturn(new File("/tmp/ignore"));
    when(javaProject.getOutputDirectory()).thenReturn(new File("/tmp/ignore"));
  }

  @Test
  void create_withNoMicroprofileDependency_shouldNotAddProbes() {
    // Given
    OpenLibertyHealthCheckEnricher openLibertyHealthCheckEnricher = new OpenLibertyHealthCheckEnricher(context);

    // When
    openLibertyHealthCheckEnricher.create(PlatformMode.kubernetes, klb);

    // Then
    assertNoProbesAdded();
  }

  @Test
  void create_withMicroprofileDependencyAndMicroprofileFeatureDisabled_shouldNotAddProbes() {
    // Given
    withMicroprofileDependency("5.0");
    OpenLibertyHealthCheckEnricher openLibertyHealthCheckEnricher = new OpenLibertyHealthCheckEnricher(context);

    // When
    openLibertyHealthCheckEnricher.create(PlatformMode.kubernetes, klb);

    // Then
    assertNoProbesAdded();
  }

  @Test
  void create_withMicroprofileDependencyAndMicroprofileFeatureEnabled_shouldAddProbes() {
    try (MockedStatic<OpenLibertyUtils> mockStatic = Mockito.mockStatic(OpenLibertyUtils.class)) {
      // Given
      mockStatic.when(() -> OpenLibertyUtils.hasAnyFeatureMatching(javaProject, "mpHealth-")).thenReturn(true);
      mockStatic.when(() -> OpenLibertyUtils.isMicroProfileHealthEnabled(javaProject)).thenReturn(true);
      withMicroprofileDependency("5.0");
      OpenLibertyHealthCheckEnricher openLibertyHealthCheckEnricher = new OpenLibertyHealthCheckEnricher(context);
      // When
      openLibertyHealthCheckEnricher.create(PlatformMode.kubernetes, klb);
      // Then
      assertProbesAdded("HTTP", "/health/live", "HTTP", "/health/ready", "HTTP", "/health/started");
    }
  }

  @Test
  void create_withLegacyMicroprofileDependencyAndMicroprofileFeatureEnabled_shouldOnlyAddLivenessReadinessProbes() {
    try (MockedStatic<OpenLibertyUtils> mockStatic = Mockito.mockStatic(OpenLibertyUtils.class)) {
      // Given
      mockStatic.when(() -> OpenLibertyUtils.hasAnyFeatureMatching(javaProject, "mpHealth-")).thenReturn(true);
      mockStatic.when(() -> OpenLibertyUtils.isMicroProfileHealthEnabled(javaProject)).thenReturn(true);
      withMicroprofileDependency("2.2");
      OpenLibertyHealthCheckEnricher openLibertyHealthCheckEnricher = new OpenLibertyHealthCheckEnricher(context);
      // When
      openLibertyHealthCheckEnricher.create(PlatformMode.kubernetes, klb);
      // Then
      assertProbesAdded("HTTP", "/health/live", "HTTP", "/health/ready", null, null);
      assertThat(getFirstContainerFromDeployment())
          .extracting(Container::getStartupProbe)
          .isNull();
    }
  }

  @Test
  void create_withMicroprofileEnabledAndEnricherConfiguration_shouldProbesAsConfigured() {
    try (MockedStatic<OpenLibertyUtils> mockStatic = Mockito.mockStatic(OpenLibertyUtils.class)) {
      // Given
      mockStatic.when(() -> OpenLibertyUtils.hasAnyFeatureMatching(javaProject, "mpHealth-")).thenReturn(true);
      mockStatic.when(() -> OpenLibertyUtils.isMicroProfileHealthEnabled(javaProject)).thenReturn(true);
      withMicroprofileDependency("5.0");
      properties.put("jkube.enricher.jkube-healthcheck-openliberty.scheme", "HTTPS");
      properties.put("jkube.enricher.jkube-healthcheck-openliberty.port", "8080");
      properties.put("jkube.enricher.jkube-healthcheck-openliberty.livenessPath", "/custom/health/live");
      properties.put("jkube.enricher.jkube-healthcheck-openliberty.livenessFailureThreshold", "5");
      properties.put("jkube.enricher.jkube-healthcheck-openliberty.livenessSuccessThreshold", "5");
      properties.put("jkube.enricher.jkube-healthcheck-openliberty.livenessInitialDelay", "5");
      properties.put("jkube.enricher.jkube-healthcheck-openliberty.livenessPeriodSeconds", "5");
      properties.put("jkube.enricher.jkube-healthcheck-openliberty.readinessPath", "/custom/health/ready");
      properties.put("jkube.enricher.jkube-healthcheck-openliberty.readinessFailureThreshold", "5");
      properties.put("jkube.enricher.jkube-healthcheck-openliberty.readinessSuccessThreshold", "5");
      properties.put("jkube.enricher.jkube-healthcheck-openliberty.readinessInitialDelay", "5");
      properties.put("jkube.enricher.jkube-healthcheck-openliberty.readinessPeriodSeconds", "5");
      properties.put("jkube.enricher.jkube-healthcheck-openliberty.startupPath", "/custom/health/startup");
      properties.put("jkube.enricher.jkube-healthcheck-openliberty.startupFailureThreshold", "5");
      properties.put("jkube.enricher.jkube-healthcheck-openliberty.startupSuccessThreshold", "5");
      properties.put("jkube.enricher.jkube-healthcheck-openliberty.startupInitialDelay", "5");
      properties.put("jkube.enricher.jkube-healthcheck-openliberty.startupPeriodSeconds", "5");
      OpenLibertyHealthCheckEnricher openLibertyHealthCheckEnricher = new OpenLibertyHealthCheckEnricher(context);
      // When
      openLibertyHealthCheckEnricher.create(PlatformMode.kubernetes, klb);
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
  }

  private void withMicroprofileDependency(String microProfileVersion) {
    when(javaProject.getDependenciesWithTransitive()).thenReturn(Collections.singletonList(Dependency.builder()
            .groupId("org.eclipse.microprofile")
            .artifactId("microprofile")
            .version(microProfileVersion)
        .build()));
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
    assertThat(deployment.getSpec().getTemplate().getSpec().getContainers()).hasSize(1);
    return deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
  }
}
