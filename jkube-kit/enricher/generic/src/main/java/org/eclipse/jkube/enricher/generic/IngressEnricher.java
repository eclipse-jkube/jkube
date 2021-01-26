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

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressPathBuilder;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressRuleValueBuilder;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressBackend;
import io.fabric8.kubernetes.api.model.extensions.IngressBackendBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressRule;
import io.fabric8.kubernetes.api.model.extensions.IngressRuleBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressSpec;
import io.fabric8.kubernetes.api.model.extensions.IngressSpecBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressTLS;
import io.fabric8.kubernetes.api.model.extensions.IngressTLSBuilder;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.config.resource.IngressRuleConfig;
import org.eclipse.jkube.kit.config.resource.IngressRulePathConfig;
import org.eclipse.jkube.kit.config.resource.IngressRulePathResourceConfig;
import org.eclipse.jkube.kit.config.resource.IngressTlsConfig;
import org.eclipse.jkube.kit.config.resource.JKubeAnnotations;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.isExposedService;

/**
 * Enricher which generates an Ingress for each exposed Service
 */
public class IngressEnricher extends BaseEnricher {
    public static final String EXPOSE_LABEL = "expose";

    public IngressEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-ingress");
    }

    @Override
    public void create(PlatformMode platformMode, final KubernetesListBuilder listBuilder) {
        ResourceConfig resourceConfig = getConfiguration().getResource();
        Boolean shouldCreateIngress = getValueFromConfig(CREATE_EXTERNAL_URLS, false);
        if (shouldCreateIngress.equals(Boolean.FALSE)) {
            return;
        }

        if (platformMode == PlatformMode.kubernetes) {
            listBuilder.accept(new TypedVisitor<ServiceBuilder>() {
                @Override
                public void visit(ServiceBuilder serviceBuilder) {
                    Ingress ingress = generateIngress(listBuilder, serviceBuilder, getRouteDomain(resourceConfig), getIngressRuleXMLConfig(resourceConfig), getIngressTlsXMLConfig(resourceConfig), log);
                    if (ingress != null) {
                        listBuilder.addToItems(ingress);
                    }
                }
            });
        }
    }

    protected static Ingress generateIngress(KubernetesListBuilder listBuilder, ServiceBuilder serviceBuilder, String routeDomainPostfix, List<IngressRuleConfig> ingressRuleConfigs, List<IngressTlsConfig> ingressTlsConfigs, KitLogger log) {
        ObjectMeta serviceMetadata = serviceBuilder.buildMetadata();
        if (serviceMetadata == null) {
            log.info("No Metadata for service! ");
        }
        if (isExposedService(serviceMetadata) && shouldCreateExternalURLForService(serviceBuilder, log)) {
            Objects.requireNonNull(serviceMetadata);
            String serviceName = serviceMetadata.getName();
            if (!hasIngress(listBuilder, serviceName)) {
                Integer servicePort = getServicePort(serviceBuilder);
                if (servicePort != null) {
                    return new IngressBuilder()
                            .withMetadata(getIngressMetadata(serviceMetadata))
                            .withSpec(getIngressSpec(routeDomainPostfix, serviceName, servicePort, ingressRuleConfigs, ingressTlsConfigs))
                            .build();

                }
            }
        }
        return null;
    }

    private static ObjectMeta getIngressMetadata(ObjectMeta serviceMetadata) {
        ObjectMetaBuilder ingressMetadataBuilder = new ObjectMetaBuilder(serviceMetadata);

        // removing `expose : true` label from metadata.
        ingressMetadataBuilder.removeFromLabels(Collections.singletonMap(EXPOSE_LABEL, "true"));
        ingressMetadataBuilder.removeFromLabels(Collections.singletonMap(JKubeAnnotations.SERVICE_EXPOSE_URL.value(), "true"));

        return ingressMetadataBuilder.build();
    }

    private static IngressSpec getIngressSpec(String routeDomainPostfix, String serviceName, Integer servicePort, List<IngressRuleConfig> ingressRuleConfig, List<IngressTlsConfig> ingressTlsConfigs) {
        if (ingressRuleConfig == null || ingressRuleConfig.isEmpty()) {
            return getOpinionatedIngressSpec(routeDomainPostfix, serviceName, servicePort);
        }
        return getXmlConfiguredIngressSpec(ingressRuleConfig, ingressTlsConfigs);
    }

    private static IngressSpec getXmlConfiguredIngressSpec(List<IngressRuleConfig> ingressRuleConfigs, List<IngressTlsConfig> ingressTlsConfigs) {
        IngressSpecBuilder ingressSpecBuilder = new IngressSpecBuilder();
        for (IngressRuleConfig ingressRuleConfig: ingressRuleConfigs) {
            IngressRule ingressRule = getIngressRuleFromXmlConfig(ingressRuleConfig);
            ingressSpecBuilder.addToRules(ingressRule);
        }

        for (IngressTlsConfig ingressTlsConfig : ingressTlsConfigs) {
            IngressTLS ingressTLS = getIngressTlsFromXMLConfig(ingressTlsConfig);
            ingressSpecBuilder.addToTls(ingressTLS);
        }
        return ingressSpecBuilder.build();
    }

    private static IngressTLS getIngressTlsFromXMLConfig(IngressTlsConfig ingressTlsConfig) {
        IngressTLSBuilder ingressTLSBuilder = new IngressTLSBuilder();
        if (ingressTlsConfig.getHosts() != null && !ingressTlsConfig.getHosts().isEmpty()) {
            ingressTLSBuilder.withHosts(ingressTlsConfig.getHosts());
        }
        if (ingressTlsConfig.getSecretName() != null) {
            ingressTLSBuilder.withSecretName(ingressTlsConfig.getSecretName());
        }
        return ingressTLSBuilder.build();
    }

    private static IngressSpec getOpinionatedIngressSpec(String routeDomainPostfix, String serviceName, Integer servicePort) {
        IngressSpecBuilder ingressSpecBuilder = new IngressSpecBuilder();
        if (StringUtils.isNotBlank(routeDomainPostfix)) {
            routeDomainPostfix = serviceName + "." + FileUtil.stripPrefix(routeDomainPostfix, ".");
            ingressSpecBuilder.addNewRule()
                    .withHost(routeDomainPostfix)
                        .withNewHttp()
                            .withPaths(new HTTPIngressPathBuilder()
                            .withNewBackend()
                            .withServiceName(serviceName)
                            .withServicePort(KubernetesHelper.createIntOrString(servicePort))
                            .endBackend()
                            .build())
                        .endHttp()
                    .endRule().build();
        } else {
            ingressSpecBuilder.withBackend(new IngressBackendBuilder().
                    withNewServiceName(serviceName)
                    .withNewServicePort(servicePort)
                    .build());
        }
        return ingressSpecBuilder.build();
    }

    private static IngressRule getIngressRuleFromXmlConfig(IngressRuleConfig ingressRuleConfig) {
        IngressRuleBuilder ingressRuleBuilder = new IngressRuleBuilder();
        if (ingressRuleConfig.getHost() != null) {
            ingressRuleBuilder.withHost(ingressRuleConfig.getHost());
        }
        if (ingressRuleConfig.getPaths() != null && !ingressRuleConfig.getPaths().isEmpty()) {
            HTTPIngressRuleValueBuilder httpIngressPathBuilder = new HTTPIngressRuleValueBuilder();
            for (IngressRulePathConfig ingressRulePathConfig : ingressRuleConfig.getPaths()) {
                httpIngressPathBuilder.addToPaths(getHTTPIngressPath(ingressRulePathConfig));
            }
            ingressRuleBuilder.withHttp(httpIngressPathBuilder.build());
        }
        return ingressRuleBuilder.build();
    }

    private static HTTPIngressPath getHTTPIngressPath(IngressRulePathConfig ingressRulePathConfig) {
        HTTPIngressPathBuilder httpIngressPathBuilder = new HTTPIngressPathBuilder();
        if (ingressRulePathConfig.getPath() != null) {
            httpIngressPathBuilder.withPath(ingressRulePathConfig.getPath());
        }
        if (ingressRulePathConfig.getPathType() != null) {
            httpIngressPathBuilder.withPathType(ingressRulePathConfig.getPathType());
        }
        return httpIngressPathBuilder.withBackend(getIngressBackend(ingressRulePathConfig))
                .build();
    }

    private static IngressBackend getIngressBackend(IngressRulePathConfig ingressRulePathConfig) {
        IngressBackendBuilder ingressBackendBuilder = new IngressBackendBuilder();
        if (ingressRulePathConfig.getServiceName() != null) {
            ingressBackendBuilder.withServiceName(ingressRulePathConfig.getServiceName());
        }
        if (ingressRulePathConfig.getServicePort() > 0) {
            ingressBackendBuilder.withServicePort(new IntOrString(ingressRulePathConfig.getServicePort()));
        }
        if (ingressRulePathConfig.getResource() != null) {
            IngressRulePathResourceConfig resource = ingressRulePathConfig.getResource();
            ingressBackendBuilder.withNewResource(resource.getApiGroup(), resource.getKind(), resource.getName());
        }

        return ingressBackendBuilder.build();
    }

    static Integer getServicePort(ServiceBuilder serviceBuilder) {
        ServiceSpec spec = serviceBuilder.buildSpec();
        if (spec != null) {
            List<ServicePort> ports = spec.getPorts();
            if (ports != null && !ports.isEmpty()) {
                for (ServicePort port : ports) {
                    if (port.getName().equals("http") || port.getProtocol().equals("http")) {
                        return port.getPort();
                    }
                }
                ServicePort servicePort = ports.get(0);
                if (servicePort != null) {
                    return servicePort.getPort();
                }
            }
        }
        return 0;
    }

    /**
     * Returns true if we already have a route created for the given name
     */
    private static boolean hasIngress(final KubernetesListBuilder listBuilder, final String name) {
        final AtomicBoolean answer = new AtomicBoolean(false);
        listBuilder.accept(new TypedVisitor<IngressBuilder>() {

            @Override
            public void visit(IngressBuilder builder) {
                ObjectMeta metadata = builder.buildMetadata();
                if (metadata != null && name.equals(metadata.getName())) {
                    answer.set(true);
                }
            }
        });
        return answer.get();
    }

    /**
     * Should we try to create an external URL for the given service?
     *
     * <p> By default let's ignore the kubernetes services and any service which does not expose ports 80 and 443
     *
     * @return true if we should create an Ingress for this service.
     */
    static boolean shouldCreateExternalURLForService(ServiceBuilder service, KitLogger log) {
        String serviceName = service.buildMetadata().getName();
        ServiceSpec spec = service.buildSpec();
        if (spec != null && !isKubernetesSystemService(serviceName)) {
            List<ServicePort> ports = spec.getPorts();
            log.debug("Service " + serviceName + " has ports: " + ports);
            if (ports.size() == 1) {
                String type = spec.getType();
                if (Objects.equals(type, "LoadBalancer")) {
                    return true;
                }
                log.info("Not generating Ingress for service " + serviceName + " type is not LoadBalancer: " + type);
            } else {
                log.info("Not generating Ingress for service " + serviceName + " as only single port services are supported. Has ports: " + ports);
            }
        }
        return false;
    }

    private static boolean isKubernetesSystemService(String serviceName) {
        return "kubernetes".equals(serviceName) || "kubernetes-ro".equals(serviceName);
    }

    protected String getRouteDomain(ResourceConfig resourceConfig) {
        if (resourceConfig != null && resourceConfig.getRouteDomain() != null) {
            return resourceConfig.getRouteDomain();
        }
        String routeDomainFromProperties = getValueFromConfig(JKUBE_DOMAIN, "");
        if (StringUtils.isNotEmpty(routeDomainFromProperties)) {
            return routeDomainFromProperties;
        }
        return null;
    }

    static List<IngressRuleConfig> getIngressRuleXMLConfig(ResourceConfig resourceConfig) {
        if (resourceConfig != null) {
            return resourceConfig.getIngressRules();
        }
        return Collections.emptyList();
    }

    static List<IngressTlsConfig> getIngressTlsXMLConfig(ResourceConfig resourceConfig) {
        if (resourceConfig != null) {
            return resourceConfig.getIngressTlsConfigs();
        }
        return Collections.emptyList();
    }
}
