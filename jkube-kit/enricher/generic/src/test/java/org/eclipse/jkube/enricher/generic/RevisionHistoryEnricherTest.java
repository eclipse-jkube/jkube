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

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RevisionHistoryEnricherTest {

    private JKubeEnricherContext context;
    @Before
    public void setUp() {
        context = mock(JKubeEnricherContext.class,RETURNS_DEEP_STUBS);
    }
    @Test
    public void testDefaultRevisionHistoryLimit() {
        // Given
        KubernetesListBuilder builder = new KubernetesListBuilder().addToItems(new DeploymentBuilder().build());

        RevisionHistoryEnricher enricher = new RevisionHistoryEnricher(context);

        // When
        enricher.create(PlatformMode.kubernetes, builder);

        // Then
        assertRevisionHistory(builder.build(), 2);
    }

    @Test
    public void testCustomRevisionHistoryLimit() {

        // Setup mock behaviour
        final String revisionNumber = "10";
        Configuration config = Configuration.builder().processorConfig(prepareEnricherConfig(revisionNumber)).build();
        when(context.getConfiguration()).thenReturn(config);


        // Given
        KubernetesListBuilder builder = new KubernetesListBuilder().addToItems(new DeploymentBuilder().build());

        RevisionHistoryEnricher enricher = new RevisionHistoryEnricher(context);

        // When
        enricher.create(PlatformMode.kubernetes, builder);

        // Then
        assertRevisionHistory(builder.build(), Integer.parseInt(revisionNumber));
    }

      private ProcessorConfig prepareEnricherConfig(final String revisionNumber) {
        return new ProcessorConfig(
            null,
            null,
            Collections.singletonMap("jkube-revision-history",
                Collections.singletonMap("limit",revisionNumber)));
      }

    private void assertRevisionHistory(KubernetesList list, Integer revisionNumber) {
        assertEquals(1, list.getItems().size());

        assertThat(list.getItems())
                .hasSize(1)
                .first()
                .hasFieldOrPropertyWithValue("spec.revisionHistoryLimit",revisionNumber);
    }

}
