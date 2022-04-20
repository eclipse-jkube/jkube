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

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.ProjectStatusBuilder;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.EnricherContext;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

public class ProjectEnricherTest {

    private EnricherContext context;

    public void setExpectations(Properties properties, ResourceConfig resourceConfig) {
        context = mock(JKubeEnricherContext.class, RETURNS_DEEP_STUBS);
    }

    @Test
    public void create_whenKubernetesListHasNamespace_thenNamespaceConvertedToProject() {
        // Given
        Properties properties = new Properties();
        setExpectations(properties, new ResourceConfig());
        final KubernetesListBuilder klb = new KubernetesListBuilder();
        klb.addToItems(new NamespaceBuilder().withNewMetadata().withName("foo").endMetadata());
        // When
        new ProjectEnricher((JKubeEnricherContext) context).create(PlatformMode.openshift, klb);
        // Then
        assertThat(klb.build())
                .extracting(KubernetesList::getItems)
                .asList()
                .hasSize(1)
                .first()
                .isInstanceOf(Project.class)
                .hasFieldOrPropertyWithValue("metadata.name", "foo");
    }

    @Test
    public void create_whenKubernetesListHasNamespaceWithSpec_thenNamespaceConvertedToProject() {
        // Given
        Properties properties = new Properties();
        setExpectations(properties, new ResourceConfig());
        final KubernetesListBuilder klb = new KubernetesListBuilder();
        klb.addToItems(new NamespaceBuilder().withNewMetadata().withName("foo").endMetadata()
                .withNewSpec().addNewFinalizer("hoo").endSpec());
        // When
        new ProjectEnricher((JKubeEnricherContext) context).create(PlatformMode.openshift, klb);
        // Then
        assertThat(klb.build())
                .extracting(KubernetesList::getItems)
                .asList()
                .hasSize(1)
                .first()
                .isInstanceOf(Project.class)
                .extracting("spec.finalizers")
                .asList().first().isEqualTo("hoo");
    }

    @Test
    public void create_whenKubernetesListHasNamespaceWithStatus_thenNamespaceConvertedToProject() {
        // Given
        Properties properties = new Properties();
        setExpectations(properties, new ResourceConfig());
        final KubernetesListBuilder klb = new KubernetesListBuilder();
        klb.addToItems(new NamespaceBuilder().withNewMetadata().withName("foo").endMetadata()
                .withNewStatus().withPhase("Complete").endStatus());
        // When
        new ProjectEnricher((JKubeEnricherContext) context).create(PlatformMode.openshift, klb);
        // Then
        assertThat(klb.build())
                .extracting(KubernetesList::getItems)
                .asList()
                .hasSize(1)
                .first()
                .isInstanceOf(Project.class)
                .hasFieldOrPropertyWithValue("status.phase", "Complete");
    }

    @Test
    public void create_whenKubernetesListDoesNotHasNamespace_thenDoesNotNamespaceConvertedToProject() {
        // Given
        Properties properties = new Properties();
        setExpectations(properties, new ResourceConfig());
        final KubernetesListBuilder klb = new KubernetesListBuilder();
        klb.addToItems(new NamespaceBuilder().withNewMetadata().endMetadata());
        // When
        new ProjectEnricher((JKubeEnricherContext) context).create(PlatformMode.openshift, klb);
        // Then
        assertThat(klb.build())
                .extracting(KubernetesList::getItems)
                .asList()
                .hasSize(1)
                .first()
                .isInstanceOf(Project.class)
                .hasFieldOrPropertyWithValue("metadata.name", null);
    }
}