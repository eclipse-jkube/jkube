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
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.ProjectBuilder;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil;
import org.eclipse.jkube.kit.enricher.handler.HandlerHub;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class DefaultNamespaceEnricher extends BaseEnricher {
    protected static final String[] NAMESPACE_KINDS = {"Project", "Namespace" };
    protected static final List<String> NAMESPACE_KINDS_LIST = Arrays.asList(NAMESPACE_KINDS);

    private final HandlerHub handlerHub;

    private final ResourceConfig config;
    // Available configuration keys
    private enum Config implements Configs.Key {
        name,
        type    {{ d = "namespace"; }};
        public String def() { return d; } protected String d;
    }

    public DefaultNamespaceEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-namespace");

        config = Optional.ofNullable(getConfiguration().getResource()).orElse(ResourceConfig.builder().build());

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
                if (config.getNamespace() != null && !config.getNamespace().isEmpty()) {
                    return config.getNamespace();
                }
                return builder.buildItems().stream()
                    .filter(item -> NAMESPACE_KINDS_LIST.contains(item.getKind()))
                    .map(HasMetadata::getMetadata)
                    .map(ObjectMeta::getName)
                    .findFirst().orElse(null);
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
                builder.withNewStatus().withPhase("Active").endStatus().editMetadata().withNamespace(null).endMetadata().build();
            }
        });

        builder.accept(new TypedVisitor<ProjectBuilder>() {
            @Override
            public void visit(ProjectBuilder builder) {
                builder.withNewStatus().withPhase("Active").endStatus().editMetadata().withNamespace(null).endMetadata().build();
            }
        });
    }
}