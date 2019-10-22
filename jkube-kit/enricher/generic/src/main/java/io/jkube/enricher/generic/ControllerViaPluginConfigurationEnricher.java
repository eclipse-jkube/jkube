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
package io.jkube.enricher.generic;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentFluent;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetFluent;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.jkube.kit.build.service.docker.ImageConfiguration;
import io.jkube.kit.common.Configs;
import io.jkube.kit.common.util.MavenUtil;
import io.jkube.kit.config.resource.PlatformMode;
import io.jkube.kit.config.resource.ResourceConfig;
import io.jkube.maven.enricher.api.BaseEnricher;
import io.jkube.maven.enricher.api.MavenEnricherContext;
import io.jkube.maven.enricher.api.util.KubernetesResourceUtil;
import io.jkube.maven.enricher.handler.DeploymentHandler;
import io.jkube.maven.enricher.handler.HandlerHub;
import io.jkube.maven.enricher.handler.StatefulSetHandler;

import java.util.Collections;
import java.util.List;

public class ControllerViaPluginConfigurationEnricher extends BaseEnricher {
    protected static final String[] POD_CONTROLLER_KINDS =
            { "ReplicationController", "ReplicaSet", "Deployment", "DeploymentConfig", "StatefulSet", "DaemonSet", "Job" };

    private final DeploymentHandler deployHandler;
    private final StatefulSetHandler statefulSetHandler;

    // Available configuration keys
    private enum Config implements Configs.Key {
        name,
        pullPolicy           {{ d = "IfNotPresent"; }},
        type                 {{ d = "deployment"; }},
        replicaCount         {{ d = "1"; }};

        public String def() { return d; } protected String d;
    }

    public ControllerViaPluginConfigurationEnricher(MavenEnricherContext context) {
        super(context, "jkube-controller-from-configuration");
        HandlerHub handlers = new HandlerHub(
                getContext().getGav(), getContext().getConfiguration().getProperties());
        deployHandler = handlers.getDeploymentHandler();
        statefulSetHandler = handlers.getStatefulSetHandler();
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        final String name = getConfig(Config.name, MavenUtil.createDefaultResourceName(getContext().getGav().getSanitizedArtifactId()));
        ResourceConfig xmlResourceConfig = getConfiguration().getResource().orElse(null);
        final ResourceConfig config = new ResourceConfig.Builder()
                .controllerName(name)
                .imagePullPolicy(getImagePullPolicy(xmlResourceConfig, getConfig(Config.pullPolicy)))
                .withReplicas(getReplicaCount(builder, xmlResourceConfig, Configs.asInt(getConfig(Config.replicaCount))))
                .build();

        final List<ImageConfiguration> images = getImages().orElse(Collections.emptyList());

        // Check if at least a replica set is added. If not add a default one
        if (KubernetesResourceUtil.checkForKind(builder, POD_CONTROLLER_KINDS)) {
            // At least one image must be present, otherwise the resulting config will be invalid
            if (KubernetesResourceUtil.checkForKind(builder, "StatefulSet")) {
                final StatefulSetSpec spec = statefulSetHandler.getStatefulSet(config, images).getSpec();
                if (spec != null) {
                    builder.accept(new TypedVisitor<StatefulSetBuilder>() {
                        @Override
                        public void visit(StatefulSetBuilder statefulSetBuilder) {
                            statefulSetBuilder.editOrNewSpec().editOrNewTemplate().editOrNewSpec().endSpec().endTemplate().endSpec();
                            mergeStatefulSetSpec(statefulSetBuilder, spec);
                        }
                    });

                    if (spec.getTemplate() != null && spec.getTemplate().getSpec() != null) {
                        final PodSpec podSpec = spec.getTemplate().getSpec();
                        builder.accept(new TypedVisitor<PodSpecBuilder>() {
                            @Override
                            public void visit(PodSpecBuilder builder) {
                                String defaultApplicationContainerName = KubernetesResourceUtil.mergePodSpec(builder, podSpec, name, getValueFromConfig(SIDECAR, false));
                                if(defaultApplicationContainerName != null) {
                                    setProcessingInstruction(NEED_IMAGECHANGE_TRIGGERS, Collections.singletonList(defaultApplicationContainerName));
                                }
                            }
                        });
                    }
                }
            } else {
                final DeploymentSpec spec = deployHandler.getDeployment(config, images).getSpec();
                if (spec != null) {
                    builder.accept(new TypedVisitor<DeploymentBuilder>() {
                        @Override
                        public void visit(DeploymentBuilder deploymentBuilder) {
                            deploymentBuilder.editOrNewSpec().editOrNewTemplate().editOrNewSpec().endSpec().endTemplate().endSpec();
                            mergeDeploymentSpec(deploymentBuilder, spec);
                        }
                    });

                    if (spec.getTemplate() != null && spec.getTemplate().getSpec() != null) {
                        final PodSpec podSpec = spec.getTemplate().getSpec();
                        builder.accept(new TypedVisitor<PodSpecBuilder>() {
                            @Override
                            public void visit(PodSpecBuilder builder) {
                                String defaultApplicationContainerName = KubernetesResourceUtil.mergePodSpec(builder, podSpec, name, getValueFromConfig(SIDECAR, false));
                                if(defaultApplicationContainerName != null) {
                                    setProcessingInstruction(NEED_IMAGECHANGE_TRIGGERS, Collections.singletonList(defaultApplicationContainerName));
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    private void mergeDeploymentSpec(DeploymentBuilder builder, DeploymentSpec spec) {
        DeploymentFluent.SpecNested<DeploymentBuilder> specBuilder = builder.editSpec();
        KubernetesResourceUtil.mergeSimpleFields(specBuilder, spec);
        specBuilder.endSpec();
    }

    private void mergeStatefulSetSpec(StatefulSetBuilder builder, StatefulSetSpec spec) {
        StatefulSetFluent.SpecNested<StatefulSetBuilder> specBuilder = builder.editSpec();
        KubernetesResourceUtil.mergeSimpleFields(specBuilder, spec);
        specBuilder.endSpec();
    }

}
