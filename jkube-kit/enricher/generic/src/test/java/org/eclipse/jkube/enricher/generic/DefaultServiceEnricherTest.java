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

import java.util.Arrays;
import java.util.Collections;
import java.util.TreeMap;

import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.enricher.generic.DefaultServiceEnricher.getPortToExpose;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author roland
 */
public class DefaultServiceEnricherTest {

    private JKubeEnricherContext context;

    ImageConfiguration imageConfiguration;

    GroupArtifactVersion groupArtifactVersion;
    @Before
    public void setUp() {
        context = mock(JKubeEnricherContext.class, RETURNS_DEEP_STUBS);
        imageConfiguration = mock(ImageConfiguration.class);
        groupArtifactVersion = mock(GroupArtifactVersion.class);
    }
    @Test
    public void checkDefaultConfiguration() {
        setupExpectations("type", "LoadBalancer");

        HasMetadata object = enrich();
        assertThat(object)
                .extracting("spec.ports")
                .asList()
                .hasSize(1);

        assertThat(object)
                .hasFieldOrPropertyWithValue("spec.type", "LoadBalancer");

        assertPort(object, 0, 80, 80, "http", "TCP");
    }

    @Test
    public void portOverride() {
        setupExpectations("port", "8080", "multiPort", "true");

        HasMetadata object = enrich();
        assertThat(object)
                .extracting("spec.ports")
                .asList()
                .hasSize(2);

        assertPort(object, 0, 8080, 80, "http", "TCP");
        assertPort(object, 1, 53, 53, "domain", "UDP");
    }

    @Test
    public void portOverrideWithMapping() {
        setupExpectations("port", "443:8181/udp", "multiPort", "true", "normalizePort", "true");

        HasMetadata object = enrich();
        assertThat(object)
                .extracting("spec.ports")
                .asList()
                .hasSize(2);

        assertPort(object, 0, 80, 8181, "https", "UDP");
        assertPort(object, 1, 53, 53, "domain", "UDP");
    }

    @Test
    public void portConfigWithMultipleMappings() {
        setupExpectations("port", "443:81,853:53", "multiPort", "true");

        HasMetadata object = enrich();
        assertThat(object)
                .extracting("spec.ports")
                .asList()
                .hasSize(2);

        assertPort(object, 0, 443, 81, "https", "TCP");
        assertPort(object, 1, 853, 53, "domain-s", "TCP");
    }

    @Test
    public void portConfigWithMultipleMapping1() {
        setupExpectations("port", "8080:8081,8443:8443", "multiPort", "true", "normalizePort", "true");

        HasMetadata object = enrich();
        assertThat(object)
                .extracting("spec.ports")
                .asList()
                .hasSize(2);

        assertPort(object, 0, 80, 8081, "http", "TCP");
        assertPort(object, 1, 443, 8443, "https", "TCP");
    }


    @Test
    public void portConfigWithMultipleMappingsNoMultiPort() {
        setupExpectations("port", "443:81,853:53", "multiPort", "false");

        HasMetadata object = enrich();
        assertThat(object)
                .extracting("spec.ports")
                .asList()
                .hasSize(1);

        assertPort(object, 0, 443, 81, "https", "TCP");
    }

    @Test
    public void portConfigWithMultipleMappingsNoMultiPortNoImagePort() {
        setupExpectations(false, "port", "443:81,853:53", "multiPort", "false");

        HasMetadata object = enrich();
        assertThat(object)
                .extracting("spec.ports")
                .asList()
                .hasSize(1);

        assertPort(object, 0, 443, 81, "https", "TCP");
    }

    @Test
    public void portConfigWithMortPortsThanImagePorts() {
        setupExpectations("port", "443:81,853:53,22/udp", "multiPort", "true");

        HasMetadata object = enrich();
        assertThat(object)
                .extracting("spec.ports")
                .asList()
                .hasSize(3);

        assertPort(object, 0, 443, 81, "https", "TCP");
        assertPort(object, 1, 853, 53, "domain-s", "TCP");
        assertPort(object, 2, 22, 22, "ssh", "UDP");
    }

    @Test
    public void portConfigWithMortPortsThanImagePortsAndNoMultiPort() {
        setupExpectations("port", "443:81,853:53,22");

        HasMetadata object = enrich();
        assertThat(object)
                .extracting("spec.ports")
                .asList()
                .hasSize(1);

        assertPort(object, 0, 443, 81, "https", "TCP");
    }

    @Test
    public void portConfigWithoutPortsFromImageConfig() {
        setupExpectations(false, "port", "443:81,853:53/UdP,22/TCP", "multiPort", "true");

        HasMetadata object = enrich();
        assertThat(object)
                .extracting("spec.ports")
                .asList()
                .hasSize(3);

        assertPort(object, 0, 443, 81, "https", "TCP");
        assertPort(object, 1, 853, 53, "domain-s", "UDP");
        assertPort(object, 2, 22, 22, "ssh", "TCP");
    }

    @Test
    public void headlessServicePositive() {
        setupExpectations(false, "headless", "true");
        HasMetadata object = enrich();

        assertThat(object)
                .hasFieldOrPropertyWithValue("spec.clusterIP", "None");

        assertThat(object)
                .extracting("spec.ports")
                .asList()
                .isEmpty();
    }

