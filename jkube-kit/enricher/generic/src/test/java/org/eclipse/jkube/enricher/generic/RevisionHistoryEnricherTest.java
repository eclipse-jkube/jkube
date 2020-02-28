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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jayway.jsonpath.matchers.JsonPathMatchers;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.maven.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.maven.enricher.api.model.Configuration;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import mockit.Expectations;
import mockit.Mocked;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Collections;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class RevisionHistoryEnricherTest {

    @Mocked
    private JKubeEnricherContext context;

    @Test
    public void testDefaultRevisionHistoryLimit() throws JsonProcessingException {
        // Given
        KubernetesListBuilder builder = new KubernetesListBuilder()
                .addNewDeploymentItem()
                .endDeploymentItem();

        RevisionHistoryEnricher enricher = new RevisionHistoryEnricher(context);

        // When
        enricher.create(PlatformMode.kubernetes, builder);

        // Then
        assertRevisionHistory(builder.build(), Configs.asInt(RevisionHistoryEnricher.Config.limit.def()));
    }

    @Test
    public void testCustomRevisionHistoryLimit() throws JsonProcessingException {

        // Setup mock behaviour
        final String revisionNumber = "10";
        new Expectations() {{
            Configuration config = new Configuration.Builder().processorConfig(prepareEnricherConfig(revisionNumber)).build();
            context.getConfiguration(); result = config;
        }};

        // Given
        KubernetesListBuilder builder = new KubernetesListBuilder()
                .addNewDeploymentItem()
                .endDeploymentItem();

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
                    Collections.singletonMap(
                            RevisionHistoryEnricher.DEFAULT_NAME,
                            new TreeMap(Collections.singletonMap(
                                    RevisionHistoryEnricher.Config.limit.name(),
                                    revisionNumber)
                            )
                    )
                );
    }

    private void assertRevisionHistory(KubernetesList list, Integer revisionNumber) throws JsonProcessingException {
        assertEquals(list.getItems().size(),1);

        String kubeJson = ResourceUtil.toJson(list.getItems().get(0));
        assertThat(kubeJson, JsonPathMatchers.isJson());
        assertThat(kubeJson, JsonPathMatchers.hasJsonPath("$.spec.revisionHistoryLimit", Matchers.equalTo(revisionNumber)));
    }

}
