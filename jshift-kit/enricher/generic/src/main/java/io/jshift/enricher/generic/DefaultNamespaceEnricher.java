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
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.ProjectBuilder;
import io.jshift.kit.common.Configs;
import io.jshift.kit.config.resource.PlatformMode;
import io.jshift.kit.config.resource.ResourceConfig;
import io.jshift.maven.enricher.api.BaseEnricher;
import io.jshift.maven.enricher.api.MavenEnricherContext;
import io.jshift.maven.enricher.api.util.KubernetesResourceUtil;
import io.jshift.maven.enricher.handler.HandlerHub;

import java.util.Arrays;

public class DefaultNamespaceEnricher extends BaseEnricher {
    protected static final String[] NAMESPACE_KINDS = {"Project", "Namespace" };

    private final HandlerHub handlerHub;

    private final ResourceConfig config;
    // Available configuration keys
    private enum Config implements Configs.Key {
        name,
        type    {{ d = "namespace"; }};
        public String def() { return d; } protected String d;
    }

    public DefaultNamespaceEnricher(MavenEnricherContext buildContext) {
        super(buildContext, "jshift-namespace");

        ResourceConfig xmlResourceConfig = getConfiguration().getResource().orElse(null);
        config = new ResourceConfig.Builder(xmlResourceConfig)
                .build();

        handlerHub = new HandlerHub(
                getContext().getGav(), getContext().getConfiguration().getProperties());
    }

    /**
     * This method will create a default Namespace or Project if a namespace property is
     * specified in the xml resourceConfig or as a parameter to a mojo.
     * @param platformMode platform mode whether it's Kubernetes or Openshift
     * @param builder list of kubernetes resources
     */
    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {

        final String name = config.getNamespace();

        if (name == null || name.isEmpty()) {
            return;
        }

        if (!KubernetesResourceUtil.checkForKind(builder, NAMESPACE_KINDS)) {
            String type = getConfig(Config.type);
            if ("project".equalsIgnoreCase(type) || "namespace".equalsIgnoreCase(type)) {
                if (platformMode == PlatformMode.kubernetes) {

                    log.info("Adding a default Namespace:" + config.getNamespace());
                    Namespace namespace = handlerHub.getNamespaceHandler().getNamespace(config.getNamespace());
                    builder.addToNamespaceItems(namespace);
                } else {

                    log.info("Adding a default Project" + config.getNamespace());
                    Project project = handlerHub.getProjectHandler().getProject(config.getNamespace());
                    builder.addToProjectItems(project);
                }
            }
        }
    }

    /**
     * This method will annotate all the items in the KubernetesListBuilder with the
     * created new namespace or project.
     * @param platformMode platform mode whether it's Kubernetes/Openshift
     * @param builder list of Kubernetes resources
     */
    @Override
    public void enrich(PlatformMode platformMode, KubernetesListBuilder builder) {

        builder.accept(new TypedVisitor<ObjectMetaBuilder>() {
            private String getNamespaceName() {
                String name = null;
                if (config.getNamespace() != null && !config.getNamespace().isEmpty()) {
                    name = config.getNamespace();
                }

                name = builder.getItems().stream()
                        .filter(item -> Arrays.asList(NAMESPACE_KINDS).contains(item.getKind()))
                        .findFirst().get().getMetadata().getName();

                return name;
            }

            @Override
            public void visit(ObjectMetaBuilder metaBuilder) {
                if (!KubernetesResourceUtil.checkForKind(builder, NAMESPACE_KINDS)) {
                    return;
                }

                String name = getNamespaceName();
                if (name == null || name.isEmpty()) {
                    return;
                }

                metaBuilder.withNamespace(name).build();
            }
        });

        // Removing namespace annotation from the namespace and project objects being generated.
        // to avoid unncessary trouble while applying these resources.
        builder.accept(new TypedVisitor<NamespaceBuilder>() {
            @Override
            public void visit(NamespaceBuilder builder) {
                builder.withNewStatus("active").editMetadata().withNamespace(null).endMetadata().build();
            }
        });

        builder.accept(new TypedVisitor<ProjectBuilder>() {
            @Override
            public void visit(ProjectBuilder builder) {
                builder.withNewStatus("active").editMetadata().withNamespace(null).endMetadata().build();
            }
        });
    }
}