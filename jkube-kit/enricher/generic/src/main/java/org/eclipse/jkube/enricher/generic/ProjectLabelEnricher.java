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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.MapUtil;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigSpec;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Add project labels to any object.
 * For selectors, the 'version' part is removed.
 * <p> The following labels are added:
 * <ul>
 *   <li>version</li>
 *   <li>app</li>
 *   <li>group</li>
 *   <li>provider (is set to jkube)</li>
 * </ul>
 *
 * <p> The "app" label can be replaced with the (old) "project" label using the "useProjectLabel" configuration option.
 *
 * <p> The project labels which are already specified in the input fragments are not overridden by the enricher.
 *
 * @author roland
 */
public class ProjectLabelEnricher extends BaseEnricher {

    public static final String LABEL_PROVIDER = "provider";

    @AllArgsConstructor
    private enum Config implements Configs.Config {
      USE_PROJECT_LABEL("useProjectLabel", "false"),
      APP("app", null);

        @Getter
        protected String key;
        @Getter
        protected String defaultValue;
    }

    public ProjectLabelEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-project-label");
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        builder.accept(new TypedVisitor<ServiceBuilder>() {
            @Override
            public void visit(ServiceBuilder serviceBuilder) {
                Map<String, String> selectors = new HashMap<>();
                if(serviceBuilder.buildSpec() != null && serviceBuilder.buildSpec().getSelector() != null) {
                    selectors.putAll(serviceBuilder.buildSpec().getSelector());
                }
                MapUtil.mergeIfAbsent(selectors, createLabels(true));
                serviceBuilder.editOrNewSpec().addToSelector(selectors).endSpec();
            }
        });

        builder.accept(new TypedVisitor<DeploymentBuilder>() {
            @Override
            public void visit(DeploymentBuilder builder) {
                final Map<String, String> selectors = mergedSelectors(Optional.ofNullable(builder.buildSpec())
                    .map(DeploymentSpec::getSelector)
                    .map(LabelSelector::getMatchLabels)
                    .orElse(new HashMap<>()));
                builder.editOrNewSpec().editOrNewSelector().withMatchLabels(selectors).endSelector().endSpec();
            }
        });

        builder.accept(new TypedVisitor<DeploymentConfigBuilder>() {
            @Override
            public void visit(DeploymentConfigBuilder builder) {
                final Map<String, String> selectors = mergedSelectors(Optional.ofNullable(builder.buildSpec())
                    .map(DeploymentConfigSpec::getSelector)
                    .orElse(new HashMap<>()));
                builder.editOrNewSpec().addToSelector(selectors).endSpec();
            }
        });

        builder.accept(new TypedVisitor<DaemonSetBuilder>() {
            @Override
            public void visit(DaemonSetBuilder builder) {
                Map<String, String> selectors = new HashMap<>();
                if(builder.buildSpec() != null && builder.buildSpec().getSelector() != null && builder.buildSpec().getSelector().getMatchLabels() != null) {
                    selectors.putAll(builder.buildSpec().getSelector().getMatchLabels());
                }
                MapUtil.mergeIfAbsent(selectors, createLabels());
                builder.editOrNewSpec().editOrNewSelector().withMatchLabels(selectors).endSelector().endSpec();
            }
        });

        builder.accept(new TypedVisitor<ReplicationControllerBuilder>() {
            @Override
            public void visit(ReplicationControllerBuilder builder) {
                final Map<String, String> selectors = mergedSelectors(Optional.ofNullable(builder.buildSpec())
                    .map(ReplicationControllerSpec::getSelector)
                    .orElse(new HashMap<>()));
                builder.editOrNewSpec().addToSelector(selectors).endSpec();
            }
        });

        builder.accept(new TypedVisitor<ReplicaSetBuilder>() {
            @Override
            public void visit(ReplicaSetBuilder builder) {
                final Map<String, String> selectors = mergedSelectors(Optional.ofNullable(builder.buildSpec())
                    .map(ReplicaSetSpec::getSelector)
                    .map(LabelSelector::getMatchLabels)
                    .orElse(new HashMap<>()));
                builder.editOrNewSpec().editOrNewSelector().withMatchLabels(selectors).endSelector().endSpec();
            }
        });

        builder.accept(new TypedVisitor<StatefulSetBuilder>() {
            @Override
            public void visit(StatefulSetBuilder builder) {
                Map<String, String> selectors = new HashMap<>();
                if(builder.buildSpec() != null && builder.buildSpec().getSelector() != null && builder.buildSpec().getSelector().getMatchLabels() != null) {
                    selectors.putAll(builder.buildSpec().getSelector().getMatchLabels());
                }
                MapUtil.mergeIfAbsent(selectors, createLabels());
                builder.editOrNewSpec().editOrNewSelector().withMatchLabels(selectors).endSelector().endSpec();
            }
        });

    }

    private Map<String, String> mergedSelectors(Map<String, String> originalSelectors) {
        MapUtil.mergeIfAbsent(originalSelectors, createLabels(true));
        return originalSelectors;
    }

    @Override
    public void enrich(PlatformMode platformMode, KubernetesListBuilder builder) {
        // Add to all objects in the builder
        builder.accept(new TypedVisitor<ObjectMetaBuilder>() {
            @Override
            public void visit(ObjectMetaBuilder element) {
                if (element.getLabels() != null) {
                    Map<String, String> labels = element.getLabels();
                    MapUtil.mergeIfAbsent(labels, createLabels());
                } else {
                    Map<String, String> newLabels = new HashMap<>();
                    MapUtil.mergeIfAbsent(newLabels, createLabels());
                    element.withLabels(newLabels);
                }
            }
        });
    }

    private Map<String, String> createLabels() {
        return createLabels(false);
    }

    private Map<String, String> createLabels(boolean withoutVersion) {
        Map<String, String> ret = new HashMap<>();

        boolean enableProjectLabel = Configs.asBoolean(getConfig(Config.USE_PROJECT_LABEL));
        final GroupArtifactVersion groupArtifactVersion = getContext().getGav();
        if (enableProjectLabel) {
            ret.put("project", groupArtifactVersion.getArtifactId());
        } else {
            ret.put("app", getConfig(Config.APP, groupArtifactVersion.getArtifactId()));
        }

        ret.put("group", groupArtifactVersion.getGroupId());
        ret.put(LABEL_PROVIDER, "jkube");
        if (!withoutVersion) {
            ret.put("version", groupArtifactVersion.getVersion());
        }
        return ret;
    }
}
