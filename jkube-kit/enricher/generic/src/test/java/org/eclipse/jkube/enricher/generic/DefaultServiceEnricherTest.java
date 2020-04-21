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
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.TreeMap;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author roland
 */
public class DefaultServiceEnricherTest {

    @Mocked
    private JKubeEnricherContext context;

    @Mocked
    ImageConfiguration imageConfiguration;

    @Mocked
    GroupArtifactVersion groupArtifactVersion;

    @Test
    public void checkDefaultConfiguration() throws Exception {
        setupExpectations("type", "LoadBalancer");

        String json = enrich();
        assertThat(json, isJson());
        assertThat(json, hasJsonPath("$.spec.type", equalTo("LoadBalancer")));
        assertPort(json, 0, 80, 80, "http", "TCP");
        assertThat(json, hasJsonPath("$.spec.ports[*]", hasSize(1)));
    }

    @Test
    public void portOverride() throws JsonProcessingException {
        setupExpectations("port", "8080", "multiPort", "true");

        String json = enrich();
        assertPort(json, 0, 8080, 80, "http", "TCP");
        assertPort(json, 1, 53, 53, "domain", "UDP");
        assertThat(json, hasJsonPath("$.spec.ports[*]", hasSize(2)));
    }

    @Test
    public void portOverrideWithMapping() throws JsonProcessingException {
        setupExpectations("port", "443:8181/udp", "multiPort", "true", "normalizePort", "true");

        String json = enrich();
        assertPort(json, 0, 80, 8181, "https", "UDP");
        assertPort(json, 1, 53, 53, "domain", "UDP");
        assertThat(json, hasJsonPath("$.spec.ports[*]", hasSize(2)));
    }

    @Test
    public void portConfigWithMultipleMappings() throws Exception {
        setupExpectations("port", "443:81,853:53", "multiPort", "true");
        String json = enrich();
        assertPort(json, 0, 443, 81, "https", "TCP");
        assertPort(json, 1, 853, 53, "domain-s", "TCP");
        assertThat(json, hasJsonPath("$.spec.ports[*]", hasSize(2)));
    }

    @Test
    public void portConfigWithMultipleMapping1() throws JsonProcessingException {
        setupExpectations("port", "8080:8081,8443:8443", "multiPort", "true", "normalizePort", "true");

        String json = enrich();
        assertPort(json, 0, 80, 8081, "http", "TCP");
        assertPort(json, 1, 443, 8443, "https", "TCP");
        assertThat(json, hasJsonPath("$.spec.ports[*]", hasSize(2)));
    }


    @Test
    public void portConfigWithMultipleMappingsNoMultiPort() throws Exception {
        setupExpectations("port", "443:81,853:53", "multiPort", "false");
        String json = enrich();
        assertPort(json, 0, 443, 81, "https", "TCP");
        assertThat(json, hasJsonPath("$.spec.ports[*]", hasSize(1)));
    }

    @Test
    public void portConfigWithMultipleMappingsNoMultiPortNoImagePort() throws Exception {
        setupExpectations(false, "port", "443:81,853:53", "multiPort", "false");
        String json = enrich();
        assertPort(json, 0, 443, 81, "https", "TCP");
        assertThat(json, hasJsonPath("$.spec.ports[*]", hasSize(1)));
    }

    @Test
    public void portConfigWithMortPortsThanImagePorts() throws Exception {
        setupExpectations("port", "443:81,853:53,22/udp", "multiPort", "true");
        String json = enrich();
        assertPort(json, 0, 443, 81, "https", "TCP");
        assertPort(json, 1, 853, 53, "domain-s", "TCP");
        assertPort(json, 2, 22, 22, "ssh", "UDP");
        assertThat(json, hasJsonPath("$.spec.ports[*]", hasSize(3)));

    }

    @Test
    public void portConfigWithMortPortsThanImagePortsAndNoMultiPort() throws Exception {
        setupExpectations("port", "443:81,853:53,22");
        String json = enrich();
        assertPort(json, 0, 443, 81, "https", "TCP");
        assertThat(json, hasJsonPath("$.spec.ports[*]", hasSize(1)));
    }

    @Test
    public void portConfigWithoutPortsFromImageConfig() throws Exception {
        setupExpectations(false, "port", "443:81,853:53/UdP,22/TCP", "multiPort", "true");
        String json = enrich();
        assertPort(json, 0, 443, 81, "https", "TCP");
        assertPort(json, 1, 853, 53, "domain-s", "UDP");
        assertPort(json, 2, 22, 22, "ssh", "TCP");
        assertThat(json, hasJsonPath("$.spec.ports[*]", hasSize(3)));
    }

