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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import mockit.Expectations;
import mockit.Mocked;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Test;

public class NameEnricherTest {

  @Mocked
  private JKubeEnricherContext context;

  private Properties properties;
  private ProcessorConfig processorConfig;
  private KubernetesListBuilder klb;

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Before
  public void setUp() {
    properties = new Properties();
    processorConfig = new ProcessorConfig();
    klb = new KubernetesListBuilder();
    // @formatter:off
    klb.addToItems(
        new ConfigMapBuilder().withNewMetadata().endMetadata().build(),
        new DeploymentBuilder().withNewMetadata().endMetadata().build(),
        new ReplicaSetBuilder().withNewMetadata().endMetadata().build(),
        new ReplicationControllerBuilder().withNewMetadata().endMetadata().build(),
        new NamespaceBuilder().build()
    );
    new Expectations() {{
      context.getProperties(); result = properties;
      context.getConfiguration().getProcessorConfig(); result = processorConfig;
      context.getGav().getSanitizedArtifactId(); result = "artifact-id";
    }};
    // @formatter:on
  }

  @Test
  public void createWithDefaultsInKubernetes() {
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
  public void createWithCustomNameInKubernetes() {
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
}
