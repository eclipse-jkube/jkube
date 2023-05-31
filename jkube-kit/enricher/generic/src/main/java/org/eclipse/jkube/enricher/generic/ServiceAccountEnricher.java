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
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.apps.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.Configs;
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
        // Check config and see if there are any service accounts specified
        ResourceConfig resourceConfig = getConfiguration().getResource();

        Map<String, String> controllerToSaPair = createControllerToServiceAccountMapping(resourceConfig);
        builder.addAllToServiceAccountItems(createServiceAccountFromResourceConfig(resourceConfig));
        builder.addAllToServiceAccountItems(createServiceAccountsReferencedInPodTemplateSpec(builder));
        bindServiceAccountToControllers(builder, resourceConfig, controllerToSaPair);
    }

    private List<ServiceAccount> createServiceAccountFromResourceConfig(ResourceConfig resourceConfig) {
        List<ServiceAccount> serviceAccounts = new ArrayList<>();
        if(resourceConfig != null && resourceConfig.getServiceAccounts() != null && !Boolean.parseBoolean(getConfig(Config.SKIP_CREATE))) {
            for(ServiceAccountConfig serviceAccountConfig : resourceConfig.getServiceAccounts()) {
                if (shouldCreateNewServiceAccount(serviceAccountConfig)) {
                    serviceAccounts.add(createServiceAccount(serviceAccountConfig.getName()));
                }
            }
        }
        return serviceAccounts;
    }

    private List<ServiceAccount> createServiceAccountsReferencedInPodTemplateSpec(KubernetesListBuilder builder) {
        List<ServiceAccount> serviceAccounts = new ArrayList<>();
        builder.accept(new TypedVisitor<PodTemplateSpecBuilder>() {
            @Override
            public void visit(PodTemplateSpecBuilder podTemplateSpecBuilder) {
                String serviceAccountName = getServiceAccountNameFromSpec(podTemplateSpecBuilder.build());
                if (shouldCreateNewServiceAccount(builder, serviceAccountName)) {
                    serviceAccounts.add(createServiceAccount(serviceAccountName));
                }
            }
        });
        return serviceAccounts;
    }

    private boolean shouldCreateNewServiceAccount(ServiceAccountConfig serviceAccountConfig) {
        if (serviceAccountConfig.getName() != null) {
            if (serviceAccountConfig.getGenerate() != null) {
                return serviceAccountConfig.getGenerate();
            }
            return true;
        }
        return false;
    }

    private boolean shouldCreateNewServiceAccount(KubernetesListBuilder builder, String serviceAccountName) {
        return serviceAccountName != null &&
            getServiceAccountFromList(builder, serviceAccountName) == null &&
            !Boolean.parseBoolean(getConfig(Config.SKIP_CREATE));
    }

    private ServiceAccount createServiceAccount(String serviceAccountName) {
        return new ServiceAccountBuilder()
                .withNewMetadata().withName(serviceAccountName).endMetadata()
                .build();
    }

    private String getServiceAccountNameFromSpec(PodTemplateSpec podTemplateSpec) {
        if (podTemplateSpec.getSpec().getServiceAccountName() != null) {
            return podTemplateSpec.getSpec().getServiceAccountName();
        }
        if (podTemplateSpec.getSpec().getServiceAccount() != null) {
            return podTemplateSpec.getSpec().getServiceAccount();
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

    private Map<String, String> createControllerToServiceAccountMapping(ResourceConfig resourceConfig) {
        Map<String, String> deploymentToSaPair = new HashMap<>();
        if (resourceConfig != null && resourceConfig.getServiceAccounts() != null) {
            deploymentToSaPair.putAll(resourceConfig.getServiceAccounts()
                .stream()
                .filter(sa -> sa.getDeploymentRef() != null)
                .collect(Collectors.toMap(ServiceAccountConfig::getDeploymentRef, ServiceAccountConfig::getName)));
        }
        return deploymentToSaPair;
    }

    private void bindServiceAccountToControllers(KubernetesListBuilder builder, ResourceConfig resourceConfig, Map<String, String> controllerToSaPair) {
        String serviceAccountToBind = getApplicableServiceAccountToBindAll(resourceConfig);
        if (StringUtils.isNotBlank(serviceAccountToBind)) {
            bindServiceAccountToAllControllers(builder, serviceAccountToBind);
        }
        bindServiceAccountToSpecificControllers(builder, controllerToSaPair);
    }

    private void bindServiceAccountToSpecificControllers(KubernetesListBuilder builder, Map<String, String> controllerToSaPair) {
        builder.accept(new TypedVisitor<DeploymentBuilder>() {
            @Override
            public void visit(DeploymentBuilder deploymentBuilder) {
                if (controllerToSaPair.containsKey(deploymentBuilder.buildMetadata().getName())) {
                    deploymentBuilder.editSpec()
                        .editTemplate()
                        .editSpec()
                        .withServiceAccountName(controllerToSaPair.get(deploymentBuilder.buildMetadata().getName()))
                        .endSpec()
                        .endTemplate()
                        .endSpec();
                }
            }
        });
        builder.accept(new TypedVisitor<StatefulSetBuilder>() {
            @Override
            public void visit(StatefulSetBuilder statefulSetBuilder) {
                if (controllerToSaPair.containsKey(statefulSetBuilder.buildMetadata().getName())) {
                    statefulSetBuilder.editSpec()
                        .editTemplate()
                        .editSpec()
                        .withServiceAccountName(controllerToSaPair.get(statefulSetBuilder.buildMetadata().getName()))
                        .endSpec()
                        .endTemplate()
                        .endSpec();
                }
            }
        });
        builder.accept(new TypedVisitor<DaemonSetBuilder>() {
            @Override
            public void visit(DaemonSetBuilder daemonSetBuilder) {
                if (controllerToSaPair.containsKey(daemonSetBuilder.buildMetadata().getName())) {
                    daemonSetBuilder.editSpec()
                        .editTemplate()
                        .editSpec()
                        .withServiceAccountName(controllerToSaPair.get(daemonSetBuilder.buildMetadata().getName()))
                        .endSpec()
                        .endTemplate()
                        .endSpec();
                }
            }
        });
        builder.accept(new TypedVisitor<ReplicaSetBuilder>() {
            @Override
            public void visit(ReplicaSetBuilder replicaSetBuilder) {
                if (controllerToSaPair.containsKey(replicaSetBuilder.buildMetadata().getName())) {
                    replicaSetBuilder.editSpec()
                        .editTemplate()
                        .editSpec()
                        .withServiceAccountName(controllerToSaPair.get(replicaSetBuilder.buildMetadata().getName()))
                        .endSpec()
                        .endTemplate()
                        .endSpec();
                }
            }
        });

        builder.accept(new TypedVisitor<ReplicationControllerBuilder>() {
            @Override
            public void visit(ReplicationControllerBuilder replicationControllerBuilder) {
                if (controllerToSaPair.containsKey(replicationControllerBuilder.buildMetadata().getName())) {
                    replicationControllerBuilder.editSpec()
                        .editTemplate()
                        .editSpec()
                        .withServiceAccountName(controllerToSaPair.get(replicationControllerBuilder.buildMetadata().getName()))
                        .endSpec()
                        .endTemplate()
                        .endSpec();
                }
            }
        });
        builder.accept(new TypedVisitor<JobBuilder>() {
            @Override
            public void visit(JobBuilder jobBuilder) {
                if (controllerToSaPair.containsKey(jobBuilder.buildMetadata().getName())) {
                    jobBuilder.editSpec()
                        .editTemplate()
                        .editSpec()
                        .withServiceAccountName(controllerToSaPair.get(jobBuilder.buildMetadata().getName()))
                        .endSpec()
                        .endTemplate()
                        .endSpec();
                }
            }
        });
    }

    private void bindServiceAccountToAllControllers(KubernetesListBuilder builder, String serviceAccountToBind) {
        builder.accept(new TypedVisitor<PodTemplateSpecBuilder>() {
            @Override
            public void visit(PodTemplateSpecBuilder podTemplateSpecBuilder) {
                if (StringUtils.isBlank(podTemplateSpecBuilder.buildSpec().getServiceAccountName()) &&
                    StringUtils.isBlank(podTemplateSpecBuilder.buildSpec().getServiceAccount())) {
                    podTemplateSpecBuilder.editSpec()
                        .withServiceAccountName(serviceAccountToBind)
                        .endSpec();
                }
            }
        });
    }

    private String getApplicableServiceAccountToBindAll(ResourceConfig resourceConfig) {
        if (resourceConfig != null && resourceConfig.getServiceAccounts() != null) {
            return resourceConfig.getServiceAccounts().stream()
                .filter(s -> s.getBindToAllControllers() != null && s.getBindToAllControllers().equals(Boolean.TRUE))
                .findFirst()
                .map(ServiceAccountConfig::getName)
                .orElse(null);
        }
        return null;
    }
}
