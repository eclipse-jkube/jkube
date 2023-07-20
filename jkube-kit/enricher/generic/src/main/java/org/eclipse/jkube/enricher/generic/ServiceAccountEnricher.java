/*
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
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.ServiceAccountConfig;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ServiceAccountEnricher extends BaseEnricher {
    @AllArgsConstructor
    public enum Config implements Configs.Config {
        SKIP_CREATE("skipCreate", "false");

        @Getter
        protected String key;
        @Getter
        protected String defaultValue;
    }

    public ServiceAccountEnricher(JKubeEnricherContext enricherContext) {
        super(enricherContext, "jkube-serviceaccount");
    }

    @Override
    public void create(PlatformMode mode, KubernetesListBuilder builder) {
        Map<String, String> deploymentToSaPair = new HashMap<>();

        // Check config and see if there are any service accounts specified
        ResourceConfig resourceConfig = getConfiguration().getResource();
        if(resourceConfig != null && resourceConfig.getServiceAccounts() != null) {
            deploymentToSaPair.putAll(resourceConfig.getServiceAccounts()
                .stream()
                .filter(sa -> sa.getDeploymentRef() != null)
                .collect(Collectors.toMap(ServiceAccountConfig::getDeploymentRef, ServiceAccountConfig::getName)));
        }
        if (resourceConfig != null && StringUtils.isNotBlank(resourceConfig.getServiceAccount())) {
            deploymentToSaPair.put(JKubeProjectUtil.createDefaultResourceName(getContext().getGav().getSanitizedArtifactId()), resourceConfig.getServiceAccount());
        }
        builder.addAllToServiceAccountItems(createServiceAccountFromResourceConfig(resourceConfig));
        builder.addAllToServiceAccountItems(createServiceAccountsReferencedInDeployment(builder, deploymentToSaPair));
    }

    private List<ServiceAccount> createServiceAccountFromResourceConfig(ResourceConfig resourceConfig) {
        List<ServiceAccount> serviceAccounts = new ArrayList<>();
        if(resourceConfig != null && resourceConfig.getServiceAccounts() != null && !Boolean.parseBoolean(getConfig(Config.SKIP_CREATE))) {
            for(ServiceAccountConfig serviceAccountConfig : resourceConfig.getServiceAccounts()) {
                if(serviceAccountConfig.getName() != null) {
                    serviceAccounts.add(createServiceAccount(serviceAccountConfig.getName()));
                }
            }
        }
        return serviceAccounts;
    }

    private List<ServiceAccount> createServiceAccountsReferencedInDeployment(KubernetesListBuilder builder, Map<String, String> deploymentToSaPair) {
        List<ServiceAccount> serviceAccounts = new ArrayList<>();
        builder.accept(new TypedVisitor<DeploymentBuilder>() {
            @Override
            public void visit(DeploymentBuilder deploymentBuilder) {
                String serviceAccountName = getServiceAccountNameFromSpec(deploymentBuilder);
                if(serviceAccountName != null && getServiceAccountFromList(builder, serviceAccountName) == null
                    && !Boolean.parseBoolean(getConfig(Config.SKIP_CREATE))) {
                    serviceAccounts.add(createServiceAccount(serviceAccountName));
                }
                if(deploymentToSaPair.containsKey(deploymentBuilder.buildMetadata().getName())) {
                    PodSpec podSpec = deploymentBuilder.buildSpec().getTemplate().getSpec();
                    if (StringUtils.isBlank(podSpec.getServiceAccount()) && StringUtils.isBlank(podSpec.getServiceAccountName())) {
                        deploymentBuilder.editSpec()
                            .editTemplate()
                            .editSpec()
                            .withServiceAccountName(deploymentToSaPair.get(deploymentBuilder.buildMetadata().getName()))
                            .endSpec()
                            .endTemplate()
                            .endSpec();
                    }
                }
            }
        });
        return serviceAccounts;
    }

    private ServiceAccount createServiceAccount(String serviceAccountName) {
        return new ServiceAccountBuilder()
                .withNewMetadata().withName(serviceAccountName).endMetadata()
                .build();
    }

    private String getServiceAccountNameFromSpec(DeploymentBuilder builder) {
        if(builder.buildSpec().getTemplate().getSpec().getServiceAccountName() != null) {
            return builder.buildSpec().getTemplate().getSpec().getServiceAccountName();
        }
        if(builder.buildSpec().getTemplate().getSpec().getServiceAccount() != null) {
            return builder.buildSpec().getTemplate().getSpec().getServiceAccount();
        }
        return null;
    }

    private ServiceAccount getServiceAccountFromList(KubernetesListBuilder builder, String serviceAccountName) {
        for(HasMetadata item : builder.buildItems()) {
            if(item instanceof ServiceAccount && item.getMetadata().getName().equals(serviceAccountName)) {
                return (ServiceAccount)item;
            }
        }
        return null;
    }
}
