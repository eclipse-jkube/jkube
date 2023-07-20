/*
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
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.apps.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import org.eclipse.jkube.kit.common.PrefixedLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.ControllerResourceConfig;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedConstruction;

import java.util.Collections;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.enricher.generic.ImageEnricher.containerImageName;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author nicola
 */
class ImageEnricherTest {

    private JKubeEnricherContext context;
    ImageConfiguration imageConfiguration;

    private ImageEnricher imageEnricher;

    @BeforeEach
    void prepareMock() {
        context = mock(JKubeEnricherContext.class,RETURNS_DEEP_STUBS);
        imageConfiguration = mock(ImageConfiguration.class,RETURNS_DEEP_STUBS);
        // Setup mock behaviour
        givenResourceConfigWithEnvVar("MY_KEY", "MY_VALUE");
        when(imageConfiguration.getName()).thenReturn("busybox");
        when(imageConfiguration.getAlias()).thenReturn("busybox");

        imageEnricher = new ImageEnricher(context);
    }

    @DisplayName("check enriched resource type")
    @ParameterizedTest(name = "{1}")
    @MethodSource("resources")
    void checkEnrich(HasMetadata item, String kind) {
        final KubernetesListBuilder builder = new KubernetesListBuilder().addToItems(item);
        imageEnricher.create(PlatformMode.kubernetes, builder);
        assertCorrectlyGeneratedResources(builder.build(), kind, "MY_KEY", "MY_VALUE");
    }

    static Stream<Arguments> resources() {
      return Stream.of(
          arguments(new DeploymentBuilder().build(), "Deployment"),
          arguments(new ReplicaSetBuilder().build(), "ReplicaSet"),
          arguments(new ReplicationControllerBuilder().build(), "ReplicationController"),
          arguments(new DaemonSetBuilder().build(), "DaemonSet"),
          arguments(new StatefulSetBuilder().build(), "StatefulSet"),
          arguments(new DeploymentConfigBuilder().build(), "DeploymentConfig"));
    }

    @Test
    void create_whenEnvironmentVariableAbsent_thenAddsEnvironmentVariable() {
        // Given
        KubernetesListBuilder builder = new KubernetesListBuilder().addToItems(new DeploymentBuilder().build());

        // When
        imageEnricher.create(PlatformMode.kubernetes, builder);

        // Then
        assertCorrectlyGeneratedResources(builder.build(), "Deployment", "MY_KEY", "MY_VALUE");
    }

    @Test
    void create_whenEnvironmentVariablePresentWithDifferentValue_thenOldValueIsPreserved() {
        // Given
        givenResourceConfigWithEnvVar("key", "valueNew");
        KubernetesListBuilder builder = new KubernetesListBuilder().addToItems(new DeploymentBuilder()
            .withNewSpec()
            .withNewTemplate()
            .withNewSpec()
            .addNewContainer()
            .addNewEnv().withName("key").withValue("valueOld").endEnv()
            .endContainer()
            .endSpec()
            .endTemplate()
            .endSpec()
          .build());

        // When
        imageEnricher.create(PlatformMode.kubernetes, builder);

        // Then
        assertCorrectlyGeneratedResources(builder.build(), "Deployment", "key", "valueOld");
    }

    @Test
    void create_whenNoImageConfiguration_thenSkip() {
        try (MockedConstruction<PrefixedLogger> prefixedLoggerMockedConstruction = mockConstruction(PrefixedLogger.class)) {
            // Given
            KubernetesListBuilder builder = new KubernetesListBuilder();
            when(context.getConfiguration()).thenReturn(Configuration.builder()
                .images(Collections.emptyList())
                .build());
            imageEnricher = new ImageEnricher(context);

            // When
            imageEnricher.create(PlatformMode.kubernetes, builder);

            // Then
            assertThat(prefixedLoggerMockedConstruction.constructed()).hasSize(1);
            verify(prefixedLoggerMockedConstruction.constructed().get(0))
                .verbose("No images resolved. Skipping ...");
        }
    }

    @Test
    void containerImageName_whenRegistryPresent_thenAddRegistryPrefixToImageName() {
        assertThat(containerImageName(ImageConfiguration.builder()
            .name("foo/bar:latest")
            .registry("example.com")
            .build())).isEqualTo("example.com/foo/bar:latest");
    }

    private void assertCorrectlyGeneratedResources(KubernetesList list, String kind, String expectedKey, String expectedValue) {
      assertThat(list.getItems())
          .satisfies(l -> assertThat(l).singleElement()
              .hasFieldOrPropertyWithValue("kind", kind)
          )
          .satisfies(l -> assertThat(l)
              .extracting("spec.template.spec.containers").first().asList()
              .extracting("env").first().asList().first()
              .hasFieldOrPropertyWithValue("name", expectedKey)
              .hasFieldOrPropertyWithValue("value", expectedValue)
          );
    }

    private void givenResourceConfigWithEnvVar(String name, String value) {
        Configuration configuration = Configuration.builder()
                .resource(ResourceConfig.builder()
                    .controller(ControllerResourceConfig.builder()
                        .env(Collections.singletonMap(name, value))
                        .build())
                    .build())
                .image(imageConfiguration)
                .build();
        when(context.getConfiguration()).thenReturn(configuration);
    }
}
