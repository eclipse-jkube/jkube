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
package org.eclipse.jkube.thorntail.v2.enricher;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

import org.eclipse.jkube.kit.common.SystemMock;
import org.eclipse.jkube.kit.common.util.ProjectClassLoaders;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ThorntailV2HealthCheckEnricherTest {

  @Mocked
  private JKubeEnricherContext context;

  private Properties properties;
  private ProcessorConfig processorConfig;
  private KubernetesListBuilder klb;

  @Before
  public void setUp() {
    properties = new Properties();
    processorConfig = new ProcessorConfig();
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
    new Expectations() {{
      context.getProperties(); result = properties;
      context.getConfiguration().getProcessorConfig(); result = processorConfig;
      context.hasDependency("io.thorntail", "monitor"); result = true;
      context.getProjectClassLoaders(); result =
          new ProjectClassLoaders(new URLClassLoader(new URL[0], ThorntailV2HealthCheckEnricherTest.class.getClassLoader()));
    }};
    // @formatter:on
  }

  @Test
  public void createWithDefaultsInKubernetes() {
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
  public void createWithCustomValuesInKubernetes() {
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
  public void createWithThorntailSpecificPropertiesInKubernetes() {
    // Given
    new SystemMock().put("thorntail.http.port", "1337");
    final ThorntailV2HealthCheckEnricher thorntailV2HealthCheckEnricher = new ThorntailV2HealthCheckEnricher(context);
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
  }

  @Test
  public void createWithNoThorntailDependency() {
    // Given
    // @formatter:off
    new Expectations() {{
      context.hasDependency("io.thorntail", "monitor"); result = false;
      context.getProjectClassLoaders(); minTimes = 0;
    }};
    // @formatter:on
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
