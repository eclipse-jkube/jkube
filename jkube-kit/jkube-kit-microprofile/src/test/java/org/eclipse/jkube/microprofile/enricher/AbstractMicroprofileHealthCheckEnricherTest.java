/**
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
package org.eclipse.jkube.microprofile.enricher;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collections;
import java.util.Properties;
import java.util.function.Supplier;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbstractMicroprofileHealthCheckEnricherTest {
  private JKubeEnricherContext context;
  private JavaProject javaProject;
  private Properties properties;
  private KubernetesListBuilder klb;

  @BeforeEach
  public void setUp() {
    context = mock(JKubeEnricherContext.class, RETURNS_DEEP_STUBS);
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
  void create_withNoMicroprofileImpl_shouldNotAddProbes() {
    // Given
    AbstractMicroprofileHealthCheckEnricher abstractMicroprofileHealthCheckEnricher = createEnricher();

    // When
    abstractMicroprofileHealthCheckEnricher.create(PlatformMode.kubernetes, klb);

    // Then
    assertNoProbesAdded();
  }

  @Test
  void create_withMicroprofileImplDependency_shouldAddProbes() {
    // Given
    withMicroprofileImplDependency();
    withMicroprofileHealthTransitiveDependency("3.1");
    AbstractMicroprofileHealthCheckEnricher abstractMicroprofileHealthCheckEnricher = createEnricher();
    // When
    abstractMicroprofileHealthCheckEnricher.create(PlatformMode.kubernetes, klb);
    // Then
    assertProbesAdded("HTTP", "/health/live", "HTTP", "/health/ready", "HTTP", "/health/started");
  }

  @Test
  void create_withMicroprofileImplDependencyAndOldMicroprofileHealthTransitiveDependency_shouldOnlyAddLivenessReadinessProbes() {
    // Given
    withMicroprofileImplDependency();
    withMicroprofileHealthTransitiveDependency("2.2");
    AbstractMicroprofileHealthCheckEnricher abstractMicroprofileHealthCheckEnricher = createEnricher();
    // When
    abstractMicroprofileHealthCheckEnricher.create(PlatformMode.kubernetes, klb);
    // Then
    assertProbesAdded("HTTP", "/health/live", "HTTP", "/health/ready", null, null);
    assertThat(getFirstContainerFromDeployment())
        .extracting(Container::getStartupProbe)
        .isNull();
  }


  @Test
  void create_withMicroprofileHealthAndMicroprofileImplWithEnricherConfiguration_shouldProbesAsConfigured() {
    // Given
    withMicroprofileImplDependency();
    withMicroprofileHealthTransitiveDependency("3.1");
    properties.put("jkube.enricher.jkube-healthcheck-microprofile-fooimpl.scheme", "HTTPS");
    properties.put("jkube.enricher.jkube-healthcheck-microprofile-fooimpl.port", "8080");
    properties.put("jkube.enricher.jkube-healthcheck-microprofile-fooimpl.livenessPath", "/custom/health/live");
    properties.put("jkube.enricher.jkube-healthcheck-microprofile-fooimpl.livenessFailureThreshold", "5");
    properties.put("jkube.enricher.jkube-healthcheck-microprofile-fooimpl.livenessSuccessThreshold", "5");
    properties.put("jkube.enricher.jkube-healthcheck-microprofile-fooimpl.livenessInitialDelay", "5");
    properties.put("jkube.enricher.jkube-healthcheck-microprofile-fooimpl.livenessPeriodSeconds", "5");
    properties.put("jkube.enricher.jkube-healthcheck-microprofile-fooimpl.readinessPath", "/custom/health/ready");
    properties.put("jkube.enricher.jkube-healthcheck-microprofile-fooimpl.readinessFailureThreshold", "5");
    properties.put("jkube.enricher.jkube-healthcheck-microprofile-fooimpl.readinessSuccessThreshold", "5");
    properties.put("jkube.enricher.jkube-healthcheck-microprofile-fooimpl.readinessInitialDelay", "5");
    properties.put("jkube.enricher.jkube-healthcheck-microprofile-fooimpl.readinessPeriodSeconds", "5");
    properties.put("jkube.enricher.jkube-healthcheck-microprofile-fooimpl.startupPath", "/custom/health/startup");
    properties.put("jkube.enricher.jkube-healthcheck-microprofile-fooimpl.startupFailureThreshold", "5");
    properties.put("jkube.enricher.jkube-healthcheck-microprofile-fooimpl.startupSuccessThreshold", "5");
    properties.put("jkube.enricher.jkube-healthcheck-microprofile-fooimpl.startupInitialDelay", "5");
    properties.put("jkube.enricher.jkube-healthcheck-microprofile-fooimpl.startupPeriodSeconds", "5");
    AbstractMicroprofileHealthCheckEnricher abstractMicroprofileHealthCheckEnricher = createEnricher();
    // When
    abstractMicroprofileHealthCheckEnricher.create(PlatformMode.kubernetes, klb);
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

  private void withMicroprofileHealthTransitiveDependency(String microProfileVersion) {
    when(javaProject.getDependenciesWithTransitive()).thenReturn(Collections.singletonList(Dependency.builder()
        .groupId("org.eclipse.microprofile.health")
        .artifactId("microprofile-health-api")
        .version(microProfileVersion)
        .build()));
  }

  private void withMicroprofileImplDependency() {
    when(javaProject.getDependencies()).thenReturn(Collections.singletonList(Dependency.builder()
        .groupId("org.example")
        .artifactId("microprofile-fooimpl")
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
    AssertionsForInterfaceTypes.assertThat(deployment.getSpec().getTemplate().getSpec().getContainers()).hasSize(1);
    return deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
  }

  private AbstractMicroprofileHealthCheckEnricher createEnricher() {
    return new AbstractMicroprofileHealthCheckEnricher(context, "jkube-healthcheck-microprofile-fooimpl") {
      @Override
      protected boolean shouldAddProbe() {
        return JKubeProjectUtil.hasDependency(getContext().getProject(), "org.example", "microprofile-fooimpl");
      }
    };
  }
}
