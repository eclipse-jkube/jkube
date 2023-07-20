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
package org.eclipse.jkube.thorntail.v2.enricher;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

import org.eclipse.jkube.kit.common.util.ProjectClassLoaders;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ThorntailV2HealthCheckEnricherTest {
  private JKubeEnricherContext context;
  private Properties properties;
  private KubernetesListBuilder klb;

  @BeforeEach
  void setUp() {
    context = mock(JKubeEnricherContext.class,RETURNS_DEEP_STUBS);
    properties = new Properties();
    ProcessorConfig processorConfig = new ProcessorConfig();
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
    when(context.getProperties()).thenReturn(properties);
    when(context.getConfiguration().getProcessorConfig()).thenReturn(processorConfig);
    when(context.hasDependency("io.thorntail", "monitor")).thenReturn(true);
    when(context.getProjectClassLoaders()).thenReturn( new ProjectClassLoaders(new URLClassLoader(new URL[0], ThorntailV2HealthCheckEnricherTest.class.getClassLoader())));
  }

  @Test
  void createWithDefaultsInKubernetes() {
    // When
    new ThorntailV2HealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems())
        .hasSize(1)
        .extracting("spec", DeploymentSpec.class)
        .extracting("template", PodTemplateSpec.class)
        .extracting("spec", PodSpec.class)
        .flatExtracting(PodSpec::getContainers)
        .extracting(
            "livenessProbe.initialDelaySeconds", "livenessProbe.httpGet.scheme", "livenessProbe.httpGet.path",
            "readinessProbe.initialDelaySeconds", "readinessProbe.httpGet.scheme", "readinessProbe.httpGet.path", "readinessProbe.httpGet.port.intVal")
        .containsExactly(tuple(180, "HTTP", "/health", 10, "HTTP", "/health", 8080));
  }

  @Test
  void createWithCustomValuesInKubernetes() {
    // Given
    properties.put("jkube.enricher.jkube-healthcheck-thorntail-v2.scheme", "HTTPS");
    properties.put("jkube.enricher.jkube-healthcheck-thorntail-v2.port", "8082");
    properties.put("jkube.enricher.jkube-healthcheck-thorntail-v2.path", "/my-custom-path");
    // When
    new ThorntailV2HealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems())
        .hasSize(1)
        .extracting("spec", DeploymentSpec.class)
        .extracting("template", PodTemplateSpec.class)
        .extracting("spec", PodSpec.class)
        .flatExtracting(PodSpec::getContainers)
        .extracting(
            "livenessProbe.initialDelaySeconds", "livenessProbe.httpGet.scheme", "livenessProbe.httpGet.path",
            "readinessProbe.initialDelaySeconds", "readinessProbe.httpGet.scheme", "readinessProbe.httpGet.path", "readinessProbe.httpGet.port.intVal")
        .containsExactly(tuple(180, "HTTPS", "/my-custom-path", 10, "HTTPS", "/my-custom-path", 8082));
  }

  @Test
  void createWithThorntailSpecificPropertiesInKubernetes() {
    // Given
    System.setProperty("thorntail.http.port", "1337");
    try {
      // When
      new ThorntailV2HealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
      // Then
      assertThat(klb.build().getItems())
          .hasSize(1)
          .extracting("spec", DeploymentSpec.class)
          .extracting("template", PodTemplateSpec.class)
          .extracting("spec", PodSpec.class)
          .flatExtracting(PodSpec::getContainers)
          .extracting(
              "livenessProbe.initialDelaySeconds", "livenessProbe.httpGet.scheme", "livenessProbe.httpGet.path",
              "readinessProbe.initialDelaySeconds", "readinessProbe.httpGet.scheme", "readinessProbe.httpGet.path", "readinessProbe.httpGet.port.intVal")
          .containsExactly(tuple(180, "HTTP", "/health", 10, "HTTP", "/health", 1337));
    } finally {
      System.clearProperty("thorntail.http.port");
    }
  }

  @Test
  void createWithNoThorntailDependency() {
    // Given
    when(context.hasDependency("io.thorntail", "monitor")).thenReturn(false);
    // When
    new ThorntailV2HealthCheckEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems())
        .hasSize(1)
        .extracting("spec", DeploymentSpec.class)
        .extracting("template", PodTemplateSpec.class)
        .extracting("spec", PodSpec.class)
        .flatExtracting(PodSpec::getContainers)
        .extracting(
            "livenessProbe", "readinessProbe")
        .containsExactly(tuple(null, null));
  }
}
