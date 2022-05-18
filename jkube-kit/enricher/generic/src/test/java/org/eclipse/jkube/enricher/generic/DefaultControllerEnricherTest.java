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
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.EnricherContext;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author kamesh
 */
public class DefaultControllerEnricherTest {

    private Map<String, Map<String, Object>> config;
    private EnricherContext context;

    @Before
    public void setUp() throws Exception {
        config = new HashMap<>();
        context = JKubeEnricherContext.builder()
            .processorConfig(new ProcessorConfig(null, null, config))
            .log(new KitLogger.SilentLogger())
            .resources(ResourceConfig.builder().build())
            .image(ImageConfiguration.builder()
                .name("helloworld")
                .build(BuildConfiguration.builder()
                    .port("8080")
                    .build()).build())
            .project(JavaProject.builder()
                .properties(new Properties())
                .groupId("group")
                .artifactId("artifact-id")
                .build())
            .build();
    }

    @Test
    public void checkReplicaCount() throws Exception {
        enrichAndAssert(3);
    }

    @Test
    public void checkDefaultReplicaCount() throws Exception {
        enrichAndAssert(1);
    }

    protected void enrichAndAssert(int replicaCount) throws Exception {
        final Map<String, Object> controllerConfig = new TreeMap<>();
        controllerConfig.put("replicaCount", String.valueOf(replicaCount));
        config.put("jkube-controller", controllerConfig);

        // Enrich
        DefaultControllerEnricher controllerEnricher = new DefaultControllerEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder();
        controllerEnricher.create(PlatformMode.kubernetes, builder);

        // Validate that the generated resource contains
        KubernetesList list = builder.build();
        assertEquals(1, list.getItems().size());

        String json = ResourceUtil.toJson(list.getItems().get(0));
        assertThat(list.getItems())
                .hasSize(1)
                .first()
                .hasFieldOrPropertyWithValue("spec.replicas", replicaCount);
    }

}
