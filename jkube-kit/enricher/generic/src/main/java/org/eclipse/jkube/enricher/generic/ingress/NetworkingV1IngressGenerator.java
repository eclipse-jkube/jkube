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
package org.eclipse.jkube.enricher.generic.ingress;

import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPathBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressRuleValueBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBackend;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBackendBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRule;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRuleBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressServiceBackend;
import io.fabric8.kubernetes.api.model.networking.v1.IngressServiceBackendBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressSpec;
import io.fabric8.kubernetes.api.model.networking.v1.IngressSpecBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressTLS;
import io.fabric8.kubernetes.api.model.networking.v1.IngressTLSBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.ServiceBackendPortBuilder;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.config.resource.IngressRuleConfig;
import org.eclipse.jkube.kit.config.resource.IngressRulePathConfig;
import org.eclipse.jkube.kit.config.resource.IngressRulePathResourceConfig;
import org.eclipse.jkube.kit.config.resource.IngressTlsConfig;

import java.util.List;

import static org.eclipse.jkube.enricher.generic.DefaultServiceEnricher.getPortToExpose;

public class NetworkingV1IngressGenerator {
    private NetworkingV1IngressGenerator() { }

    public static Ingress generate(ServiceBuilder serviceBuilder, String routeDomainPostfix, String host, List<IngressRuleConfig> ingressRuleConfigs, List<IngressTlsConfig> ingressTlsConfigs) {
        Integer servicePort = getPortToExpose(serviceBuilder);
        if (servicePort != null) {
            return new IngressBuilder()
                    .withNewMetadataLike(serviceBuilder.buildMetadata()).endMetadata()
                    .withSpec(getIngressSpec(routeDomainPostfix, host, serviceBuilder.editOrNewMetadata().getName(), servicePort, ingressRuleConfigs, ingressTlsConfigs))
                    .build();
        }
        return null;
    }

    private static IngressSpec getIngressSpec(String routeDomainPostfix, String host, String serviceName, Integer servicePort, List<IngressRuleConfig> ingressRuleConfig, List<IngressTlsConfig> ingressTlsConfigs) {
        if (ingressRuleConfig == null || ingressRuleConfig.isEmpty()) {
            return getOpinionatedIngressSpec(routeDomainPostfix, host, serviceName, servicePort);
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

    private static IngressSpec getOpinionatedIngressSpec(String routeDomainPostfix, String host, String serviceName, Integer servicePort) {
        IngressSpecBuilder ingressSpecBuilder = new IngressSpecBuilder();
        if (StringUtils.isNotBlank(routeDomainPostfix) || StringUtils.isNotBlank(host)) {
            ingressSpecBuilder.addNewRule()
                    .withHost(resolveIngressHost(serviceName, routeDomainPostfix, host))
                    .withNewHttp()
                    .withPaths(new HTTPIngressPathBuilder()
                            .withPathType("ImplementationSpecific")
                            .withPath("/")
                            .withNewBackend()
                            .withService(getIngressServiceBackend(serviceName, servicePort))
                            .endBackend()
                            .build())
                    .endHttp()
                    .endRule().build();
        } else {
            ingressSpecBuilder.withDefaultBackend(new IngressBackendBuilder()
                    .withService(getIngressServiceBackend(serviceName, servicePort))
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
        if (ingressRulePathConfig.getResource() != null) {
            IngressRulePathResourceConfig resource = ingressRulePathConfig.getResource();
            ingressBackendBuilder.withNewResource(resource.getApiGroup(), resource.getKind(), resource.getName());
        }

        ingressBackendBuilder.withService(getIngressServiceBackend(ingressRulePathConfig.getServiceName(), ingressRulePathConfig.getServicePort()));

        return ingressBackendBuilder.build();
    }

    private static IngressServiceBackend getIngressServiceBackend(String serviceName, int servicePort) {
        IngressServiceBackendBuilder ingressServiceBackendBuilder = new IngressServiceBackendBuilder();
        if (serviceName != null) {
            ingressServiceBackendBuilder.withName(serviceName);
        }
        if (servicePort > 0) {
            ingressServiceBackendBuilder.withPort(new ServiceBackendPortBuilder().withNumber(servicePort).build());
        }
        return ingressServiceBackendBuilder.build();
    }

    static String resolveIngressHost(String serviceName, String routeDomainPostfix, String host) {
        if (host != null) {
            return host;
        }
        return serviceName + "." + FileUtil.stripPrefix(routeDomainPostfix, ".");
    }
}