    @Test
    public void headlessServiceNegative() {
        setupExpectations(false, "headless", "false");
        DefaultServiceEnricher serviceEnricher = new DefaultServiceEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder();
        serviceEnricher.create(PlatformMode.kubernetes, builder);

        // Validate that the generated resource contains
        KubernetesList list = builder.build();
        assertTrue(list.getItems().isEmpty());
    }

    @Test
    public void miscConfiguration() {
        setupExpectations("headless", "true", "type", "NodePort", "expose", "true");
        HasMetadata object = enrich();

        assertThat(object)
                .hasFieldOrPropertyWithValue("spec.type", "NodePort")
                .hasFieldOrPropertyWithValue("metadata.labels.expose", "true")
                .hasFieldOrPropertyWithValue("spec.clusterIP", null);
    }

    @Test
    public void serviceImageLabelEnrichment() {
        ImageConfiguration imageConfigurationWithLabels = ImageConfiguration.builder()
                .name("test-label")
                .alias("test")
                .build();
        final TreeMap<String, Object> config = new TreeMap<>();
        config.put("type", "LoadBalancer");
        Configuration configuration = Configuration.builder()
                    .image(imageConfigurationWithLabels)
                    .processorConfig(new ProcessorConfig(null, null, Collections.singletonMap("jkube-service", config)))
                    .build();
        when(groupArtifactVersion.getSanitizedArtifactId()).thenReturn("jkube-service");
        when(context.getConfiguration()).thenReturn(configuration);
        when(imageConfigurationWithLabels.getBuildConfiguration()).thenReturn(BuildConfiguration.builder()
                .labels(Collections.singletonMap("jkube.generator.service.ports", "9090"))
                .ports(Arrays.asList("80", "53/UDP"))
                .build());
        HasMetadata object = enrich();
        assertPort(object, 0, 9090, 9090, "http", "TCP");
    }

    @Test
    public void getPortToExpose_withHttpPort() {
        // Given
        final ServiceBuilder serviceBuilder = new ServiceBuilder()
            .withNewSpec()
            .addNewPort().withPort(443).withNewTargetPort(8443).withName("https").endPort()
            .addNewPort().withPort(80).withNewTargetPort(8080).withName("http").endPort()
            .addNewPort().withPort(8778).withNewTargetPort(8778).withName("jolokia").endPort()
            .endSpec();
        // When
        final Integer result = getPortToExpose(serviceBuilder);
        // Then
        assertThat(result).isEqualTo(80);
    }

    @Test
    public void getPortToExpose_withNoHttpPort() {
        // Given
        final ServiceBuilder serviceBuilder = new ServiceBuilder()
            .withNewSpec()
            .addNewPort().withPort(443).withNewTargetPort(8443).withName("https").endPort()
            .addNewPort().withPort(9001).withNewTargetPort(9001).withName("p1").withProtocol("TCP").endPort()
            .addNewPort().withPort(8778).withNewTargetPort(8778).withName("jolokia").endPort()
            .endSpec();
        // When
        final Integer result = getPortToExpose(serviceBuilder);
        // Then
        assertThat(result).isEqualTo(443);
    }

    @Test
    public void getPortToExpose_withNoPort() {
        // Given
        final ServiceBuilder serviceBuilder = new ServiceBuilder().withNewSpec().endSpec();
        // When
        final Integer result = getPortToExpose(serviceBuilder);
        // Then
        assertThat(result).isNull();
    }

    // ======================================================================================================

    private HasMetadata enrich(){
        // Enrich
        DefaultServiceEnricher serviceEnricher = new DefaultServiceEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder();
        serviceEnricher.create(PlatformMode.kubernetes, builder);

        // Validate that the generated resource contains
        KubernetesList list = builder.build();
        assertEquals(1, list.getItems().size());
        return list.getItems().get(0);
    }

    private void assertPort(HasMetadata object, int idx, int port, int targetPort, String name, String protocol) {
        assertThat(object)
                .extracting("spec.ports")
                .asList()
                .element(idx)
                .hasFieldOrPropertyWithValue("port", port)
                .hasFieldOrPropertyWithValue("targetPort.IntVal", targetPort)
                .hasFieldOrPropertyWithValue("name", name)
                .hasFieldOrPropertyWithValue("protocol", protocol);
    }

    private void setupExpectations(String ... configParams) {
        setupExpectations(true, configParams);
    }

    private void setupExpectations(final boolean withPorts, String ... configParams) {
        // Setup mock behaviour
        final TreeMap<String, Object> config = new TreeMap<>();
        for (int i = 0; i < configParams.length; i += 2) {
                config.put(configParams[i],configParams[i+1]);
        }
        Configuration configuration = Configuration.builder()
                .image(imageConfiguration)
                .processorConfig(new ProcessorConfig(null, null, Collections.singletonMap("jkube-service", config)))
                .build();

        when(context.getConfiguration()).thenReturn(configuration);
        when(imageConfiguration.getBuildConfiguration()).thenReturn(initBuildConfig(withPorts));
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
