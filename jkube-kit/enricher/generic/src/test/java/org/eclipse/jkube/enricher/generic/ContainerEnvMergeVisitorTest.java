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

import org.eclipse.jkube.kit.config.image.ImageConfiguration;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.enricher.generic.ContainerEnvJavaOptionsMergeTest.containerList;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ContainerEnvMergeVisitorTest {

  @SuppressWarnings("unused")
  @Mocked
  private ImageConfiguration imageConfiguration;

  private KubernetesListBuilder kubernetesListBuilder;

  @Before
  public void setUp() {
    kubernetesListBuilder = new KubernetesListBuilder();
  }

  @Test
  public void noImageConfigurationsNoContainersShouldDoNothing() {
    // When
    kubernetesListBuilder.accept(new AbstractContainerEnvMergeEnricher.
            ContainerEnvMergeVisitor(Collections.emptyList(), ContainerEnvJavaOptionsMergeEnricher.ENV_KEY));
    // Then
    assertThat(kubernetesListBuilder.build().getItems()).isEmpty();
  }

  @Test
  public void imageConfigurationsNoContainersShouldDoNothing() {
    // When
    kubernetesListBuilder.accept(new AbstractContainerEnvMergeEnricher.
            ContainerEnvMergeVisitor(Collections.singletonList(imageConfiguration), 
                    ContainerEnvJavaOptionsMergeEnricher.ENV_KEY));
    // Then
    assertThat(kubernetesListBuilder.build().getItems()).isEmpty();
  }

  @Test
  public void imageConfigurationAndContainersWithMatchingImageNameAndNoEnvShouldDoNothing() {
    // Given
    // @formatter:off
    new Expectations() {{
      imageConfiguration.getName(); result = "the-image:latest";
    }};
    // @formatter:on
    initDeployments(new ContainerBuilder()
        .withImage("the-image:latest")
        .build());
    // When
    kubernetesListBuilder.accept(new AbstractContainerEnvMergeEnricher.
            ContainerEnvMergeVisitor(Collections.singletonList(imageConfiguration), 
                    ContainerEnvJavaOptionsMergeEnricher.ENV_KEY));
    // Then
    assertThat(kubernetesListBuilder.build().getItems()).asList()
        .hasSize(1)
        .flatExtracting("spec.template.spec.containers")
        .flatExtracting("env").isEmpty();
  }

  @Test
  public void imageConfigurationWithJavaOptionsEnvAndContainersWithMatchingImageNameShouldDoNothing() {
    // Given
    // @formatter:off
    new Expectations() {{
      imageConfiguration.getName(); result = "the-image:latest";
      imageConfiguration.getBuild().getEnv(); result = Collections.singletonMap("JAVA_OPTIONS", "-DsomeOption");
    }};
    // @formatter:on
    initDeployments(new ContainerBuilder()
        .withImage("the-image:latest")
        .build());
    // When
    kubernetesListBuilder.accept(new AbstractContainerEnvMergeEnricher.
            ContainerEnvMergeVisitor(Collections.singletonList(imageConfiguration), 
                    ContainerEnvJavaOptionsMergeEnricher.ENV_KEY));
    // Then
    assertThat(kubernetesListBuilder.build().getItems()).asList()
        .hasSize(1)
        .flatExtracting("spec.template.spec.containers")
        .flatExtracting("env").isEmpty();
  }

  @Test
  public void imageConfigurationWithJavaOptionsEnvAndContainersWithMatchingImageNameAndEnvShouldAdd() {
    // Given
    // @formatter:off
    new Expectations() {{
      imageConfiguration.getName(); result = "the-image:latest";
      imageConfiguration.getBuild().getEnv(); result = Collections.singletonMap("JAVA_OPTIONS", "-DsomeOption");
    }};
    // @formatter:on
    initDeployments(new ContainerBuilder()
        .withImage("the-image:latest")
        .addToEnv(new EnvVar("JAVA_OPTIONS", "-DotherOption", null))
        .addToEnv(new EnvVar("OTHER", "OTHER_VALUE", null))
        .build(), new ContainerBuilder()
        .withImage("other-image:latest")
        .addToEnv(new EnvVar("JAVA_OPTIONS", "-DnotMerged", null))
        .build());
    // When
    kubernetesListBuilder.accept(new AbstractContainerEnvMergeEnricher.
            ContainerEnvMergeVisitor(Collections.singletonList(imageConfiguration), 
                    ContainerEnvJavaOptionsMergeEnricher.ENV_KEY));
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
