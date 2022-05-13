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
import org.eclipse.jkube.kit.config.image.build.Arguments;
import org.eclipse.jkube.kit.config.image.build.HealthCheckConfiguration;
import org.eclipse.jkube.kit.config.image.build.HealthCheckMode;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author nicola
 */
public class DockerHealthCheckEnricherTest {

    @Mocked
    private JKubeEnricherContext context;

    @Test
    public void testEnrichFromSingleImage() {
        // Setup mock behaviour
        new Expectations() {{
            List<ImageConfiguration> images =  Arrays.asList(ImageConfiguration.builder()
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
            context.getConfiguration();
            result = Configuration.builder().images(images).build();
        }};

        KubernetesListBuilder builder = createDeployment("myImage");

        DockerHealthCheckEnricher enricher = new DockerHealthCheckEnricher(context);
        enricher.create(PlatformMode.kubernetes, builder);

        KubernetesList list = builder.build();
        assertEquals(1, list.getItems().size());
        assertHealthCheckMatching(builder.build().getItems().get(0), "livenessProbe", "/bin/check", 1, 3601, 3);
        assertHealthCheckMatching(builder.build().getItems().get(0), "readinessProbe", "/bin/check", 1, 3601, 3);
    }

    @Test
    public void testEnrichFromDoubleImage() {
        // Setup mock behaviour
        new Expectations() {{
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
            context.getConfiguration();
            result = Configuration.builder().images(images).build();

            context.getProcessingInstructions();
            result = Collections.singletonMap("FABRIC8_GENERATED_CONTAINERS", "myImage,myImage2");
        }};

        KubernetesListBuilder builder = addDeployment(createDeployment("myImage"), "myImage2");

        DockerHealthCheckEnricher enricher = new DockerHealthCheckEnricher(context);
        enricher.create(PlatformMode.kubernetes, builder);

        KubernetesList list = builder.build();
        assertEquals(2, list.getItems().size());
        assertHealthCheckMatching(builder.build().getItems().get(0), "livenessProbe", "/bin/check", 1, 3601, 3);
        assertHealthCheckMatching(builder.build().getItems().get(0), "readinessProbe", "/bin/check", 1, 3601, 3);
        assertHealthCheckMatching(builder.build().getItems().get(1), "livenessProbe", "/xxx/check", 3, 10801, 9);
        assertHealthCheckMatching(builder.build().getItems().get(1), "readinessProbe", "/xxx/check", 3, 10801, 9);
    }

    @Test
    public void testInvalidHealthCheck() {
        // Setup mock behaviour
        new Expectations() {{
            final ImageConfiguration image = ImageConfiguration.builder()
                    .alias("myImage")
                    .build(BuildConfiguration.builder()
                            .healthCheck(HealthCheckConfiguration.builder()
                                    .mode(HealthCheckMode.none)
                                    .build())
                            .build())
                    .build();
            context.getConfiguration();
            result = Configuration.builder().image(image).build();
        }};

        KubernetesListBuilder builder = createDeployment("myImage");

        DockerHealthCheckEnricher enricher = new DockerHealthCheckEnricher(context);
        enricher.create(PlatformMode.kubernetes, builder);

        KubernetesList list = builder.build();
        assertEquals(1, list.getItems().size());
        assertNoProbes(list.getItems().get(0));
    }

    @Test
    public void testUnmatchingHealthCheck() {
        // Setup mock behaviour
        new Expectations() {{
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
            context.getConfiguration();
            result = Configuration.builder().image(image).build();
        }};

        KubernetesListBuilder builder = createDeployment("myUnmatchingImage");

        DockerHealthCheckEnricher enricher = new DockerHealthCheckEnricher(context);
        enricher.create(PlatformMode.kubernetes, builder);

        KubernetesList list = builder.build();
        assertEquals(1, list.getItems().size());
        assertNoProbes(list.getItems().get(0));
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

    private void assertNoProbes(HasMetadata object) {
        assertThat(object)
                .extracting("spec.template.spec.containers")
                .asList()
                .first()
                .hasFieldOrPropertyWithValue("livenessProbe", null)
                .hasFieldOrPropertyWithValue("readinessProbe", null);
    }

    private void assertHealthCheckMatching(HasMetadata object, String type, String command, Integer timeoutSeconds, Integer periodSeconds, Integer failureThreshold) {
        assertThat(object)
                .extracting("spec.template.spec.containers")
                .asList()
                .first()
                .hasFieldOrProperty(type);

        if (command != null){
            assertThat(object)
                    .extracting("spec.template.spec.containers")
                    .asList()
                    .first()
                    .extracting(type + ".exec.command")
                    .asList()
                    .first()
                    .isEqualTo(command);
        }
        if (timeoutSeconds != null){
            assertThat(object)
                    .extracting("spec.template.spec.containers")
                    .asList()
                    .first()
                    .extracting(type)
                    .hasFieldOrPropertyWithValue("timeoutSeconds", timeoutSeconds);
        }
        if (periodSeconds != null){
            assertThat(object)
                    .extracting("spec.template.spec.containers")
                    .asList()
                    .first()
                    .extracting(type)
                    .hasFieldOrPropertyWithValue("periodSeconds", periodSeconds);
        }
        if (failureThreshold != null){
          assertThat(object)
              .extracting("spec.template.spec.containers")
              .asList()
              .first()
              .extracting(type)
              .hasFieldOrPropertyWithValue("failureThreshold", failureThreshold);
        }
    }

}
