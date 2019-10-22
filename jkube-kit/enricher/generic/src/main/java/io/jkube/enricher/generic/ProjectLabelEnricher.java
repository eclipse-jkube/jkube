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
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.jkube.kit.common.Configs;
import io.jkube.kit.common.util.MapUtil;
import io.jkube.kit.config.resource.GroupArtifactVersion;
import io.jkube.kit.config.resource.PlatformMode;
import io.jkube.maven.enricher.api.BaseEnricher;
import io.jkube.maven.enricher.api.MavenEnricherContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Add project labels to any object.
 * For selectors, the 'version' part is removed.
 * <p>
 * The following labels are added:
 * <ul>
 * <li>version</li>
 * <li>app</li>
 * <li>group</li>
 * <li>provider (is set to jkube)</li>
 * </ul>
 *
 * The "app" label can be replaced with the (old) "project" label using the "useProjectLabel" configuraiton option.
 *
 * The project labels which are already specified in the input fragments are not overridden by the enricher.
 *
 * @author roland
 * @since 01/04/16
 */
public class ProjectLabelEnricher extends BaseEnricher {

    // Available configuration keys
    private enum Config implements Configs.Key {

        useProjectLabel {{ d = "false"; }};

        protected String d; public String def() {
            return d;
        }
    }

    public ProjectLabelEnricher(MavenEnricherContext buildContext) {
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
                };
                MapUtil.mergeIfAbsent(selectors, createLabels(true));
                serviceBuilder.editOrNewSpec().addToSelector(selectors).endSpec();
            }
        });

        builder.accept(new TypedVisitor<DeploymentBuilder>() {
            @Override
            public void visit(DeploymentBuilder builder) {
                Map<String, String> selectors = new HashMap<>();
                if(builder.buildSpec() != null && builder.buildSpec().getSelector() != null && builder.buildSpec().getSelector().getMatchLabels() != null) {
                    selectors.putAll(builder.buildSpec().getSelector().getMatchLabels());
                }
                MapUtil.mergeIfAbsent(selectors, createLabels(true));
                builder.editOrNewSpec().editOrNewSelector().withMatchLabels(selectors).endSelector().endSpec();
            }
        });

        builder.accept(new TypedVisitor<DeploymentConfigBuilder>() {
            @Override
            public void visit(DeploymentConfigBuilder builder) {
                Map<String, String> selectors = new HashMap<>();
                if(builder.buildSpec() != null && builder.buildSpec().getSelector() != null) {
                    selectors.putAll(builder.buildSpec().getSelector());
                }
                MapUtil.mergeIfAbsent(selectors, createLabels(true));
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

    @Override
    public void enrich(PlatformMode platformMode, KubernetesListBuilder builder) {
        // Add to all objects in the builder
        builder.accept(new TypedVisitor<ObjectMetaBuilder>() {
            @Override
            public void visit(ObjectMetaBuilder element) {
                Map<String, String> labels = element.getLabels();
                MapUtil.mergeIfAbsent(labels, createLabels());
            }
        });
    }

    private Map<String, String> createLabels() {
        return createLabels(false);
    }

    private Map<String, String> createLabels(boolean withoutVersion) {
        Map<String, String> ret = new HashMap<>();

        boolean enableProjectLabel = Configs.asBoolean(getConfig(Config.useProjectLabel));
        final GroupArtifactVersion groupArtifactVersion = getContext().getGav();
        if (enableProjectLabel) {
            ret.put("project", groupArtifactVersion.getArtifactId());
        } else {
            // default label is app
            ret.put("app", groupArtifactVersion.getArtifactId());
        }

        ret.put("group", groupArtifactVersion.getGroupId());
        ret.put("provider", "jkube");
        if (!withoutVersion) {
            ret.put("version", groupArtifactVersion.getVersion());
        }
        return ret;
    }
}
