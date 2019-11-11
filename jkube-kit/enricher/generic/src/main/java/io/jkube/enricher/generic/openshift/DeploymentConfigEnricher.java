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
package io.jkube.enricher.generic.openshift;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentStrategy;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigFluent;
import io.jkube.kit.config.resource.PlatformMode;
import io.jkube.maven.enricher.api.BaseEnricher;
import io.jkube.maven.enricher.api.MavenEnricherContext;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.jkube.maven.enricher.api.util.KubernetesResourceUtil.removeItemFromKubernetesBuilder;


public class DeploymentConfigEnricher extends BaseEnricher {
    static final String ENRICHER_NAME = "jkube-openshift-deploymentconfig";
    private Boolean enableAutomaticTrigger;
    private Long openshiftDeployTimeoutSeconds;

    public DeploymentConfigEnricher(MavenEnricherContext context) {
        super(context, ENRICHER_NAME);
        this.enableAutomaticTrigger = getValueFromConfig(OPENSHIFT_ENABLE_AUTOMATIC_TRIGGER, true);;
        this.openshiftDeployTimeoutSeconds =  getOpenshiftDeployTimeoutInSeconds(3600L);
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {

        if(platformMode == PlatformMode.openshift) {

            for(HasMetadata item : builder.buildItems()) {
                if(item instanceof Deployment && !useDeploymentforOpenShift()) {
                    DeploymentConfig deploymentConfig = convert(item);
                    removeItemFromKubernetesBuilder(builder, item);
                    builder.addToDeploymentConfigItems(deploymentConfig);
                }
            }
        }
    }

    private DeploymentConfig convert(HasMetadata item) {
        Deployment resource = (Deployment) item;
        DeploymentConfigBuilder builder = new DeploymentConfigBuilder();
        builder.withMetadata(resource.getMetadata());
        DeploymentSpec spec = resource.getSpec();
        if (spec != null) {
            DeploymentConfigFluent.SpecNested<DeploymentConfigBuilder> specBuilder = builder.withNewSpec();
            Integer replicas = spec.getReplicas();
            if (replicas != null) {
                specBuilder.withReplicas(replicas);
            }
            Integer revisionHistoryLimit = spec.getRevisionHistoryLimit();
            if (revisionHistoryLimit != null) {
                specBuilder.withRevisionHistoryLimit(revisionHistoryLimit);
            }

            LabelSelector selector = spec.getSelector();
            if (selector  != null) {
                Map<String, String> matchLabels = selector.getMatchLabels();
                if (matchLabels != null && !matchLabels.isEmpty()) {
                    specBuilder.withSelector(matchLabels);
                }
            }
            PodTemplateSpec template = spec.getTemplate();
            if (template != null) {
                specBuilder.withTemplate(template);
                PodSpec podSpec = template.getSpec();
                Objects.requireNonNull(podSpec, "No PodSpec for PodTemplate:" + template);
                List<Container> containers = podSpec.getContainers();
                Objects.requireNonNull(podSpec, "No containers for PodTemplate.spec: " + template);
            }
            DeploymentStrategy strategy = spec.getStrategy();
            String strategyType = null;
            if (strategy != null) {
                strategyType = strategy.getType();
            }
            if (openshiftDeployTimeoutSeconds != null && openshiftDeployTimeoutSeconds > 0) {
                if (StringUtils.isBlank(strategyType) || "Rolling".equals(strategyType)) {
                    specBuilder.withNewStrategy().withType("Rolling").
                            withNewRollingParams().withTimeoutSeconds(openshiftDeployTimeoutSeconds).endRollingParams().endStrategy();
                } else if ("Recreate".equals(strategyType)) {
                    specBuilder.withNewStrategy().withType("Recreate").
                            withNewRecreateParams().withTimeoutSeconds(openshiftDeployTimeoutSeconds).endRecreateParams().endStrategy();
                } else {
                    specBuilder.withNewStrategy().withType(strategyType).endStrategy();
                }
            } else if (StringUtils.isNotBlank(strategyType)) {
                // TODO is there any values we can copy across?
                specBuilder.withNewStrategy().withType(strategyType).endStrategy();
            }

            if(enableAutomaticTrigger) {
                specBuilder.addNewTrigger().withType("ConfigChange").endTrigger();
            }

            specBuilder.endSpec();
        }
        return builder.build();
    }
}
