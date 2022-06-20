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

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.EnricherContext;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
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
    private Properties properties;

    @Before
    public void setUp() throws Exception {
        config = new HashMap<>();
        properties = new Properties();
        context = JKubeEnricherContext.builder()
            .processorConfig(new ProcessorConfig(null, null, config))
            .log(new KitLogger.SilentLogger())
            .image(ImageConfiguration.builder()
                .name("helloworld")
                .build(BuildConfiguration.builder()
                    .port("8080")
                    .build()).build())
            .project(JavaProject.builder()
                .properties(properties)
                .groupId("group")
                .artifactId("artifact-id")
                .build())
            .build();
    }

    @Test
    public void checkReplicaCount() {
        givenReplicaCountInEnricherConfig(String.valueOf(3));
        enrichAndAssert(3, "IfNotPresent");
    }

    @Test
    public void checkDefaultReplicaCount() {
        givenReplicaCountInEnricherConfig(String.valueOf(1));
        enrichAndAssert(1, "IfNotPresent");
    }

    @Test
    public void create_withImagePullPolicyConfigured_shouldUseConfiguredImagePullPolicy() {
        // Given
        properties.put("jkube.enricher.jkube-controller.pullPolicy", "Never");

        // When + Then
        enrichAndAssert(1, "Never");
    }

    @Test
    public void create_withImagePullPolicyProperty_shouldUseConfiguredImagePullPolicy() {
        // Given
        properties.put("jkube.imagePullPolicy", "Never");

        // When + Then
        enrichAndAssert(1, "Never");
    }

    @Test
    public void create_withReplicasConfigured_shouldUseConfiguredReplicas() {
        // Given
        properties.put("jkube.enricher.jkube-controller.replicaCount", "5");

        // When + Then
        enrichAndAssert(5, "IfNotPresent");
    }

    protected void enrichAndAssert(int replicaCount, String imagePullPolicy) {
        // Enrich
        DefaultControllerEnricher controllerEnricher = new DefaultControllerEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder();
        controllerEnricher.create(PlatformMode.kubernetes, builder);

        // Validate that the generated resource contains
        KubernetesList list = builder.build();
        assertEquals(1, list.getItems().size());

        assertThat(list.getItems())
            .hasSize(1)
            .first()
            .asInstanceOf(InstanceOfAssertFactories.type(Deployment.class))
            .hasFieldOrPropertyWithValue("metadata.name", "artifact-id")
            .hasFieldOrPropertyWithValue("spec.replicas", replicaCount)
            .extracting(Deployment::getSpec)
            .extracting(DeploymentSpec::getTemplate)
            .extracting(PodTemplateSpec::getSpec)
            .extracting(PodSpec::getContainers)
            .asList()
            .first(InstanceOfAssertFactories.type(Container.class))
            .hasFieldOrPropertyWithValue("imagePullPolicy", imagePullPolicy);
    }

    private void givenReplicaCountInEnricherConfig(String value) {
        final Map<String, Object> controllerConfig = new TreeMap<>();
        controllerConfig.put("replicaCount", value);
        config.put("jkube-controller", controllerConfig);
    }

}
