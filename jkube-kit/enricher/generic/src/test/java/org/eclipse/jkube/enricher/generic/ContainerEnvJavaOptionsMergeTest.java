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
package org.eclipse.jkube.enricher.generic;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ContainerEnvJavaOptionsMergeTest {

  @SuppressWarnings("unused")
  private ImageConfiguration imageConfiguration;
  private JKubeEnricherContext context;
  private ContainerEnvJavaOptionsMergeEnricher containerEnvJavaOptionsMergeEnricher;
  private KubernetesListBuilder kubernetesListBuilder;
  private Properties properties;

  @Before
  public void setUp() {
    context = mock(JKubeEnricherContext.class,RETURNS_DEEP_STUBS);
    imageConfiguration = mock(ImageConfiguration.class,RETURNS_DEEP_STUBS);
    containerEnvJavaOptionsMergeEnricher = new ContainerEnvJavaOptionsMergeEnricher(context);
    kubernetesListBuilder = new KubernetesListBuilder();
    properties = new Properties();
    kubernetesListBuilder.addToItems(new DeploymentBuilder().withNewSpec()
        .withNewTemplate()
          .withNewSpec()
            .addToContainers(new ContainerBuilder()
                .withImage("the-image:latest")
                .addToEnv(new EnvVar("JAVA_OPTIONS", "val-from-container", null))
                .build())
          .endSpec()
        .endTemplate()
      .endSpec().build());
    when(context.getConfiguration()).thenReturn(Configuration.builder().image(imageConfiguration).build());
    when(context.getProperties()).thenReturn(properties);
  }

  @Test
  public void enrichWithDefaultsShouldMergeValues() {
    // Given
    when(imageConfiguration.getName()).thenReturn("the-image:latest");
    when(imageConfiguration.getBuild().getEnv()).thenReturn(Collections.singletonMap("JAVA_OPTIONS", "val-from-ic"));
    // When
    containerEnvJavaOptionsMergeEnricher.enrich(PlatformMode.kubernetes, kubernetesListBuilder);
    // Then
    assertThat(containerList(kubernetesListBuilder))
        .flatExtracting("env")
        .hasSize(1)
        .containsOnly(new EnvVar("JAVA_OPTIONS", "val-from-ic val-from-container", null));
  }

  @Test
  public void enrichWithDisabledShouldDoNothing() {
    // Given
    properties.put("jkube.enricher.jkube-container-env-java-options.disable", "true");
    // When
    containerEnvJavaOptionsMergeEnricher.enrich(PlatformMode.kubernetes, kubernetesListBuilder);
    // Then
    assertThat(containerList(kubernetesListBuilder))
        .flatExtracting("env")
        .hasSize(1)
        .containsOnly(new EnvVar("JAVA_OPTIONS", "val-from-container", null));
  }

  @Test
  public void enrichWithNullBuildInImageConfiguration() {
    // Given
    when(imageConfiguration.getName()).thenReturn("the-image:latest");
    when(imageConfiguration.getBuild()).thenReturn(null);
    // When
    containerEnvJavaOptionsMergeEnricher.enrich(PlatformMode.kubernetes, kubernetesListBuilder);
    // Then
    assertThat(containerList(kubernetesListBuilder))
            .flatExtracting("env")
            .hasSize(1)
            .containsOnly(new EnvVar("JAVA_OPTIONS", "val-from-container", null));
  }

  @Test
  public void enrichWithNullEnvInImageConfiguration() {
    // Given
    when(imageConfiguration.getName()).thenReturn("the-image:latest");
    when(imageConfiguration.getBuild().getEnv()).thenReturn(null);
    // When
    containerEnvJavaOptionsMergeEnricher.enrich(PlatformMode.kubernetes, kubernetesListBuilder);
    // Then
    assertThat(containerList(kubernetesListBuilder))
            .flatExtracting("env")
            .hasSize(1)
            .containsOnly(new EnvVar("JAVA_OPTIONS", "val-from-container", null));
  }

  static List<Container> containerList(KubernetesListBuilder kubernetesListBuilder) {
    return kubernetesListBuilder.build().getItems().stream()
        .map(Deployment.class::cast)
        .flatMap(d -> d.getSpec().getTemplate().getSpec().getContainers().stream())
        .collect(Collectors.toList());
  }

}
