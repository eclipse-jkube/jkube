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
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.config.resource.IngressConfig;
import org.eclipse.jkube.kit.config.resource.IngressRuleConfig;
import org.eclipse.jkube.kit.config.resource.IngressTlsConfig;
import org.eclipse.jkube.kit.config.resource.JKubeAnnotations;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enricher which generates an Ingress for each exposed Service
 */
public class IngressEnricher extends BaseEnricher {
    public static final String EXPOSE_LABEL = "expose";

    @AllArgsConstructor
    public enum Config implements Configs.Config {
        HOST("host", null),
        TARGET_API_VERSION("targetApiVersion", "extensions/v1beta1");

        @Getter
        protected String key;
        @Getter
        protected String defaultValue;
    }

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
                    HasMetadata generatedIngress = generateIngressWithConfiguredApiVersion(serviceBuilder, listBuilder, resourceConfig);
                    if (generatedIngress != null) {
                        listBuilder.addToItems(generatedIngress);
                    }
                }
            });
        }
    }

    private HasMetadata generateIngressWithConfiguredApiVersion(ServiceBuilder serviceBuilder, KubernetesListBuilder listBuilder, ResourceConfig resourceConfig) {
        io.fabric8.kubernetes.api.model.networking.v1.Ingress ingress = NetworkingV1IngressGenerator.generate(listBuilder, serviceBuilder, getRouteDomain(resourceConfig), getConfig(Config.HOST), getIngressRuleXMLConfig(resourceConfig), getIngressTlsXMLConfig(resourceConfig), log);
        HasMetadata generatedIngress = ingress;

        String targetIngressApiVersion = getConfig(Config.TARGET_API_VERSION);
        if (targetIngressApiVersion.equalsIgnoreCase("extensions/v1beta1")) {
            generatedIngress = ExtensionsV1beta1IngressConverter.convert(ingress);
        }
        return generatedIngress;
    }

    static ObjectMeta getIngressMetadata(ObjectMeta serviceMetadata) {
        ObjectMetaBuilder ingressMetadataBuilder = new ObjectMetaBuilder(serviceMetadata);

        // removing `expose : true` label from metadata.
        ingressMetadataBuilder.removeFromLabels(Collections.singletonMap(EXPOSE_LABEL, "true"));
        ingressMetadataBuilder.removeFromLabels(Collections.singletonMap(JKubeAnnotations.SERVICE_EXPOSE_URL.value(), "true"));

        return ingressMetadataBuilder.build();
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
     * Returns true if we already have an ingress created for the given name
     */
    static boolean hasIngress(final KubernetesListBuilder listBuilder, final String name) {
        final AtomicBoolean answer = new AtomicBoolean(false);
        listBuilder.accept(new TypedVisitor<io.fabric8.kubernetes.api.model.extensions.IngressBuilder>() {

            @Override
            public void visit(io.fabric8.kubernetes.api.model.extensions.IngressBuilder builder) {
                ObjectMeta metadata = builder.buildMetadata();
                if (metadata != null && name.equals(metadata.getName())) {
                    answer.set(true);
                }
            }
        });
        listBuilder.accept(new TypedVisitor<io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder>() {
            @Override
            public void visit(io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder builder) {
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
                return true;
            } else {
                log.info("Not generating Ingress for service " + serviceName + " as only single port services are supported. Has ports: " + ports);
            }
        }
        return false;
    }

    static String resolveIngressHost(String serviceName, String routeDomainPostfix, String host) {
        if (host != null) {
            return host;
        }
        return serviceName + "." + FileUtil.stripPrefix(routeDomainPostfix, ".");
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
        return Optional.ofNullable(resourceConfig).map(ResourceConfig::getIngress).map(IngressConfig::getIngressRules)
            .orElse(Collections.emptyList());
    }

    static List<IngressTlsConfig> getIngressTlsXMLConfig(ResourceConfig resourceConfig) {
        return Optional.ofNullable(resourceConfig).map(ResourceConfig::getIngress).map(IngressConfig::getIngressTlsConfigs)
            .orElse(Collections.emptyList());
    }
}
