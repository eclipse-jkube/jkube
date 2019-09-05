/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.jshift.enricher.generic;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.jshift.kit.config.resource.PlatformMode;
import io.jshift.kit.config.resource.ResourceConfig;
import io.jshift.kit.config.resource.ServiceAccountConfig;
import io.jshift.maven.enricher.api.BaseEnricher;
import io.jshift.maven.enricher.api.MavenEnricherContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceAccountEnricher extends BaseEnricher {
    public ServiceAccountEnricher(MavenEnricherContext enricherContext) {
        super(enricherContext, "jshift-serviceaccount");
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
