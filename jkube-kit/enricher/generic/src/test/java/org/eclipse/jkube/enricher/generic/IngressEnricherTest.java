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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressTLSBuilder;
import mockit.Expectations;
import mockit.Mocked;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.IngressRuleConfig;
import org.eclipse.jkube.kit.config.resource.IngressRulePathConfig;
import org.eclipse.jkube.kit.config.resource.IngressRulePathResourceConfig;
import org.eclipse.jkube.kit.config.resource.IngressTlsConfig;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.eclipse.jkube.kit.enricher.api.BaseEnricher.CREATE_EXTERNAL_URLS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class IngressEnricherTest {
    @Mocked
    private JKubeEnricherContext context;

    @Mocked
    private KitLogger logger;

    @Test
    public void testCreateShouldNotCreateAnyIngress() {
        // Given
        Service providedService = getTestService().build();
        KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder().addToItems(providedService);
        IngressEnricher ingressEnricher = new IngressEnricher(context);

        // When
        ingressEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);

        // Then
        assertEquals(1, kubernetesListBuilder.buildItems().size());
    }

    @Test
    public void testCreateWithExternalUrlsSetTrue() {
        // Given
        new Expectations() {{
            // Enable creation of Ingress for Service of type LoadBalancer
            context.getProperty(CREATE_EXTERNAL_URLS);
            result = "true";
        }};

        Service providedService = getTestService().build();
        KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder().addToItems(providedService);
        IngressEnricher ingressEnricher = new IngressEnricher(context);

        // When
        ingressEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);

        // Then
        Ingress ingress = (Ingress) kubernetesListBuilder.buildLastItem();
        assertEquals(2, kubernetesListBuilder.buildItems().size());
        assertNotNull(ingress);
        assertEquals(providedService.getMetadata().getName(), ingress.getMetadata().getName());
        assertNotNull(ingress.getSpec());
        assertEquals(providedService.getMetadata().getName(), ingress.getSpec().getBackend().getServiceName());
        assertEquals(providedService.getSpec().getPorts().get(0).getTargetPort(), ingress.getSpec().getBackend().getServicePort());
    }

    @Test
    public void testCreateIngressFromXMLConfigWithConfiguredServiceName() {
        // Given
        ResourceConfig resourceConfig = ResourceConfig.builder()
                .ingressRule(IngressRuleConfig.builder()
                        .host("foo.bar.com")
                        .paths(Collections.singletonList(IngressRulePathConfig.builder()
                                .pathType("ImplementationSpecific")
                                .path("/icons")
                                .serviceName("hello-k8s")
                                .servicePort(80)
                                .build()))
                        .build())
                .ingressRule(IngressRuleConfig.builder()
                        .host("*.foo.com")
                        .paths(Collections.singletonList(IngressRulePathConfig.builder()
                                .path("/icons-storage")
                                .pathType("Exact")
                                .resource(IngressRulePathResourceConfig.builder()
                                        .apiGroup("k8s.example.com")
                                        .kind("StorageBucket")
                                        .name("icon-assets")
                                        .build())
                                .build()))
                        .build())
                .ingressTlsConfig(IngressTlsConfig.builder()
                        .hosts(Collections.singletonList("https-example.foo.com"))
                        .secretName("testsecret-tls")
                        .build())
                .build();
        new Expectations() {{
            // Enable creation of Ingress for Service of type LoadBalancer
            context.getProperty(CREATE_EXTERNAL_URLS);
            result = "true";

            context.getConfiguration().getResource();
            result = resourceConfig;
        }};

        Service providedService = getTestService().build();
        KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder().addToItems(providedService);
        IngressEnricher ingressEnricher = new IngressEnricher(context);

        // When
        ingressEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);

        // Then
        assertEquals(2, kubernetesListBuilder.buildItems().size());
        HasMetadata lastItem = kubernetesListBuilder.buildLastItem();
        assertTrue(lastItem instanceof Ingress);
        Ingress ingress = (Ingress) lastItem;
        assertNotNull(ingress);
        assertEquals(providedService.getMetadata().getName(), ingress.getMetadata().getName());
        assertNotNull(ingress.getSpec());
        assertEquals(resourceConfig.getIngressTlsConfigs().get(0).getHosts(), ingress.getSpec().getTls().get(0).getHosts());
        assertEquals(resourceConfig.getIngressTlsConfigs().get(0).getSecretName(), ingress.getSpec().getTls().get(0).getSecretName());
        assertEquals(resourceConfig.getIngressRules().get(0).getHost(), ingress.getSpec().getRules().get(0).getHost());
        assertEquals(resourceConfig.getIngressRules().get(0).getPaths().get(0).getServiceName(), ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getBackend().getServiceName());
        assertEquals(resourceConfig.getIngressRules().get(0).getPaths().get(0).getServicePort(), ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getBackend().getServicePort().getIntVal().intValue());
        assertEquals(resourceConfig.getIngressRules().get(0).getPaths().get(0).getPath(), ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getPath());
        assertEquals(resourceConfig.getIngressRules().get(0).getPaths().get(0).getPathType(), ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getPathType());
        assertEquals(resourceConfig.getIngressRules().get(1).getHost(), ingress.getSpec().getRules().get(1).getHost());
        assertEquals(resourceConfig.getIngressRules().get(1).getPaths().get(0).getResource().getApiGroup(), ingress.getSpec().getRules().get(1).getHttp().getPaths().get(0).getBackend().getResource().getApiGroup());
        assertEquals(resourceConfig.getIngressRules().get(1).getPaths().get(0).getResource().getKind(), ingress.getSpec().getRules().get(1).getHttp().getPaths().get(0).getBackend().getResource().getKind());
        assertEquals(resourceConfig.getIngressRules().get(1).getPaths().get(0).getResource().getName(), ingress.getSpec().getRules().get(1).getHttp().getPaths().get(0).getBackend().getResource().getName());
        assertEquals(resourceConfig.getIngressRules().get(1).getPaths().get(0).getPathType(), ingress.getSpec().getRules().get(1).getHttp().getPaths().get(0).getPathType());
        assertEquals(resourceConfig.getIngressRules().get(1).getPaths().get(0).getPath(), ingress.getSpec().getRules().get(1).getHttp().getPaths().get(0).getPath());
    }

    @Test
    public void testCreateIngressFromResourceFragment() {
        // Given
        new Expectations() {{
            // Enable creation of Ingress for Service of type LoadBalancer
            context.getProperty(CREATE_EXTERNAL_URLS);
            result = "true";
        }};

        Service providedService = getTestService().build();
        Ingress providedIngress = getTestIngressFragment().build();
        KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder().addToItems(providedService, providedIngress);
        IngressEnricher ingressEnricher = new IngressEnricher(context);

        // When
        ingressEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);

        // Then
        HasMetadata hasMetadata = kubernetesListBuilder.buildLastItem();
        assertEquals(2, kubernetesListBuilder.buildItems().size());
        assertTrue(hasMetadata instanceof Ingress);
        Ingress ingress = (Ingress) hasMetadata;
        assertEquals(providedIngress, ingress);
    }

    @Test
    public void testAddIngress() {
        // Given
        ServiceBuilder testSvcBuilder = getTestService();
        KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder().addToItems(testSvcBuilder);

        // When
        Ingress ingress = IngressEnricher.addIngress(kubernetesListBuilder, testSvcBuilder, "org.eclipse.jkube", Collections.emptyList(), Collections.emptyList(), logger);

        // Then
        assertNotNull(ingress);
        assertEquals(testSvcBuilder.buildMetadata().getName(), ingress.getMetadata().getName());
        assertEquals(1, ingress.getSpec().getRules().size());
        assertEquals(testSvcBuilder.buildMetadata().getName() + "." + "org.eclipse.jkube", ingress.getSpec().getRules().get(0).getHost());
    }

    @Test
    public void testAddIngressWithNullServiceMetadata() {
        // Given
        ServiceBuilder serviceBuilder = new ServiceBuilder().withMetadata(null);
        KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder().addToItems(serviceBuilder);

        // When
        Ingress ingress = IngressEnricher.addIngress(kubernetesListBuilder, serviceBuilder, "org.eclipse.jkube", Collections.emptyList(), Collections.emptyList(), logger);

        // Then
        assertNull(ingress);
    }

    @Test
    public void testGetServicePortWithHttpPort() {
        // Given
        ServiceBuilder serviceBuilder = getTestService();

        // When
        Integer port = IngressEnricher.getServicePort(serviceBuilder);

        // Then
        assertNotNull(port);
        assertEquals(8080, port.intValue());
    }

    @Test
    public void testGetServiceWithNoHttpPort() {
        // Given
        ServiceBuilder serviceBuilder = getTestService()
                .editSpec()
                .withPorts(new ServicePortBuilder()
                        .withName("p1")
                        .withProtocol("TCP")
                        .withPort(9001)
                        .build())
                .endSpec();

        // When
        Integer port = IngressEnricher.getServicePort(serviceBuilder);

        // Then
        assertNotNull(port);
        assertEquals(9001, port.intValue());
    }

    @Test
    public void testGetServiceWithNoPort() {
        // Given
        ServiceBuilder serviceBuilder = getTestService()
                .editSpec()
                .withPorts(Collections.emptyList())
                .endSpec();

        // When
        Integer port = IngressEnricher.getServicePort(serviceBuilder);

        // Then
        assertNotNull(port);
        assertEquals(0, port.intValue());
    }

    @Test
    public void testShouldCreateExternalURLForService() {
        assertTrue(IngressEnricher.shouldCreateExternalURLForService(getTestService(), logger));
        assertFalse(IngressEnricher.shouldCreateExternalURLForService(getTestService().editSpec()
                .withType("ClusterIP")
                .endSpec(), logger));
        assertFalse(IngressEnricher.shouldCreateExternalURLForService(getTestService().editSpec()
                .addNewPort()
                .withName("p2")
                .withProtocol("TCP")
                .withPort(9090)
                .endPort()
                .endSpec(), logger));
    }

    @Test
    public void testGetRouteDomainNoConfig() {
        // Given
        IngressEnricher ingressEnricher = new IngressEnricher(context);

        // When
        String result = ingressEnricher.getRouteDomain(ResourceConfig.builder().build());

        assertNull(result);
    }

    @Test
    public void testGetRouteDomainFromResourceConfig() {
        // Given
        IngressEnricher ingressEnricher = new IngressEnricher(context);
        ResourceConfig resourceConfig = ResourceConfig.builder()
                .routeDomain("org.eclipse.jkube")
                .build();

        // When
        String result = ingressEnricher.getRouteDomain(resourceConfig);

        // Then
        assertEquals("org.eclipse.jkube", result);
    }

    @Test
    public void testGetRouteDomainFromProperty() {
        // Given
        new Expectations() {{
            context.getProperty("jkube.domain");
            result = "org.eclipse.jkube";
        }};
        IngressEnricher ingressEnricher = new IngressEnricher(context);

        // When
        String result = ingressEnricher.getRouteDomain(ResourceConfig.builder().build());

        // Then
        assertEquals("org.eclipse.jkube", result);
    }

    @Test
    public void testGetIngressRuleXMLConfigWithNonNullResourceConfig() {
        // Given
        ResourceConfig resourceConfig = ResourceConfig.builder()
                .ingressRule(IngressRuleConfig.builder()
                        .host("host1")
                        .build())
                .build();

        // When
        List<IngressRuleConfig> ingressRuleXMLConfig = IngressEnricher.getIngressRuleXMLConfig(resourceConfig);

        // Then
        assertNotNull(ingressRuleXMLConfig);
        assertEquals(1, ingressRuleXMLConfig.size());
    }

    @Test
    public void testGetIngressRuleXMLConfigWithNullResourceConfig() {
        // Given + When
        List<IngressRuleConfig> ingressRuleConfigs = IngressEnricher.getIngressRuleXMLConfig(null);

        // Then
        assertNotNull(ingressRuleConfigs);
        assertTrue(ingressRuleConfigs.isEmpty());
    }

    @Test
    public void testGetIngressTlsXMLConfigWithNonNullResourceConfig() {
        // Given
        ResourceConfig resourceConfig = ResourceConfig.builder()
                .ingressTlsConfig(IngressTlsConfig.builder()
                        .secretName("secret1")
                        .build())
                .build();

        // When
        List<IngressTlsConfig> ingressTlsConfig = IngressEnricher.getIngressTlsXMLConfig(resourceConfig);

        // Then
        assertNotNull(ingressTlsConfig);
        assertEquals(1, ingressTlsConfig.size());
    }

    @Test
    public void testGetIngressTlsXMLConfigWithNullResourceConfig() {
        // Given + When
        List<IngressTlsConfig> ingressTlsConfigs = IngressEnricher.getIngressTlsXMLConfig(null);

        // Then
        assertNotNull(ingressTlsConfigs);
        assertTrue(ingressTlsConfigs.isEmpty());
    }

    private IngressBuilder getTestIngressFragment() {
        return new IngressBuilder()
                .withNewMetadata()
                .withName("test-svc")
                .addToAnnotations("ingress.kubernetes.io/rewrite-target", "/")
                .endMetadata()
                .withNewSpec()
                .withTls(new IngressTLSBuilder()
                        .addNewHost("my.host.com")
                        .withSecretName("letsencrypt-pod")
                        .build())
                .addNewRule()
                .withHost("my.host.com")
                .withNewHttp()
                .addNewPath()
                .withPath("/")
                .withPathType("Prefix")
                .withNewBackend()
                .withServiceName("test-svc")
                .withServicePort(new IntOrString(8080))
                .endBackend()
                .endPath()
                .endHttp()
                .endRule()
                .endSpec();
    }

    private ServiceBuilder getTestService() {
        return new ServiceBuilder()
                .withNewMetadata()
                .withName("test-svc")
                .addToLabels("expose", "true")
                .endMetadata()
                .withNewSpec()
                .addNewPort()
                .withName("http")
                .withPort(8080)
                .withProtocol("TCP")
                .withTargetPort(new IntOrString(8080))
                .endPort()
                .addToSelector("group", "test")
                .withType("LoadBalancer")
                .endSpec();
    }
}
