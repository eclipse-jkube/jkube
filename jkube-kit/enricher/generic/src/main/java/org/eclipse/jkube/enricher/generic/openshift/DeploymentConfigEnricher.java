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
package org.eclipse.jkube.enricher.generic.openshift;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigSpec;
import io.fabric8.openshift.api.model.DeploymentConfigSpecBuilder;
import io.fabric8.openshift.api.model.DeploymentStrategy;
import io.fabric8.openshift.api.model.DeploymentStrategyBuilder;
import org.eclipse.jkube.enricher.generic.DefaultControllerEnricher;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.EnricherConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Objects;

import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.removeItemFromKubernetesBuilder;


public class DeploymentConfigEnricher extends BaseEnricher {

    private static final String ENRICHER_NAME = "jkube-openshift-deploymentconfig";

    public DeploymentConfigEnricher(JKubeEnricherContext context) {
        super(context, ENRICHER_NAME);
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        if (isApplicable(platformMode)) {
            for(HasMetadata item : builder.buildItems()) {
                if(item instanceof Deployment) {
                    DeploymentConfig deploymentConfig = convertFromAppsV1Deployment(item);
                    removeItemFromKubernetesBuilder(builder, item);
                    builder.addToItems(deploymentConfig);
                } else if (item instanceof io.fabric8.kubernetes.api.model.extensions.Deployment) {
                    DeploymentConfig deploymentConfig = convertFromExtensionsV1Beta1Deployment(item);
                    removeItemFromKubernetesBuilder(builder, item);
                    builder.addToItems(deploymentConfig);
                }
            }
        }
    }

    private boolean isApplicable(PlatformMode platformMode) {
        return platformMode == PlatformMode.openshift
            && !useDeploymentForOpenShift()
            && isNotHandledByDefaultControllerEnricher();
    }

    private boolean isNotHandledByDefaultControllerEnricher() {
        final String type = new EnricherConfig(DefaultControllerEnricher.ENRICHER_NAME, getContext())
            .get(DefaultControllerEnricher.Config.TYPE, "DeploymentConfig");
        return type.equalsIgnoreCase("DeploymentConfig");
    }

    private DeploymentConfig convertFromAppsV1Deployment(HasMetadata item) {
        Deployment resource = (Deployment) item;
        DeploymentConfigBuilder builder = new DeploymentConfigBuilder();
        builder.withMetadata(resource.getMetadata());
        DeploymentSpec spec = resource.getSpec();
        if (spec != null) {
            builder.withSpec(getDeploymentConfigSpec(spec.getReplicas(), spec.getRevisionHistoryLimit(), spec.getSelector(), spec.getTemplate(), spec.getStrategy() != null ? spec.getStrategy().getType() : null));
        }
        return builder.build();
    }

    private DeploymentConfig convertFromExtensionsV1Beta1Deployment(HasMetadata item) {
        io.fabric8.kubernetes.api.model.extensions.Deployment resource = (io.fabric8.kubernetes.api.model.extensions.Deployment) item;
        DeploymentConfigBuilder builder = new DeploymentConfigBuilder();
        builder.withMetadata(resource.getMetadata());
        io.fabric8.kubernetes.api.model.extensions.DeploymentSpec spec = resource.getSpec();
        if (spec != null) {
            builder.withSpec(getDeploymentConfigSpec(spec.getReplicas(), spec.getRevisionHistoryLimit(), spec.getSelector(), spec.getTemplate(), spec.getStrategy() != null ? spec.getStrategy().getType() : null));
        }
        return builder.build();
    }

    private DeploymentConfigSpec getDeploymentConfigSpec(Integer replicas, Integer revisionHistoryLimit, LabelSelector selector, PodTemplateSpec podTemplateSpec, String strategyType) {
        DeploymentConfigSpecBuilder specBuilder = new DeploymentConfigSpecBuilder();
        if (replicas != null) {
            specBuilder.withReplicas(replicas);
        }
        if (revisionHistoryLimit != null) {
            specBuilder.withRevisionHistoryLimit(revisionHistoryLimit);
        }

        if (selector  != null) {
            Map<String, String> matchLabels = selector.getMatchLabels();
            if (matchLabels != null && !matchLabels.isEmpty()) {
                specBuilder.withSelector(matchLabels);
            }
        }
        if (podTemplateSpec != null) {
            specBuilder.withTemplate(podTemplateSpec);
            PodSpec podSpec = podTemplateSpec.getSpec();
            Objects.requireNonNull(podSpec, "No PodSpec for PodTemplate:" + podTemplateSpec);
            Objects.requireNonNull(podSpec, "No containers for PodTemplate.spec: " + podTemplateSpec);
        }
        DeploymentStrategy deploymentStrategy = getDeploymentStrategy(strategyType);
        if (deploymentStrategy != null) {
            specBuilder.withStrategy(deploymentStrategy);
        }

        if(getValueFromConfig(OPENSHIFT_ENABLE_AUTOMATIC_TRIGGER, true)) {
            specBuilder.addNewTrigger().withType("ConfigChange").endTrigger();
        }

        return specBuilder.build();
    }

    private DeploymentStrategy getDeploymentStrategy(String strategyType) {
        final long openshiftDeployTimeoutSeconds = getOpenshiftDeployTimeoutInSeconds(3600L);
        if (openshiftDeployTimeoutSeconds > 0) {
            if (StringUtils.isBlank(strategyType) || "Rolling".equals(strategyType)) {
                return new DeploymentStrategyBuilder().withType("Rolling").
                        withNewRollingParams().withTimeoutSeconds(openshiftDeployTimeoutSeconds).endRollingParams().build();
            } else if ("Recreate".equals(strategyType)) {
                return new DeploymentStrategyBuilder().withType("Recreate").
                        withNewRecreateParams().withTimeoutSeconds(openshiftDeployTimeoutSeconds).endRecreateParams().build();
            } else {
                return new DeploymentStrategyBuilder().withType(strategyType).build();
            }
        } else if (StringUtils.isNotBlank(strategyType)) {
            return new DeploymentStrategyBuilder().withType(strategyType).build();
        }
        return null;
    }
}
