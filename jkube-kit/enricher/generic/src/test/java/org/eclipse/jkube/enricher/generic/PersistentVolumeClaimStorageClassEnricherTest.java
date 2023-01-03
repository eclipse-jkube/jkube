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
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PersistentVolumeClaimStorageClassEnricherTest {
  private JKubeEnricherContext context;

  @BeforeEach
  void setUp() {
    context = mock(JKubeEnricherContext.class, RETURNS_DEEP_STUBS);
  }

  @ParameterizedTest
  @ValueSource(strings = {"jkube-persistentvolumeclaim-storageclass", "jkube-volume-permission"})
  void enrich_withPersistentVolumeClaim_shouldAddStorageClassToSpec(String enricher) {
    // Given
    Properties properties = new Properties();
    properties.put("jkube.enricher." + enricher + ".defaultStorageClass", "standard");
    when(context.getProperties()).thenReturn(properties);
    PersistentVolumeClaimStorageClassEnricher volumePermissionEnricher = new PersistentVolumeClaimStorageClassEnricher(context);
    KubernetesListBuilder klb = new KubernetesListBuilder();
    klb.addToItems(createNewPersistentVolumeClaim());

    // When
    volumePermissionEnricher.enrich(PlatformMode.kubernetes, klb);

    // Then
    assertThat(klb.buildItems())
        .singleElement(InstanceOfAssertFactories.type(PersistentVolumeClaim.class))
        .hasFieldOrPropertyWithValue("spec.storageClassName", "standard")
        .extracting("metadata.annotations")
        .asInstanceOf(InstanceOfAssertFactories.map(String.class, Object.class))
        .isNullOrEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"jkube-persistentvolumeclaim-storageclass", "jkube-volume-permission"})
  void enrich_withPersistentVolumeClaimAndUseAnnotationEnabled_shouldAddStorageClassAnnotation(String enricher) {
    // Given
    Properties properties = new Properties();
    properties.put("jkube.enricher." + enricher + ".defaultStorageClass", "standard");
    properties.put("jkube.enricher." + enricher + ".useStorageClassAnnotation", "true");
    when(context.getProperties()).thenReturn(properties);
    PersistentVolumeClaimStorageClassEnricher volumePermissionEnricher = new PersistentVolumeClaimStorageClassEnricher(context);
    KubernetesListBuilder klb = new KubernetesListBuilder();
    klb.addToItems(createNewPersistentVolumeClaim());

    // When
    volumePermissionEnricher.enrich(PlatformMode.kubernetes, klb);

    // Then
    assertThat(klb.buildItems())
        .singleElement(InstanceOfAssertFactories.type(PersistentVolumeClaim.class))
        .hasFieldOrPropertyWithValue("metadata.annotations", Collections.singletonMap("volume.beta.kubernetes.io/storage-class", "standard"))
        .hasFieldOrPropertyWithValue("spec.storageClassName", null);
  }

  private PersistentVolumeClaim createNewPersistentVolumeClaim() {
    return new PersistentVolumeClaimBuilder()
        .withNewMetadata().withName("pv1").endMetadata()
        .withNewSpec().endSpec()
        .build();
  }
}
