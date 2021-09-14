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
package org.eclipse.jkube.enricher.generic.openshift;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import mockit.Expectations;
import mockit.Mocked;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class ImageChangeTriggerEnricherTest {
  @Mocked
  private JKubeEnricherContext context;

  @Test
  public void create_shouldAddImageChangeTriggers_whenDeploymentConfigPresent() {
    // Given
    Properties properties = new Properties();
    properties.put("jkube.internal.effective.platform.mode", "OPENSHIFT");
    new Expectations() {{
      context.getProperties();
      result = properties;
      context.getProcessingInstructions();
      result = Collections.singletonMap("IMAGECHANGE_TRIGGER", "test-container");
    }};
    ImageChangeTriggerEnricher imageChangeTriggerEnricher = new ImageChangeTriggerEnricher(context);
    KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder();
    kubernetesListBuilder.addToItems(new DeploymentConfigBuilder()
      .withNewMetadata().withName("test-dc").endMetadata()
      .withNewSpec()
      .withNewTemplate()
        .withNewSpec()
        .addNewContainer()
        .withName("test-container")
        .withImage("test-user/test-container:latest")
        .endContainer()
        .endSpec()
      .endTemplate()
      .endSpec());

    // When
    imageChangeTriggerEnricher.create(PlatformMode.openshift, kubernetesListBuilder);

    // Then
    List<HasMetadata> items = kubernetesListBuilder.buildItems();
    assertThat(items)
      .hasSize(1)
      .asList().element(0)
      .extracting("spec.triggers")
      .asList()
      .hasSize(1)
      .element(0)
      .extracting("imageChangeParams")
      .hasFieldOrPropertyWithValue("automatic", true)
      .extracting("from")
      .hasFieldOrPropertyWithValue("kind", "ImageStreamTag")
      .hasFieldOrPropertyWithValue("name", "test-container:latest")
      .hasFieldOrPropertyWithValue("namespace", null);

  }
}
