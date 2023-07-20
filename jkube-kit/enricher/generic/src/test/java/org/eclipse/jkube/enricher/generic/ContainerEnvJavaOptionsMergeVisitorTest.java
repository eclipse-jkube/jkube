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
package org.eclipse.jkube.enricher.generic;

import java.util.Collections;

import org.eclipse.jkube.kit.config.image.ImageConfiguration;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.enricher.generic.ContainerEnvJavaOptionsMergeTest.containerList;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContainerEnvJavaOptionsMergeVisitorTest {

  @SuppressWarnings("unused")
  private ImageConfiguration imageConfiguration;

  private KubernetesListBuilder kubernetesListBuilder;

  @BeforeEach
  void setUp() {
    kubernetesListBuilder = new KubernetesListBuilder();
    imageConfiguration = mock(ImageConfiguration.class,RETURNS_DEEP_STUBS);
  }

  @Test
  void noImageConfigurationsNoContainersShouldDoNothing() {
    // When
    kubernetesListBuilder.accept(new ContainerEnvJavaOptionsMergeEnricher.ContainerEnvJavaOptionsMergeVisitor(Collections.emptyList()));
    // Then
    assertThat(kubernetesListBuilder.build().getItems()).isEmpty();
  }

  @Test
  void imageConfigurationsNoContainersShouldDoNothing() {
    // When
    kubernetesListBuilder.accept(new ContainerEnvJavaOptionsMergeEnricher
        .ContainerEnvJavaOptionsMergeVisitor(Collections.singletonList(imageConfiguration)));
    // Then
    assertThat(kubernetesListBuilder.build().getItems()).isEmpty();
  }

  @Test
  void imageConfigurationAndContainersWithMatchingImageNameAndNoEnvShouldDoNothing() {
    // Given
    when(imageConfiguration.getName()).thenReturn("the-image:latest");
    initDeployments(new ContainerBuilder()
        .withImage("the-image:latest")
        .build());
    // When
    kubernetesListBuilder.accept(new ContainerEnvJavaOptionsMergeEnricher
        .ContainerEnvJavaOptionsMergeVisitor(Collections.singletonList(imageConfiguration)));
    // Then
    assertThat(kubernetesListBuilder.build().getItems()).asList()
        .hasSize(1)
        .flatExtracting("spec.template.spec.containers")
        .flatExtracting("env").isEmpty();
  }

  @Test
  void imageConfigurationWithJavaOptionsEnvAndContainersWithMatchingImageNameShouldDoNothing() {
    // Given
    when(imageConfiguration.getName()).thenReturn("the-image:latest");
    when(imageConfiguration.getBuild().getEnv()).thenReturn(Collections.singletonMap("JAVA_OPTIONS", "-DsomeOption"));

    initDeployments(new ContainerBuilder()
        .withImage("the-image:latest")
        .build());
    // When
    kubernetesListBuilder.accept(new ContainerEnvJavaOptionsMergeEnricher
        .ContainerEnvJavaOptionsMergeVisitor(Collections.singletonList(imageConfiguration)));
    // Then
    assertThat(kubernetesListBuilder.build().getItems()).asList()
        .hasSize(1)
        .flatExtracting("spec.template.spec.containers")
        .flatExtracting("env").isEmpty();
  }

  @Test
  void imageConfigurationWithJavaOptionsEnvAndContainersWithMatchingImageNameAndEnvShouldAdd() {
    // Given
    when(imageConfiguration.getName()).thenReturn("the-image:latest");
    when(imageConfiguration.getBuild().getEnv()).thenReturn(Collections.singletonMap("JAVA_OPTIONS", "-DsomeOption"));
    initDeployments(new ContainerBuilder()
        .withImage("the-image:latest")
        .addToEnv(new EnvVar("JAVA_OPTIONS", "-DotherOption", null))
        .addToEnv(new EnvVar("OTHER", "OTHER_VALUE", null))
        .build(), new ContainerBuilder()
        .withImage("other-image:latest")
        .addToEnv(new EnvVar("JAVA_OPTIONS", "-DnotMerged", null))
        .build());
    // When
    kubernetesListBuilder.accept(new ContainerEnvJavaOptionsMergeEnricher
        .ContainerEnvJavaOptionsMergeVisitor(Collections.singletonList(imageConfiguration)));
    // Then
    assertThat(containerList(kubernetesListBuilder)).asList()
        .hasSize(2)
        .filteredOn("image", "the-image:latest")
        .hasSize(1)
        .flatExtracting("env")
        .contains(
            new EnvVar("JAVA_OPTIONS", "-DsomeOption -DotherOption", null),
            new EnvVar("OTHER", "OTHER_VALUE", null)
        );
    assertThat(containerList(kubernetesListBuilder)).asList()
        .filteredOn("image", "other-image:latest")
        .hasSize(1)
        .flatExtracting("env")
        .contains(
            new EnvVar("JAVA_OPTIONS", "-DnotMerged", null)
        );
  }


  private void initDeployments(Container... containers) {
    // @formatter:off
    kubernetesListBuilder.addToItems(new DeploymentBuilder().withNewSpec()
        .withNewTemplate()
          .withNewSpec()
            .addToContainers(containers)
          .endSpec()
        .endTemplate()
      .endSpec().build());
    // @formatter:on
  }
}
