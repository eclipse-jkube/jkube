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
package io.jkube.enricher.generic;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.jkube.kit.config.resource.PlatformMode;
import io.jkube.kit.config.resource.ResourceConfig;
import io.jkube.kit.config.resource.ServiceAccountConfig;
import io.jkube.maven.enricher.api.BaseEnricher;
import io.jkube.maven.enricher.api.MavenEnricherContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceAccountEnricher extends BaseEnricher {
    public ServiceAccountEnricher(MavenEnricherContext enricherContext) {
        super(enricherContext, "jkube-serviceaccount");
    }

    @Override
    public void create(PlatformMode mode, KubernetesListBuilder builder) {
        Map<String, String> deploymentToSaPair = new HashMap<>();
        List<ServiceAccount> serviceAccounts = new ArrayList<>();

        // Check XML config and see if there are any service accounts specified
        ResourceConfig xmlResourceConfig = getConfiguration().getResource().orElse(null);
        if(xmlResourceConfig != null && xmlResourceConfig.getServiceAccounts() != null) {
            for(ServiceAccountConfig serviceAccountConfig : xmlResourceConfig.getServiceAccounts()) {
                if(serviceAccountConfig.getName() != null) {
                    serviceAccounts.add(createServiceAccount(builder, serviceAccountConfig.getName()));
                }
                if(serviceAccountConfig.getDeploymentRef() != null) {
                    deploymentToSaPair.put(serviceAccountConfig.getDeploymentRef(), serviceAccountConfig.getName());
                }
            }
        }

        // If any service account is referenced in deployment spec, then
        // create sa on fly.
        builder.accept(new TypedVisitor<DeploymentBuilder>() {
           @Override
           public void visit(DeploymentBuilder deploymentBuilder) {
               String serviceAccountName = getServiceAccountNameFromSpec(deploymentBuilder);
               if(serviceAccountName != null && getServiceAccountFromList(builder, serviceAccountName) == null) {
                   serviceAccounts.add(createServiceAccount(builder, serviceAccountName));
               }
               if(deploymentToSaPair.containsKey(deploymentBuilder.buildMetadata().getName())) {
                   deploymentBuilder.editSpec()
                           .editTemplate()
                           .editSpec()
                           .withServiceAccountName(deploymentToSaPair.get(deploymentBuilder.buildMetadata().getName()))
                           .endSpec()
                           .endTemplate()
                           .endSpec();
               }
           }
        });

        builder.addAllToServiceAccountItems(serviceAccounts);
    }

    private ServiceAccount createServiceAccount(KubernetesListBuilder builder, String serviceAccountName) {
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
