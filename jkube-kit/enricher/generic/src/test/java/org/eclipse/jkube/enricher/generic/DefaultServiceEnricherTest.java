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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.enricher.generic.DefaultServiceEnricher.getPortToExpose;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author roland
 */
class DefaultServiceEnricherTest {

    private JKubeEnricherContext context;

    private ImageConfiguration imageConfiguration;

    private GroupArtifactVersion groupArtifactVersion;

    @BeforeEach
    void setUp() {
        context = mock(JKubeEnricherContext.class, RETURNS_DEEP_STUBS);
        imageConfiguration = mock(ImageConfiguration.class);
        groupArtifactVersion = mock(GroupArtifactVersion.class);
    }

    @Nested
    @DisplayName("check port config")
    class PortConfig {
      @Test
      void defaultConfiguration() {
        setupExpectations("type", "LoadBalancer");
        HasMetadata resource = enrich();
        assertAll(
            () -> assertThat(resource).hasFieldOrPropertyWithValue("spec.type", "LoadBalancer"),
            () -> assertPort(resource, 1, 0, 80, 80, "http", "TCP"));
      }

      @Test
      void portOverride() {
        setupExpectations("port", "8080", "multiPort", "true");
        HasMetadata resource = enrich();
        assertAll(
            () -> assertPort(resource, 2, 0, 8080, 80, "http", "TCP"),
            () -> assertPort(resource, 2, 1, 53, 53, "domain", "UDP"));
      }

      @Test
      void portOverrideWithMapping() {
        setupExpectations("port", "443:8181/udp", "multiPort", "true", "normalizePort", "true");
        HasMetadata resource = enrich();
        assertAll(
            () -> assertPort(resource, 2, 0, 80, 8181, "https", "UDP"),
            () -> assertPort(resource, 2, 1, 53, 53, "domain", "UDP"));
      }

      @Test
      void withMultipleMappings() {
        setupExpectations("port", "443:81,853:53", "multiPort", "true");
        HasMetadata resource = enrich();
        assertAll(
            () -> assertPort(resource, 2, 0, 443, 81, "https", "TCP"),
            () -> assertPort(resource, 2, 1, 853, 53, "domain-s", "TCP"));
      }

      @Test
      void withMultipleMapping1() {
        setupExpectations("port", "8080:8081,8443:8443", "multiPort", "true", "normalizePort", "true");
        HasMetadata resource = enrich();
        assertAll(
            () -> assertPort(resource, 2, 0, 80, 8081, "http", "TCP"),
            () -> assertPort(resource, 2, 1, 443, 8443, "https", "TCP"));
      }

      @Test
      void withMultipleMappingsNoMultiPort() {
        setupExpectations("port", "443:81,853:53", "multiPort", "false");
        HasMetadata resource = enrich();
        assertPort(resource, 1, 0, 443, 81, "https", "TCP");
      }

      @Test
      void withMultipleMappingsNoMultiPortNoImagePort() {
        setupExpectations(false, "port", "443:81,853:53", "multiPort", "false");
        HasMetadata resource = enrich();
        assertPort(resource, 1, 0, 443, 81, "https", "TCP");
      }

      @Test
      void withMorePortsThanImagePorts() {
        setupExpectations("port", "443:81,853:53,22/udp", "multiPort", "true");
        HasMetadata resource = enrich();
        assertAll(
            () -> assertPort(resource, 3, 0, 443, 81, "https", "TCP"),
            () -> assertPort(resource, 3, 1, 853, 53, "domain-s", "TCP"),
            () -> assertPort(resource, 3, 2, 22, 22, "ssh", "UDP"));
      }

      @Test
      void withMorePortsThanImagePortsAndNoMultiPort() {
        setupExpectations("port", "443:81,853:53,22");
        HasMetadata resource = enrich();
        assertPort(resource, 1, 0, 443, 81, "https", "TCP");
      }

      @Test
      void withoutPortsFromImageConfig() {
        setupExpectations(false, "port", "443:81,853:53/UdP,22/TCP", "multiPort", "true");
        HasMetadata resource = enrich();
        assertAll(
            () -> assertPort(resource, 3, 0, 443, 81, "https", "TCP"),
            () -> assertPort(resource, 3, 1, 853, 53, "domain-s", "UDP"),
            () -> assertPort(resource, 3, 2, 22, 22, "ssh", "TCP"));
      }
    }

    @Test
    void headlessServicePositive() {
        setupExpectations(false, "headless", "true");
        HasMetadata resource = enrich();
        assertThat(resource)
            .hasFieldOrPropertyWithValue("spec.clusterIP", "None")
            .extracting("spec.ports")
            .asList()
            .isEmpty();
    }

    @Test
    void headlessServiceNegative() {
        setupExpectations(false, "headless", "false");
        DefaultServiceEnricher serviceEnricher = new DefaultServiceEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder();
        serviceEnricher.create(PlatformMode.kubernetes, builder);

        // Validate that the generated resource contains
        KubernetesList list = builder.build();
        assertThat(list.getItems()).isEmpty();
    }

    @Test
    void miscConfiguration() {
        setupExpectations("headless", "true", "type", "NodePort", "expose", "true");
        HasMetadata resource = enrich();
        assertThat(resource)
            .hasFieldOrPropertyWithValue("spec.type", "NodePort")
            .hasFieldOrPropertyWithValue("metadata.labels.expose", "true")
            .hasFieldOrPropertyWithValue("spec.clusterIP", null);
    }

    @Test
    void serviceImageLabelEnrichment() {
        ImageConfiguration imageConfigurationWithLabels = ImageConfiguration.builder()
                .name("test-label")
                .alias("test")
                .build(BuildConfiguration.builder()
                    .labels(Collections.singletonMap("jkube.generator.service.ports", "9090"))
                    .ports(Arrays.asList("80", "53/UDP"))
                    .build())
                .build();
        final TreeMap<String, Object> config = new TreeMap<>();
        config.put("type", "LoadBalancer");
        Configuration configuration = Configuration.builder()
                    .image(imageConfigurationWithLabels)
                    .processorConfig(new ProcessorConfig(null, null, Collections.singletonMap("jkube-service", config)))
                    .build();
        when(groupArtifactVersion.getSanitizedArtifactId()).thenReturn("jkube-service");
        when(context.getConfiguration()).thenReturn(configuration);
        HasMetadata resource = enrich();
        assertPort(resource, 1, 0, 9090, 9090, "http", "TCP");
    }

    @Test
    void getPortToExpose_withHttpPort() {
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
    void getPortToExpose_withNoHttpPort() {
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
    void getPortToExpose_withNoPort() {
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
        assertThat(list.getItems()).hasSize(1);
        return list.getItems().get(0);
    }

    private void assertPort(HasMetadata resource, int noOfPorts, int idx, int port, int targetPort, String name, String protocol) {
      assertThat(resource)
          .extracting("spec.ports").asList()
          .hasSize(noOfPorts)
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