    @Test
    public void headlessServicePositive() throws Exception {
        setupExpectations(false, "headless", "true");
        String json = enrich();
        assertThat(json, hasNoJsonPath("$.spec.ports[*]"));
        assertThat(json, hasJsonPath("$.spec.clusterIP", equalTo("None")));

    }

    @Test
    public void headlessServiceNegative() throws Exception {
        setupExpectations(false, "headless", "false");
        DefaultServiceEnricher serviceEnricher = new DefaultServiceEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder();
        serviceEnricher.create(PlatformMode.kubernetes, builder);

        // Validate that the generated resource contains
        KubernetesList list = builder.build();
        assertEquals(list.getItems().size(),0);
    }

    @Test
    public void miscConfiguration() throws Exception {
        setupExpectations("headless", "true", "type", "NodePort", "expose", "true");
        String json = enrich();
        assertThat(json, hasJsonPath("$.spec.type", equalTo("NodePort")));
        assertThat(json, hasJsonPath("$.metadata.labels.expose", equalTo("true")));
        assertThat(json, hasNoJsonPath("$.spec.clusterIP"));

    }

    @Test
    public void serviceImageLabelEnrichment() throws Exception {
        ImageConfiguration imageConfigurationWithLabels = ImageConfiguration.builder()
                .name("test-label")
                .alias("test")
                .build();
        final TreeMap<String, String> config = new TreeMap<>();
        config.put("type", "LoadBalancer");

        new Expectations() {{

            Configuration configuration = Configuration.builder()
                    .image(imageConfigurationWithLabels)
                    .processorConfig(new ProcessorConfig(null, null, Collections.singletonMap("jkube-service", config)))
                    .build();

            groupArtifactVersion.getSanitizedArtifactId();
            result = "jkube-service";

            context.getConfiguration();
            result = configuration;

            imageConfigurationWithLabels.getBuildConfiguration();
            result = BuildConfiguration.builder()
                    .labels(Collections.singletonMap("jkube.generator.service.ports", "9090"))
                    .ports(Arrays.asList("80", "53/UDP"))
                    .build();
        }};

        String json = enrich();
        assertPort(json, 0, 9090, 9090, "http", "TCP");
    }

    // ======================================================================================================

    private String enrich() throws JsonProcessingException {
        // Enrich
        DefaultServiceEnricher serviceEnricher = new DefaultServiceEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder();
        serviceEnricher.create(PlatformMode.kubernetes, builder);

        // Validate that the generated resource contains
        KubernetesList list = builder.build();
        assertEquals(1, list.getItems().size());

        return ResourceUtil.toJson(list.getItems().get(0));
    }


    private void assertPort(String json, int idx, int port, int targetPort, String name, String protocol) {
        assertThat(json, isJson());
        assertThat(json, hasJsonPath("$.spec.ports[" + idx + "].port", equalTo(port)));
        assertThat(json, hasJsonPath("$.spec.ports[" + idx + "].targetPort", equalTo(targetPort)));
        assertThat(json, hasJsonPath("$.spec.ports[" + idx + "].name", equalTo(name)));
        assertThat(json, hasJsonPath("$.spec.ports[" + idx + "].protocol", equalTo(protocol)));
    }

    private void setupExpectations(String ... configParams) {
        setupExpectations(true, configParams);
    }

    private void setupExpectations(final boolean withPorts, String ... configParams) {
        // Setup mock behaviour
        final TreeMap config = new TreeMap();
        for (int i = 0; i < configParams.length; i += 2) {
                config.put(configParams[i],configParams[i+1]);
        }

        new Expectations() {{

            Configuration configuration = Configuration.builder()
                .image(imageConfiguration)
                .processorConfig(new ProcessorConfig(null, null, Collections.singletonMap("jkube-service", config)))
                .build();

            context.getConfiguration();
            result = configuration;

            imageConfiguration.getBuildConfiguration();
            result = initBuildConfig(withPorts);
        }};
    }

    private BuildConfiguration initBuildConfig(boolean withPorts) {
        // Setup a sample docker build configuration
        BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder();
        if (withPorts) {
            builder.ports(Arrays.asList("80", "53/UDP"));
        }
        return builder.build();
    }


}
