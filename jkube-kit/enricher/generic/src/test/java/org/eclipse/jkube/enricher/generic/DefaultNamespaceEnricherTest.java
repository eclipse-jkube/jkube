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

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.ProjectBuilder;
import mockit.Expectations;
import mockit.Mocked;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;

public class DefaultNamespaceEnricherTest {

  @Mocked
  private JKubeEnricherContext context;

  private Properties properties;
  private ProcessorConfig processorConfig;

  @Before
  public void setUp() {
    properties = new Properties();
    processorConfig = new ProcessorConfig();
    // @formatter:off
    new Expectations() {{
      context.getProperties(); result = properties;
      context.getConfiguration().getProcessorConfig(); result = processorConfig;
    }};
    // @formatter:on
  }

  @Test
  public void noNameShouldReturnEmpty() {
    // Given
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    // When
    new DefaultNamespaceEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems()).isEmpty();
  }

  @Test
  public void createWithPropertiesInKubernetesShouldAddNamespace() {
    // Given
    properties.put("jkube.enricher.jkube-namespace.namespace", "example");
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    // When
    new DefaultNamespaceEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems()).hasSize(1);
    assertThat(klb.build().getItems().iterator().next())
        .isInstanceOf(Namespace.class)
        .hasFieldOrPropertyWithValue("metadata.name", "example")
        .hasFieldOrPropertyWithValue("status.phase", "Active");
  }

  @Test
  public void createWithPropertiesInOpenShiftShouldAddProject() {
    // Given
    properties.put("jkube.enricher.jkube-namespace.namespace", "example");
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    // When
    new DefaultNamespaceEnricher(context).create(PlatformMode.openshift, klb);
    // Then
    assertThat(klb.build().getItems()).hasSize(1);
    assertThat(klb.build().getItems().iterator().next())
        .isInstanceOf(Project.class)
        .hasFieldOrPropertyWithValue("metadata.name", "example")
        .hasFieldOrPropertyWithValue("status.phase", "Active");
  }

  @Test
  public void createWithPropertiesAndConfigInKubernetesShouldAddConfigNamespace() {
    // Given
    properties.put("jkube.enricher.jkube-namespace.namespace", "example");
    processorConfig.getConfig().put("jkube-namespace", Collections.singletonMap("namespace", "config-example"));
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    // When
    new DefaultNamespaceEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems()).hasSize(1);
    assertThat(klb.build().getItems().iterator().next())
        .isInstanceOf(Namespace.class)
        .hasFieldOrPropertyWithValue("metadata.name", "config-example")
        .hasFieldOrPropertyWithValue("status.phase", "Active");
  }

  @Test
  public void enrichWithPropertiesInKubernetesShouldAddNamespaceWithStatus() {
    // Given
    properties.put("jkube.enricher.jkube-namespace.namespace", "example");
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    Namespace namespace = new NamespaceBuilder()
            .editOrNewMetadata().withName("name").withNamespace("to-be-overwritten").endMetadata()
            .editOrNewStatus().withPhase("active").endStatus().build();
    Deployment deployment = new DeploymentBuilder().withNewMetadata().withName("d1").endMetadata().build();
    klb.addToItems(namespace, deployment);
    // When
    new DefaultNamespaceEnricher(context).enrich(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems()).hasSize(2);
    assertThat(klb.build().getItems().get(1))
        .hasFieldOrPropertyWithValue("metadata.namespace", "example");
  }

  @Test
  public void enrichWithPropertiesInKubernetesShouldAddProjectWithStatus() {
    // Given
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    klb.addToItems(new ProjectBuilder()
            .withNewMetadata().withName("name").endMetadata()
            .withNewStatus().withPhase("active").endStatus().build());
    // When
    new DefaultNamespaceEnricher(context).enrich(PlatformMode.openshift, klb);
    // Then
    assertThat(klb.build().getItems()).hasSize(1);
    assertThat(klb.build().getItems().iterator().next())
            .hasFieldOrPropertyWithValue("metadata.namespace", null);
  }

  @Test
  public void enrichWithNamespaceFragmentWithNoStatus() {
    // Given
    final KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder();
    kubernetesListBuilder.addToItems(new NamespaceBuilder()
            .withNewMetadata().withName("test-jkube").endMetadata()
            .build());

    // When
    new DefaultNamespaceEnricher(context).enrich(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    assertThat(kubernetesListBuilder.build().getItems()).hasSize(1);
    assertThat(kubernetesListBuilder.build().getItems().iterator().next())
            .hasFieldOrPropertyWithValue("metadata.name", "test-jkube");
    assertNull(kubernetesListBuilder.build().getItems().get(0).getMetadata().getNamespace());
  }

  @Test
  public void enrichWithOpenShiftProjectFragmentWithNoStatus() {
    // Given
    final KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder();
    kubernetesListBuilder.addToItems(new ProjectBuilder()
            .withNewMetadata().withName("test-jkube").endMetadata()
            .build());

    // When
    new DefaultNamespaceEnricher(context).enrich(PlatformMode.openshift, kubernetesListBuilder);

    // Then
    assertThat(kubernetesListBuilder.build().getItems()).hasSize(1);
    assertThat(kubernetesListBuilder.build().getItems().iterator().next())
            .hasFieldOrPropertyWithValue("metadata.name", "test-jkube");
    assertNull(kubernetesListBuilder.build().getItems().get(0).getMetadata().getNamespace());
  }
}
