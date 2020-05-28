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
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressPathBuilder;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressBackendBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressSpecBuilder;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.config.resource.JKubeAnnotations;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.containsLabelInMetadata;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.removeLabel;

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
                    Ingress ingress = addIngress(listBuilder, serviceBuilder, getRouteDomain(resourceConfig), log);
                    if (ingress != null) {
                        listBuilder.addToItems(ingress);
                    }
                }
            });
        }
    }

    protected static Ingress addIngress(KubernetesListBuilder listBuilder, ServiceBuilder serviceBuilder, String routeDomainPostfix, KitLogger log) {
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

                    IngressBuilder ingressBuilder = new IngressBuilder().
                            withMetadata(serviceMetadata).
                            withNewSpec().
                            endSpec();

                    // removing `expose : true` label from metadata.
                    removeLabel(ingressBuilder.buildMetadata(), EXPOSE_LABEL, "true");
                    removeLabel(ingressBuilder.buildMetadata(), JKubeAnnotations.SERVICE_EXPOSE_URL.value(), "true");
                    ingressBuilder.withNewMetadataLike(ingressBuilder.buildMetadata());

                    if (StringUtils.isNotBlank(routeDomainPostfix)) {
                        routeDomainPostfix = serviceName + "." + FileUtil.stripPrefix(routeDomainPostfix, ".");
                        ingressBuilder = ingressBuilder.withSpec(new IngressSpecBuilder().addNewRule().
                                withHost(routeDomainPostfix).
                                withNewHttp().
                                withPaths(new HTTPIngressPathBuilder()
                                        .withNewBackend()
                                        .withServiceName(serviceName)
                                        .withServicePort(KubernetesHelper.createIntOrString(getServicePort(serviceBuilder)))
                                        .endBackend()
                                        .build())
                                .endHttp().
                                        endRule().build());
                    } else {
                        ingressBuilder.withSpec(new IngressSpecBuilder().withBackend(new IngressBackendBuilder().
                                withNewServiceName(serviceName)
                                .withNewServicePort(getServicePort(serviceBuilder))
                                .build()).build());
                    }

                    return ingressBuilder.build();
                }
            }
        }
        return null;
    }

    private static Integer getServicePort(ServiceBuilder serviceBuilder) {
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
     * <p>
     * By default lets ignore the kubernetes services and any service which does not expose ports 80 and 443
     *
     * @return true if we should create an Ingress for this service.
     */
    private static boolean shouldCreateExternalURLForService(ServiceBuilder service, KitLogger log) {
        String serviceName = service.buildMetadata().getName();
        ServiceSpec spec = service.buildSpec();
        if (spec != null && !isKuberentesSystemService(serviceName)) {
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

    private static boolean isKuberentesSystemService(String serviceName) {
        return "kubernetes".equals(serviceName) || "kubernetes-ro".equals(serviceName);
    }

    private String getRouteDomain(ResourceConfig resourceConfig) {
        if (resourceConfig != null && resourceConfig.getRouteDomain() != null) {
            return resourceConfig.getRouteDomain();
        }
        String routeDomainFromProperties = getValueFromConfig(JKUBE_DOMAIN, "");
        if (StringUtils.isNotEmpty(routeDomainFromProperties)) {
            return routeDomainFromProperties;
        }
        return null;
    }

    private static boolean isExposedService(ObjectMeta objectMeta) {
        return containsLabelInMetadata(objectMeta, EXPOSE_LABEL, "true") ||
                containsLabelInMetadata(objectMeta, JKubeAnnotations.SERVICE_EXPOSE_URL.value(), "true");
    }
}
