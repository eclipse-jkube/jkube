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

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.openshift.api.model.Project;
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
  public void enrichWithPropertiesInKubernetesShouldAddNamespace() {
    // Given
    properties.put("jkube.enricher.jkube-namespace.namespace", "example");
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    klb.addToItems(new NamespaceBuilder()
        .editOrNewMetadata().withName("name").withNamespace("to-be-overwritten").endMetadata()
        .editOrNewStatus().withPhase("active").endStatus().build());
    // When
    new DefaultNamespaceEnricher(context).enrich(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems()).hasSize(1);
    assertThat(klb.build().getItems().iterator().next())
        .hasFieldOrPropertyWithValue("metadata.namespace", "example");
  }
}
