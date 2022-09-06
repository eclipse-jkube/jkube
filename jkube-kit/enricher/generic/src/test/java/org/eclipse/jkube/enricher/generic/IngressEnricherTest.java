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

import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.IngressConfig;
import org.eclipse.jkube.kit.config.resource.IngressRuleConfig;
import org.eclipse.jkube.kit.config.resource.IngressRulePathConfig;
import org.eclipse.jkube.kit.config.resource.IngressRulePathResourceConfig;
import org.eclipse.jkube.kit.config.resource.IngressTlsConfig;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressSpec;
import io.fabric8.kubernetes.api.model.networking.v1.IngressTLSBuilder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.jkube.kit.enricher.api.BaseEnricher.CREATE_EXTERNAL_URLS;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IngressEnricherTest {
    private JKubeEnricherContext context;

    private KitLogger logger;

    ImageConfiguration imageConfiguration;

    private IngressEnricher ingressEnricher;

    @Before
    public void setUp() throws Exception {
        context = mock(JKubeEnricherContext.class,RETURNS_DEEP_STUBS);
        logger = mock(KitLogger.class);
        imageConfiguration = mock(ImageConfiguration.class);
        ingressEnricher = new IngressEnricher(context);
    }

    @Test
    public void testCreateIngressFromXMLConfigWithConfiguredServiceName() {
        // Given
        ResourceConfig resourceConfig = ResourceConfig.builder()
            .ingress(IngressConfig.builder()
                .ingressRule(IngressRuleConfig.builder()
                        .host("foo.bar.com")
                        .path(IngressRulePathConfig.builder()
                            .pathType("ImplementationSpecific")
                            .path("/icons")
                            .serviceName("hello-k8s")
                            .servicePort(80)
                            .build())
                        .build())
                .ingressRule(IngressRuleConfig.builder()
                        .host("*.foo.com")
                        .path(IngressRulePathConfig.builder()
                            .path("/icons-storage")
                            .pathType("Exact")
                            .resource(IngressRulePathResourceConfig.builder()
                                .apiGroup("k8s.example.com")
                                .kind("StorageBucket")
                                .name("icon-assets")
                                .build())
                            .build())
                        .build())
                .ingressTlsConfig(IngressTlsConfig.builder()
                        .host("https-example.foo.com")
                        .secretName("testsecret-tls")
                        .build())
                .build()
            ).build();
        when(context.getProperty(CREATE_EXTERNAL_URLS)).thenReturn("true");
        when(context.getConfiguration().getResource()).thenReturn(resourceConfig);

        Service providedService = initTestService().build();
        KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder().addToItems(providedService);

        // When
        ingressEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);

        // Then
        assertThat(kubernetesListBuilder)
            .extracting(KubernetesListBuilder::buildItems).asList()
            .hasSize(2)
            .element(1).asInstanceOf(InstanceOfAssertFactories.type(Ingress.class))
            .hasFieldOrPropertyWithValue("apiVersion", "networking.k8s.io/v1")
            .hasFieldOrPropertyWithValue("metadata.name", providedService.getMetadata().getName())
            .extracting(Ingress::getSpec)
            .satisfies(is -> assertThat(is).extracting("tls").asList().element(0)
                .hasFieldOrPropertyWithValue("secretName", "testsecret-tls")
                .extracting("hosts").asList().containsExactly("https-example.foo.com"))
            .extracting(IngressSpec::getRules)
            .satisfies(r -> assertThat(r).asList().element(0)
                .hasFieldOrPropertyWithValue("host", "foo.bar.com")
                .extracting("http.paths").asList().element(0)
                .hasFieldOrPropertyWithValue("path", "/icons")
                .hasFieldOrPropertyWithValue("pathType", "ImplementationSpecific")
                .hasFieldOrPropertyWithValue("backend.service.name", "hello-k8s")
                .hasFieldOrPropertyWithValue("backend.service.port.number", 80)
            )
            .satisfies(r -> assertThat(r).asList().element(1)
                .hasFieldOrPropertyWithValue("host", "*.foo.com")
                .extracting("http.paths").asList().element(0)
                .hasFieldOrPropertyWithValue("path", "/icons-storage")
                .hasFieldOrPropertyWithValue("pathType", "Exact")
                .hasFieldOrPropertyWithValue("backend.resource.apiGroup", "k8s.example.com")
                .hasFieldOrPropertyWithValue("backend.resource.kind", "StorageBucket")
                .hasFieldOrPropertyWithValue("backend.resource.name", "icon-assets")
            );
    }

    @Test
    public void testGetRouteDomainNoConfig() {
        assertThat(ingressEnricher.getRouteDomain()).isNull();
    }

    @Test
    public void testGetRouteDomainFromResourceConfig() {
        // Given
        when(context.getConfiguration().getResource()).thenReturn(ResourceConfig.builder()
                .routeDomain("org.eclipse.jkube")
                .build());
        // When
        String result = ingressEnricher.getRouteDomain();

        // Then
        assertThat(result).isEqualTo("org.eclipse.jkube");
    }

    @Test
    public void testGetRouteDomainFromProperty() {
        // Given
        when(context.getProperty("jkube.domain")).thenReturn("org.eclipse.jkube.property");
        // When
        String result = ingressEnricher.getRouteDomain();

        // Then
        assertThat(result).isEqualTo("org.eclipse.jkube.property");
    }

    @Test
    public void testGetIngressRuleXMLConfigWithNonNullResourceConfig() {
        // Given
        ResourceConfig resourceConfig = ResourceConfig.builder()
            .ingress(IngressConfig.builder()
                .ingressRule(IngressRuleConfig.builder()
                        .host("host1")
                        .build())
                .build()
            ).build();

        // When
        List<IngressRuleConfig> ingressRuleXMLConfig = IngressEnricher.getIngressRuleXMLConfig(resourceConfig);

        // Then
        assertThat(ingressRuleXMLConfig).asList().hasSize(1);
    }

    @Test
    public void testGetIngressRuleXMLConfigWithNullResourceConfig() {
        // Given + When
        List<IngressRuleConfig> ingressRuleConfigs = IngressEnricher.getIngressRuleXMLConfig(null);

        // Then
        assertThat(ingressRuleConfigs).asList().isEmpty();
    }

    @Test
    public void testGetIngressTlsXMLConfigWithNonNullResourceConfig() {
        // Given
        ResourceConfig resourceConfig = ResourceConfig.builder()
            .ingress(IngressConfig.builder()
                .ingressTlsConfig(IngressTlsConfig.builder()
                        .secretName("secret1")
                        .build())
                .build()
            ).build();

        // When
        List<IngressTlsConfig> ingressTlsConfigs = IngressEnricher.getIngressTlsXMLConfig(resourceConfig);

        // Then
        assertThat(ingressTlsConfigs).asList().hasSize(1);
    }

    @Test
    public void testGetIngressTlsXMLConfigWithNullResourceConfig() {
        // Given + When
        List<IngressTlsConfig> ingressTlsConfigs = IngressEnricher.getIngressTlsXMLConfig(null);

        // Then
        assertThat(ingressTlsConfigs).asList().isEmpty();
    }

    @Test
    public void testNetworkingV1IngressIsGenerated() {
        // Given
        final TreeMap<String, Object> config = new TreeMap<>();
        config.put("host", "test.192.168.39.25.nip.io");
        config.put("targetApiVersion", "networking.k8s.io/v1");
        Configuration configuration = Configuration.builder()
                .image(imageConfiguration)
                .processorConfig(new ProcessorConfig(null, null, Collections.singletonMap("jkube-ingress", config)))
                .build();
        when(context.getConfiguration()).thenReturn(configuration);
        when(context.getProperty("jkube.createExternalUrls")).thenReturn("true");

        KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder();
        kubernetesListBuilder.addToItems(initTestService().build());

        // When
        ingressEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);

        // Then
        assertThat(kubernetesListBuilder)
            .extracting(KubernetesListBuilder::buildItems).asList()
            .hasSize(2)
            .element(1)
            .extracting("spec.rules").asList()
            .extracting("host")
            .containsExactly("test.192.168.39.25.nip.io");
    }

    private IngressBuilder initTestIngressFragment() {
        return new IngressBuilder()
                .withMetadata(createIngressFragmentMetadata())
                .withNewSpec()
                .withTls(new IngressTLSBuilder()
                        .addToHosts("my.host.com")
                        .withSecretName("letsencrypt-pod")
                        .build())
                .addNewRule()
                .withHost("my.host.com")
                .withNewHttp()
                .addNewPath()
                .withPath("/")
                .withPathType("Prefix")
                .withNewBackend()
                .withNewService().withName("test-svc").withNewPort().withNumber(8080).endPort().endService()
                .endBackend()
                .endPath()
                .endHttp()
                .endRule()
                .endSpec();
    }

    private ObjectMeta createIngressFragmentMetadata() {
        return new ObjectMetaBuilder()
                .withName("test-svc")
                .addToAnnotations("ingress.kubernetes.io/rewrite-target", "/")
                .build();
    }

    private io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder networkingV1IngressFragment() {
        return new io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder()
                .withMetadata(createIngressFragmentMetadata())
                .withNewSpec()
                .addNewRule()
                .withHost("test-jkube-ingress.test.192.168.39.25.nip.io")
                .withNewHttp()
                .addNewPath()
                .withNewBackend()
                .withNewService()
                .withName("test-jkube-ingress")
                .withNewPort().withNumber(8080).endPort()
                .endService()
                .endBackend()
                .endPath()
                .endHttp()
                .endRule()
                .endSpec();
    }

    private ServiceBuilder initTestService() {
        return new ServiceBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName("test-svc")
                    .addToLabels("expose", "true")
                .build())
                .withSpec(new ServiceSpecBuilder()
                    .addNewPort()
                    .withName("http")
                    .withPort(8080)
                    .withProtocol("TCP")
                    .withTargetPort(new IntOrString(8080))
                    .endPort()
                    .addToSelector("group", "test")
                    .withType("LoadBalancer")
                    .build()
                );
    }
}
