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
package org.eclipse.jkube.maven.enricher.specific;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jayway.jsonpath.matchers.JsonPathMatchers;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import org.eclipse.jkube.kit.build.core.config.JKubeBuildConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.Arguments;
import org.eclipse.jkube.kit.config.image.build.HealthCheckConfiguration;
import org.eclipse.jkube.kit.config.image.build.HealthCheckMode;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.maven.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.maven.enricher.api.model.Configuration;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import mockit.Expectations;
import mockit.Mocked;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author nicola
 */
public class DockerHealthCheckEnricherTest {

    @Mocked
    private JKubeEnricherContext context;

    @Test
    public void testEnrichFromSingleImage() throws Exception {
        // Setup mock behaviour
        new Expectations() {{
            List<ImageConfiguration> images =  Arrays.asList(new ImageConfiguration.Builder()
                            .alias("myImage")
                            .buildConfig(new JKubeBuildConfiguration.Builder()
                                    .healthCheck(new HealthCheckConfiguration.Builder()
                                            .mode(HealthCheckMode.cmd)
                                            .cmd(new Arguments("/bin/check"))
                                            .timeout("1s")
                                            .interval("1h1s")
                                            .retries(3)
                                            .build())
                                    .build())
                            .build(),
                                                   new ImageConfiguration.Builder()
                            .alias("myImage2")
                            .buildConfig(new JKubeBuildConfiguration.Builder()
                                    .healthCheck(new HealthCheckConfiguration.Builder()
                                            .mode(HealthCheckMode.cmd)
                                            .cmd(new Arguments("/xxx/check"))
                                            .timeout("3s")
                                            .interval("3h1s")
                                            .retries(9)
                                            .build())
                                    .build())
                            .build());
            context.getConfiguration();
            result = new Configuration.Builder().images(images).build();
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
    public void testEnrichFromDoubleImage() throws Exception {
        // Setup mock behaviour
        new Expectations() {{
            List<ImageConfiguration> images = Arrays.asList(new ImageConfiguration.Builder()
                            .alias("myImage")
                            .buildConfig(new JKubeBuildConfiguration.Builder()
                                    .healthCheck(new HealthCheckConfiguration.Builder()
                                            .mode(HealthCheckMode.cmd)
                                            .cmd(new Arguments("/bin/check"))
                                            .timeout("1s")
                                            .interval("1h1s")
                                            .retries(3)
                                            .build())
                                    .build())
                            .build(),
                    new ImageConfiguration.Builder()
                            .alias("myImage2")
                            .buildConfig(new JKubeBuildConfiguration.Builder()
                                    .healthCheck(new HealthCheckConfiguration.Builder()
                                            .mode(HealthCheckMode.cmd)
                                            .cmd(new Arguments("/xxx/check"))
                                            .timeout("3s")
                                            .interval("3h1s")
                                            .retries(9)
                                            .build())
                                    .build())
                            .build());
            context.getConfiguration();
            result = new Configuration.Builder().images(images).build();

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
    public void testInvalidHealthCheck() throws Exception {
        // Setup mock behaviour
        new Expectations() {{
            List<ImageConfiguration> images = Arrays.asList(new ImageConfiguration.Builder()
                    .alias("myImage")
                    .buildConfig(new JKubeBuildConfiguration.Builder()
                            .healthCheck(new HealthCheckConfiguration.Builder()
                                    .mode(HealthCheckMode.none)
                                    .build())
                            .build())
                    .build());
            context.getConfiguration();
            result = new Configuration.Builder().images(images).build();
        }};

        KubernetesListBuilder builder = createDeployment("myImage");

        DockerHealthCheckEnricher enricher = new DockerHealthCheckEnricher(context);
        enricher.create(PlatformMode.kubernetes, builder);

        KubernetesList list = builder.build();
        assertEquals(1, list.getItems().size());
        assertNoProbes(list.getItems().get(0));
    }

    @Test
    public void testUnmatchingHealthCheck() throws Exception {
        // Setup mock behaviour
        new Expectations() {{
            List<ImageConfiguration> images = Arrays.asList(new ImageConfiguration.Builder()
                    .alias("myImage")
                    .buildConfig(new JKubeBuildConfiguration.Builder()
                            .healthCheck(new HealthCheckConfiguration.Builder()
                                    .mode(HealthCheckMode.cmd)
                                    .cmd(new Arguments("/bin/check"))
                                    .timeout("1s")
                                    .interval("1h1s")
                                    .retries(3)
                                    .build())
                            .build())
                    .build());
            context.getConfiguration();
            result = new Configuration.Builder().images(images).build();
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
        return list.addNewDeploymentItem()
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
                .endDeploymentItem();
    }

    private void assertNoProbes(HasMetadata object) throws JsonProcessingException {
        String json = ResourceUtil.toJson(object);
        assertThat(json, JsonPathMatchers.isJson());
        assertThat(json, JsonPathMatchers.hasJsonPath("$.spec.template.spec.containers[0]", Matchers.not(Matchers.hasKey("livenessProbe"))));
        assertThat(json, JsonPathMatchers.hasJsonPath("$.spec.template.spec.containers[0]", Matchers.not(Matchers.hasKey("readinessProbe"))));
    }

    private void assertHealthCheckMatching(HasMetadata object, String type, String command, Integer timeoutSeconds, Integer periodSeconds, Integer failureThreshold) throws JsonProcessingException {
        String json = ResourceUtil.toJson(object);
        assertThat(json, JsonPathMatchers.isJson());
        assertThat(json, JsonPathMatchers.hasJsonPath("$.spec.template.spec.containers[0]", Matchers.hasKey(type)));

        if (command != null) {
            assertThat(json, JsonPathMatchers.hasJsonPath("$.spec.template.spec.containers[0]." + type + ".exec.command[0]", Matchers.equalTo(command)));
        }
        if (timeoutSeconds != null) {
            assertThat(json, JsonPathMatchers.hasJsonPath("$.spec.template.spec.containers[0]." + type + ".timeoutSeconds", Matchers.equalTo(timeoutSeconds)));
        }
        if (periodSeconds != null) {
            assertThat(json, JsonPathMatchers.hasJsonPath("$.spec.template.spec.containers[0]." + type + ".periodSeconds", Matchers.equalTo(periodSeconds)));
        }
        if (failureThreshold != null) {
            assertThat(json, JsonPathMatchers.hasJsonPath("$.spec.template.spec.containers[0]." + type + ".failureThreshold", Matchers.equalTo(failureThreshold)));
        }
    }

}
