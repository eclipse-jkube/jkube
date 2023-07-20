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
package org.eclipse.jkube.kit.enricher.specific;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.Arguments;
import org.eclipse.jkube.kit.config.image.build.HealthCheckConfiguration;
import org.eclipse.jkube.kit.config.image.build.HealthCheckMode;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author nicola
 */
class DockerHealthCheckEnricherTest {

    private JKubeEnricherContext context;

    @BeforeEach
    void setUp() {
        context = mock(JKubeEnricherContext.class,RETURNS_DEEP_STUBS);
    }

    @Test
    void enrichFromSingleImage() {
        DockerHealthCheckEnricher enricher = new DockerHealthCheckEnricher(context);
        // Setup mock behaviour
        List<ImageConfiguration> images = Arrays.asList(ImageConfiguration.builder()
                .alias("myImage")
                .build(BuildConfiguration.builder()
                    .healthCheck(HealthCheckConfiguration.builder()
                        .mode(HealthCheckMode.cmd)
                        .cmd(Arguments.builder().shell("/bin/check").build())
                        .timeout("1s")
                        .interval("1h1s")
                        .retries(3)
                        .build())
                    .build())
                .build(),
            ImageConfiguration.builder()
                .alias("myImage2")
                .build(BuildConfiguration.builder()
                    .healthCheck(HealthCheckConfiguration.builder()
                        .mode(HealthCheckMode.cmd)
                        .cmd(Arguments.builder().shell("/xxx/check").build())
                        .timeout("3s")
                        .interval("3h1s")
                        .retries(9)
                        .build())
                    .build())
                .build());
        when(context.getConfiguration()).thenReturn(Configuration.builder().images(images).build());
        KubernetesListBuilder builder = createDeployment("myImage");

        enricher.create(PlatformMode.kubernetes, builder);

        KubernetesList list = builder.build();
        assertThat(list.getItems()).hasSize(1);
        assertHealthCheckMatching(builder.build().getItems().get(0), "livenessProbe", "/bin/check", 1, 3601, 3);
        assertHealthCheckMatching(builder.build().getItems().get(0), "readinessProbe", "/bin/check", 1, 3601, 3);
    }

    @Test
    void enrichFromDoubleImage() {
        // Setup mock behaviour
        List<ImageConfiguration> images = Arrays.asList(ImageConfiguration.builder()
                .alias("myImage")
                .build(BuildConfiguration.builder()
                    .healthCheck(HealthCheckConfiguration.builder()
                        .mode(HealthCheckMode.cmd)
                        .cmd(Arguments.builder().shell("/bin/check").build())
                        .timeout("1s")
                        .interval("1h1s")
                        .retries(3)
                        .build())
                    .build())
                .build(),
            ImageConfiguration.builder()
                .alias("myImage2")
                .build(BuildConfiguration.builder()
                    .healthCheck(HealthCheckConfiguration.builder()
                        .mode(HealthCheckMode.cmd)
                        .cmd(Arguments.builder().shell("/xxx/check").build())
                        .timeout("3s")
                        .interval("3h1s")
                        .retries(9)
                        .build())
                    .build())
                .build());
        when(context.getConfiguration()).thenReturn(Configuration.builder().images(images).build());
        when(context.getProcessingInstructions()).thenReturn(Collections.singletonMap("FABRIC8_GENERATED_CONTAINERS", "myImage,myImage2"));

        KubernetesListBuilder builder = addDeployment(createDeployment("myImage"), "myImage2");

        DockerHealthCheckEnricher enricher = new DockerHealthCheckEnricher(context);
        enricher.create(PlatformMode.kubernetes, builder);

        KubernetesList list = builder.build();
        assertThat(list.getItems()).hasSize(2);
        assertHealthCheckMatching(builder.build().getItems().get(0), "livenessProbe", "/bin/check", 1, 3601, 3);
        assertHealthCheckMatching(builder.build().getItems().get(0), "readinessProbe", "/bin/check", 1, 3601, 3);
        assertHealthCheckMatching(builder.build().getItems().get(1), "livenessProbe", "/xxx/check", 3, 10801, 9);
        assertHealthCheckMatching(builder.build().getItems().get(1), "readinessProbe", "/xxx/check", 3, 10801, 9);
    }

    @Test
    void invalidHealthCheck() {

        // Setup mock behaviour
        final ImageConfiguration image = ImageConfiguration.builder()
            .alias("myImage")
            .build(BuildConfiguration.builder()
                .healthCheck(HealthCheckConfiguration.builder()
                    .mode(HealthCheckMode.none)
                    .build())
                .build())
            .build();
        when(context.getConfiguration()).thenReturn( Configuration.builder().image(image).build());
        KubernetesListBuilder builder = createDeployment("myImage");

        DockerHealthCheckEnricher enricher = new DockerHealthCheckEnricher(context);
        enricher.create(PlatformMode.kubernetes, builder);

        KubernetesList list = builder.build();
        assertNoProbes(list);
    }

    @Test
    void unmatchingHealthCheck() {
        // Setup mock behaviour
        final ImageConfiguration image = ImageConfiguration.builder()
            .alias("myImage")
            .build(BuildConfiguration.builder()
                .healthCheck(HealthCheckConfiguration.builder()
                    .mode(HealthCheckMode.cmd)
                    .cmd(Arguments.builder().shell("/bin/check").build())
                    .timeout("1s")
                    .interval("1h1s")
                    .retries(3)
                    .build())
                .build())
            .build();
        when(context.getConfiguration()).thenReturn(Configuration.builder().image(image).build());
        KubernetesListBuilder builder = createDeployment("myUnmatchingImage");

        DockerHealthCheckEnricher enricher = new DockerHealthCheckEnricher(context);
        enricher.create(PlatformMode.kubernetes, builder);

        KubernetesList list = builder.build();
        assertNoProbes(list);
    }

    private KubernetesListBuilder createDeployment(String name) {
        return addDeployment(new KubernetesListBuilder(), name);
    }

    private KubernetesListBuilder addDeployment(KubernetesListBuilder list, String name) {
        return list.addToItems(new DeploymentBuilder()
            .withNewMetadata()
            .withName(name)
            .endMetadata()
            .withNewSpec()
            .withNewTemplate()
            .withNewSpec()
            .addNewContainer()
            .withName(name)
            .endContainer()
            .endSpec()
            .endTemplate()
            .endSpec()
            .build());
    }

    private void assertNoProbes(KubernetesList list) {
      assertThat(list.getItems()).singleElement()
          .extracting("spec.template.spec.containers")
          .asList()
          .first()
          .hasFieldOrPropertyWithValue("livenessProbe", null)
          .hasFieldOrPropertyWithValue("readinessProbe", null);
    }

    private void assertHealthCheckMatching(HasMetadata resource, String type, String command, int timeoutSeconds, int periodSeconds, int failureThreshold) {
      assertThat(resource)
          .extracting("spec.template.spec.containers").asList()
          .first()
          .extracting(type).isNotNull()
          .hasFieldOrPropertyWithValue("timeoutSeconds", timeoutSeconds)
          .hasFieldOrPropertyWithValue("periodSeconds", periodSeconds)
          .hasFieldOrPropertyWithValue("failureThreshold", failureThreshold)
          .satisfies(r -> assertThat(r)
              .extracting("exec.command").asList()
              .first()
              .isEqualTo(command));
    }

}
