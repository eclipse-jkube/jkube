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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * @author kamesh
 */
class DefaultControllerEnricherTest {

    private Map<String, Map<String, Object>> config;
    private EnricherContext context;
    private Properties properties;

    @BeforeEach
    void setUp() throws Exception {
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
    void checkReplicaCount() {
        givenReplicaCountInEnricherConfig(String.valueOf(3));

        // When
        KubernetesListBuilder builder = enrich();

        // Then
        assertReplicas(builder, 3);
        assertImagePullPolicy(builder, "IfNotPresent");
    }

    @Test
    void checkDefaultReplicaCount() {
        givenReplicaCountInEnricherConfig(String.valueOf(1));

        // When
        KubernetesListBuilder builder = enrich();

        // Then
        assertReplicas(builder, 1);
        assertImagePullPolicy(builder, "IfNotPresent");
    }

    @DisplayName("create with")
    @ParameterizedTest(name = "configured {0}, should use configured {0} ({2})")
    @MethodSource("data")
    void create_with(String description, String property, String propertyValue, int expectedReplicas, String expectedImagePullPolicy) {
        // Given
        properties.put(property, propertyValue);
        // When
        KubernetesListBuilder builder = enrich();
        // Then
        assertReplicas(builder, expectedReplicas);
        assertImagePullPolicy(builder, expectedImagePullPolicy);
    }

    static Stream<Arguments> data() {
      return Stream.of(
          arguments("image pull policy", "jkube.enricher.jkube-controller.pullPolicy", "Never", 1, "Never"),
          arguments("image pull policy property", "jkube.imagePullPolicy", "Never", 1, "Never"),
          arguments("replicas", "jkube.enricher.jkube-controller.replicaCount", "5", 5, "IfNotPresent"));
    }

    private KubernetesListBuilder enrich() {
        DefaultControllerEnricher controllerEnricher = new DefaultControllerEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder();
        controllerEnricher.create(PlatformMode.kubernetes, builder);
        return builder;
    }

    private void assertReplicas(KubernetesListBuilder kubernetesListBuilder, int expectedReplicas) {
        assertThat(kubernetesListBuilder.buildItems())
            .hasSize(1)
            .first(InstanceOfAssertFactories.type(Deployment.class))
            .hasFieldOrPropertyWithValue("metadata.name", "artifact-id")
            .hasFieldOrPropertyWithValue("spec.replicas", expectedReplicas);
    }

    private void assertImagePullPolicy(KubernetesListBuilder kubernetesListBuilder, String expectedImagePullPolicy) {
        assertThat(kubernetesListBuilder.buildItems())
            .hasSize(1)
            .first(InstanceOfAssertFactories.type(Deployment.class))
            .extracting(Deployment::getSpec)
            .extracting(DeploymentSpec::getTemplate)
            .extracting(PodTemplateSpec::getSpec)
            .extracting(PodSpec::getContainers)
            .asList()
            .first(InstanceOfAssertFactories.type(Container.class))
            .hasFieldOrPropertyWithValue("imagePullPolicy", expectedImagePullPolicy);
    }

    private void givenReplicaCountInEnricherConfig(String value) {
        final Map<String, Object> controllerConfig = new TreeMap<>();
        controllerConfig.put("replicaCount", value);
        config.put("jkube-controller", controllerConfig);
    }

}
