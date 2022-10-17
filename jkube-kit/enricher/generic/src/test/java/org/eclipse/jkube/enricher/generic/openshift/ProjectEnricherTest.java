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

import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.openshift.api.model.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

class ProjectEnricherTest {

    private JKubeEnricherContext context;

    @BeforeEach
    void setExpectations() {
        context = mock(JKubeEnricherContext.class, RETURNS_DEEP_STUBS);
    }

    @Test
    void create_whenKubernetesListHasNamespace_thenNamespaceConvertedToProject() {
        // Given
        final KubernetesListBuilder klb = new KubernetesListBuilder()
            .addToItems(new NamespaceBuilder().withNewMetadata().withName("foo").endMetadata());
        // When
        new ProjectEnricher(context).create(PlatformMode.openshift, klb);
        // Then
        assertThat(klb.build())
            .extracting(KubernetesList::getItems)
            .asList()
            .singleElement()
            .isInstanceOf(Project.class)
            .hasFieldOrPropertyWithValue("metadata.name", "foo");
    }

    @Test
    void create_whenKubernetesListHasNamespaceWithSpec_thenNamespaceConvertedToProject() {
        // Given
        final KubernetesListBuilder klb = new KubernetesListBuilder()
            .addToItems(new NamespaceBuilder().withNewMetadata().withName("foo").endMetadata()
                .withNewSpec().addToFinalizers("hoo").endSpec());
        // When
        new ProjectEnricher(context).create(PlatformMode.openshift, klb);
        // Then
        assertThat(klb.build())
            .extracting(KubernetesList::getItems)
            .asList()
            .singleElement()
            .isInstanceOf(Project.class)
            .extracting("spec.finalizers")
            .asList().first().isEqualTo("hoo");
    }

    @Test
    void create_whenKubernetesListHasNamespaceWithStatus_thenNamespaceConvertedToProject() {
        // Given
        final KubernetesListBuilder klb = new KubernetesListBuilder()
            .addToItems(new NamespaceBuilder().withNewMetadata().withName("foo").endMetadata()
                .withNewStatus().withPhase("Complete").endStatus());
        // When
        new ProjectEnricher(context).create(PlatformMode.openshift, klb);
        // Then
        assertThat(klb.build())
            .extracting(KubernetesList::getItems)
            .asList()
            .singleElement()
            .isInstanceOf(Project.class)
            .hasFieldOrPropertyWithValue("status.phase", "Complete");
    }

    @Test
    void create_whenKubernetesListDoesNotHasNamespace_thenDoesNotNamespaceConvertedToProject() {
        // Given
        final KubernetesListBuilder klb = new KubernetesListBuilder()
            .addToItems(new ServiceBuilder().withNewMetadata().endMetadata());
        // When
        new ProjectEnricher(context).create(PlatformMode.openshift, klb);
        // Then
        assertThat(klb.build())
            .extracting(KubernetesList::getItems)
            .asList()
            .singleElement()
            .isInstanceOf(Service.class)
            .hasFieldOrPropertyWithValue("metadata.name", null);
    }
}

