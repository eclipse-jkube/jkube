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

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import org.assertj.core.api.Condition;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NameEnricherTest {

  private JKubeEnricherContext context;

  private Properties properties;
  private KubernetesListBuilder klb;

  @BeforeEach
  void setUp() {
    properties = new Properties();
    ProcessorConfig processorConfig = new ProcessorConfig();
    klb = new KubernetesListBuilder();
    // @formatter:off
    klb.addToItems(
        new ConfigMapBuilder().withNewMetadata().endMetadata().build(),
        new DeploymentBuilder().withNewMetadata().endMetadata().build(),
        new ReplicaSetBuilder().withNewMetadata().endMetadata().build(),
        new ReplicationControllerBuilder().withNewMetadata().endMetadata().build(),
        new NamespaceBuilder().build()
    );
    // @formatter:on
    context = mock(JKubeEnricherContext.class, RETURNS_DEEP_STUBS);
    when(context.getProperties()).thenReturn(properties);
    when(context.getConfiguration().getProcessorConfig()).thenReturn(processorConfig);
    when(context.getGav().getSanitizedArtifactId()).thenReturn("artifact-id");
  }

  @Test
  void createWithDefaultsInKubernetes() {
    // When
    new NameEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    final Condition<Object> nonNull = new Condition<Object>("Not Null") {
      @Override
      public boolean matches(Object value) {
        return value != null;
      }
    };
    assertThat(klb.build().getItems())
        .hasSize(5)
        .extracting(HasMetadata::getMetadata)
        .filteredOn(nonNull)
        .extracting(ObjectMeta::getName)
        .containsExactly("artifact-id", "artifact-id", "artifact-id", "artifact-id");
  }

  @Test
  void createWithCustomNameInKubernetes() {
    // Given
    properties.put("jkube.enricher.jkube-name.name", "custom-name");
    // When
    new NameEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    final Condition<Object> nonNull = new Condition<Object>("Not Null") {
      @Override
      public boolean matches(Object value) {
        return value != null;
      }
    };
    assertThat(klb.build().getItems())
        .hasSize(5)
        .extracting(HasMetadata::getMetadata)
        .filteredOn(nonNull)
        .extracting(ObjectMeta::getName)
        .containsExactly("custom-name", "custom-name", "custom-name", "custom-name");
  }

  @Test
  void create_withAlreadyExistingName_shouldKeepExistingName() {
    // Given
    klb = new KubernetesListBuilder();
    klb.addToItems(new ServiceBuilder().withNewMetadata().withName("existing-name").endMetadata().build());
    // When
    new NameEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems())
        .singleElement()
        .hasFieldOrPropertyWithValue("metadata.name", "existing-name");
  }

  @Test
  void create_withCustomNameAndAlreadyExistingName_shouldOverrideExistingName() {
    // Given
    klb = new KubernetesListBuilder();
    properties.put("jkube.enricher.jkube-name.name", "custom-name");
    // When
    klb.addToItems(new ServiceBuilder().withNewMetadata().withName("existing-name").endMetadata().build());
    new NameEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems())
        .singleElement()
        .hasFieldOrPropertyWithValue("metadata.name", "custom-name");
  }
}
