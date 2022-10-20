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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultServiceEnricherExternalServiceTest {
  private DefaultServiceEnricher enricher;
  private KubernetesListBuilder kubernetesListBuilder;

  @Before
  public void setUp() {
    Properties properties = new Properties();
    List<ImageConfiguration> images = new ArrayList<>();
    images.add(ImageConfiguration.builder()
        .name("test-image")
        .build(BuildConfiguration.builder()
            .from("foo:latest")
            .port("8080")
            .build())
        .build());
    JKubeEnricherContext context = mock(JKubeEnricherContext.class, RETURNS_DEEP_STUBS);
    when(context.getProperties()).thenReturn(properties);
    when(context.getConfiguration().getImages()).thenReturn(images);
    when(context.getGav().getSanitizedArtifactId()).thenReturn("artifact-id");
    when(context.getConfiguration().getResource()).thenReturn(null);
    when(context.getConfiguration().getImages()).thenReturn(images);
    kubernetesListBuilder = new KubernetesListBuilder();
    kubernetesListBuilder.addToItems(createNewExternalServiceFragment());
    kubernetesListBuilder.addToItems(createNewExternalDeploymentFragment());
    enricher = new DefaultServiceEnricher(context);
  }

  @After
  public void tearDown() {
    kubernetesListBuilder = null;
  }

  @Test
  public void create_whenNoServiceFragmentExceptExternalServicePresent_thenGenerateDefaultService() {
    // Given + When
    enricher.create(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    assertContainsResourcesWithName("external", "external", "artifact-id");
  }

  @Test
  public void create_whenServiceFragmentAndExternalServicePresent_thenMergeDefaultServiceWithFragment() {
    // Given
    kubernetesListBuilder.addToItems(new ServiceBuilder()
        .withNewMetadata()
        .withName("artifact-id")
        .addToLabels("foo", "bar")
        .endMetadata()
        .build());

    // Whe
    enricher.create(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    assertContainsResourcesWithName("external", "external", "artifact-id");
    Service mainService = getMainServiceFromBuilder();
    assertThat(mainService)
        .isNotNull()
        .hasFieldOrPropertyWithValue("metadata.name", "artifact-id")
        .hasFieldOrPropertyWithValue("metadata.labels.foo", "bar")
        .extracting(Service::getSpec)
        .extracting(ServiceSpec::getPorts)
        .asList()
        .first(InstanceOfAssertFactories.type(ServicePort.class))
        .hasFieldOrPropertyWithValue("name", "http")
        .hasFieldOrPropertyWithValue("port", 8080);
  }

  private void assertContainsResourcesWithName(String ...names) {
    assertThat(kubernetesListBuilder.buildItems())
        .hasSize(names.length)
        .map(HasMetadata::getMetadata)
        .map(ObjectMeta::getName)
        .containsExactlyInAnyOrder(names);
  }

  private Service createNewExternalServiceFragment() {
    return new ServiceBuilder()
        .withNewMetadata()
        .withName("external")
        .endMetadata()
        .withNewSpec()
        .addToSelector("app", "external")
        .addNewPort()
        .withProtocol("TCP")
        .withPort(80)
        .withTargetPort(new IntOrString(9390))
        .endPort()
        .endSpec()
        .build();
  }

  private Deployment createNewExternalDeploymentFragment() {
    return new DeploymentBuilder()
        .withNewMetadata()
        .withName("external")
        .addToLabels("app", "external")
        .endMetadata()
        .withNewSpec()
        .withReplicas(1)
        .withNewSelector()
        .addToMatchLabels("app", "external")
        .endSelector()
        .withNewTemplate()
        .withNewSpec()
        .addNewContainer()
        .withName("external")
        .withImage("external:latest")
        .addNewPort()
        .withContainerPort(80)
        .endPort()
        .endContainer()
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
  }

  private Service getMainServiceFromBuilder() {
    return (Service) kubernetesListBuilder.buildItems().stream()
        .filter(h -> h.getMetadata().getName().equals("artifact-id"))
        .findFirst()
        .orElse(null);
  }
}
