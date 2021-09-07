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

import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import mockit.Mocked;
import org.junit.Test;

/**
 * @author dgaur
 */
public class PortNameEnricherTest {

  @Mocked
  private JKubeEnricherContext context;

  @Test
  public void defaultConfigurationInKubernetesShouldEnrichWithDefaults() {
    // Given
    final KubernetesListBuilder klb = initKubernetesList();
    // When
    new PortNameEnricher(context).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.build().getItems())
        .hasSize(1)
        .extracting("template", PodTemplateSpec.class)
        .extracting("spec", PodSpec.class)
        .flatExtracting(PodSpec::getContainers)
        .flatExtracting(Container::getPorts)
        .hasSize(3)
        .contains(new ContainerPortBuilder().withContainerPort(80).withName("http").withProtocol("TCP").build())
        .contains(new ContainerPortBuilder().withContainerPort(8443).withName("https").withProtocol("TCP").build())
        .contains(new ContainerPortBuilder().withContainerPort(313373).withProtocol("TCP").build());
  }

  @Test
  public void defaultConfigurationInOpenShiftShouldEnrichWithDefaults() {
    // Given
    final KubernetesListBuilder klb = initKubernetesList();
    // When
    new PortNameEnricher(context).create(PlatformMode.openshift, klb);
    // Then
    assertThat(klb.build().getItems())
        .hasSize(1)
        .extracting("template", PodTemplateSpec.class)
        .extracting("spec", PodSpec.class)
        .flatExtracting(PodSpec::getContainers)
        .flatExtracting(Container::getPorts)
        .hasSize(3)
        .contains(new ContainerPortBuilder().withContainerPort(80).withName("http").withProtocol("TCP").build())
        .contains(new ContainerPortBuilder().withContainerPort(8443).withName("https").withProtocol("TCP").build())
        .contains(new ContainerPortBuilder().withContainerPort(313373).withProtocol("TCP").build());
  }

  private static KubernetesListBuilder initKubernetesList() {
    // @formatter:off
    final PodTemplateBuilder ptb = new PodTemplateBuilder()
        .withNewMetadata().withName("test-pod")
        .endMetadata()
        .withNewTemplate()
        .withNewSpec()
          .addNewContainer()
            .withName("test-port-enricher")
            .withImage("test-image")
            .addNewPort()
              .withContainerPort(80)
              .withProtocol("TCP")
            .endPort()
            .addNewPort()
              .withContainerPort(8443)
              .withProtocol("TCP")
            .endPort()
            .addNewPort()
              .withContainerPort(313373)
              .withProtocol("TCP")
            .endPort()
          .endContainer()
        .endSpec()
        .endTemplate();
    // @formatter:on
    return new KubernetesListBuilder().addToPodTemplateItems(ptb.build());
  }
}
