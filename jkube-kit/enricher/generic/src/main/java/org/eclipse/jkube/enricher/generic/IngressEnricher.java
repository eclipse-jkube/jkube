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
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.enricher.generic.ingress.ExtensionsV1beta1IngressConverter;
import org.eclipse.jkube.enricher.generic.ingress.NetworkingV1IngressGenerator;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.SummaryUtil;
import org.eclipse.jkube.kit.config.resource.IngressConfig;
import org.eclipse.jkube.kit.config.resource.IngressRuleConfig;
import org.eclipse.jkube.kit.config.resource.IngressTlsConfig;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.ServiceExposer;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enricher which generates an Ingress for every applicable service whenever the <code>jkube.createExternalUrls</code>
 * property is set to true.
 */
public class IngressEnricher extends BaseEnricher implements ServiceExposer {

    @AllArgsConstructor
    public enum Config implements Configs.Config {
        HOST("host", null),
        TARGET_API_VERSION("targetApiVersion", "networking.k8s.io/v1");

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
        if (!getCreateExternalUrls()) {
            return;
        }
        if (platformMode == PlatformMode.kubernetes) {
            listBuilder.accept(new TypedVisitor<ServiceBuilder>() {
                @Override
                public void visit(ServiceBuilder serviceBuilder) {
                    if (!canExposeService(serviceBuilder)) {
                        getLog().debug("Service %s cannot be exposed",
                          serviceBuilder.editOrNewMetadata().getName());
                        return;
                    }
                    if (hasIngressForService(listBuilder, serviceBuilder)) {
                        getLog().debug("Service %s already has an Ingress",
                          serviceBuilder.editOrNewMetadata().getName());
                        return;
                    }
                    HasMetadata generatedIngress = generateIngressWithConfiguredApiVersion(serviceBuilder);
                    if (generatedIngress != null) {
                        SummaryUtil.addToEnrichers(getName());
                        listBuilder.addToItems(generatedIngress);
                    }
                }
            });
            logHintIfNoDomainOrHostProvided();
        }
    }

    private HasMetadata generateIngressWithConfiguredApiVersion(ServiceBuilder serviceBuilder) {
        ResourceConfig resourceConfig = getConfiguration().getResource();
        io.fabric8.kubernetes.api.model.networking.v1.Ingress ingress = NetworkingV1IngressGenerator.generate(
          serviceBuilder, getRouteDomain(), getConfig(Config.HOST), getIngressRuleXMLConfig(resourceConfig), getIngressTlsXMLConfig(resourceConfig));
        HasMetadata generatedIngress = ingress;

        String targetIngressApiVersion = getConfig(Config.TARGET_API_VERSION);
        if (targetIngressApiVersion.equalsIgnoreCase("extensions/v1beta1")) {
            generatedIngress = ExtensionsV1beta1IngressConverter.convert(ingress);
        }
        return generatedIngress;
    }


    /**
     * Returns true if we already have an ingress created for the given name
     */
    private static boolean hasIngressForService(final KubernetesListBuilder listBuilder, final ServiceBuilder service) {
        final String serviceName = service.editOrNewMetadata().getName();
        final AtomicBoolean answer = new AtomicBoolean(false);
        listBuilder.accept(new TypedVisitor<io.fabric8.kubernetes.api.model.extensions.IngressBuilder>() {

            @Override
            public void visit(io.fabric8.kubernetes.api.model.extensions.IngressBuilder builder) {
                ObjectMeta metadata = builder.buildMetadata();
                if (metadata != null && Objects.equals(serviceName, metadata.getName())) {
                    answer.set(true);
                }
            }
        });
        listBuilder.accept(new TypedVisitor<io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder>() {
            @Override
            public void visit(io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder builder) {
                ObjectMeta metadata = builder.buildMetadata();
                if (metadata != null && Objects.equals(serviceName, metadata.getName())) {
                    answer.set(true);
                }
            }
        });
        return answer.get();
    }


    protected String getRouteDomain() {
        if (getConfiguration().getResource() != null && StringUtils.isNotEmpty(getConfiguration().getResource().getRouteDomain())) {
            return getConfiguration().getResource().getRouteDomain();
        }
        String routeDomainFromProperties = getValueFromConfig(JKUBE_DOMAIN, "");
        if (StringUtils.isNotEmpty(routeDomainFromProperties)) {
            return routeDomainFromProperties;
        }
        return null;
    }

    void logHintIfNoDomainOrHostProvided() {
        ResourceConfig resourceConfig = getContext().getConfiguration().getResource();
        if (resourceConfig != null && resourceConfig.getIngress() != null) {
            logHintIfNoDomainOrHostForResourceConfig(resourceConfig);
        } else if (StringUtils.isBlank(getRouteDomain()) && StringUtils.isBlank(getConfig(Config.HOST))) {
            logJKubeDomainHint();
        }
    }

    private void logHintIfNoDomainOrHostForResourceConfig(ResourceConfig resourceConfig) {
        List<IngressRuleConfig> ingressRuleConfigs = getIngressRuleXMLConfig(resourceConfig);
        if (!ingressRuleConfigs.isEmpty()) {
            Optional<String> configuredHost = ingressRuleConfigs.stream()
                .map(IngressRuleConfig::getHost)
                .filter(StringUtils::isNotBlank)
                .findAny();
            if (!configuredHost.isPresent()) {
                logJKubeDomainHint();
            }
        }
    }

    private void logJKubeDomainHint() {
        getContext().getLog().info("[[B]]HINT:[[B]] No host configured for Ingress. You might want to use `jkube.domain` to add desired host suffix");
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
