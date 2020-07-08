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
import lombok.AllArgsConstructor;
import lombok.Getter;
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
    @AllArgsConstructor
    private enum Config implements Configs.Config {
        NAMESPACE("namespace", null),
        TYPE("type", "namespace");

        @Getter
        protected String key;
        @Getter
        protected String defaultValue;
    }

    public DefaultNamespaceEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-namespace");

        config = Optional.ofNullable(getConfiguration().getResource()).orElse(ResourceConfig.builder().build());

        handlerHub = new HandlerHub(
                getContext().getGav(), getContext().getProperties());
    }

    /**
     * This method will create a default Namespace or Project if a namespace property is
     * specified in the xml resourceConfig or as a parameter to a mojo.
     * @param platformMode platform mode whether it's Kubernetes or OpenShift
     * @param builder list of kubernetes resources
     */
    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {

        final String ns = getNamespace(config, getConfig(Config.NAMESPACE));

        if (ns == null || ns.isEmpty()) {
            return;
        }

        if (!KubernetesResourceUtil.checkForKind(builder, NAMESPACE_KINDS)) {
            String type = getConfig(Config.TYPE);
            if ("project".equalsIgnoreCase(type) || "namespace".equalsIgnoreCase(type)) {
                if (platformMode == PlatformMode.kubernetes) {
                    log.info("Adding a default Namespace: %s", ns);
                    Namespace namespace = handlerHub.getNamespaceHandler().getNamespace(ns);
                    builder.addToNamespaceItems(namespace);
                } else {
                    log.info("Adding a default Project %s", ns);
                    Project project = handlerHub.getProjectHandler().getProject(ns);
                    builder.addToItems(project);
                }
            }
        }
    }

    /**
     * This method will annotate all the items in the KubernetesListBuilder with the
     * created new namespace or project.
     * @param platformMode platform mode whether it's Kubernetes/OpenShift
     * @param builder list of Kubernetes resources
     */
    @Override
    public void enrich(PlatformMode platformMode, KubernetesListBuilder builder) {
        builder.accept(new TypedVisitor<ObjectMetaBuilder>() {
            private String getNamespaceName() {
                final String defaultValue = builder.buildItems().stream()
                    .filter(item -> NAMESPACE_KINDS_LIST.contains(item.getKind()))
                    .map(HasMetadata::getMetadata)
                    .map(ObjectMeta::getName)
                    .findFirst().orElse(null);
                return getNamespace(config, getConfig(Config.NAMESPACE, defaultValue));
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
        // to avoid unnecessary trouble while applying these resources.
        builder.accept(new TypedVisitor<NamespaceBuilder>() {
            @Override
            public void visit(NamespaceBuilder builder) {
                if (builder.buildStatus().getPhase().equals("active")) {
                    builder.editOrNewStatus().endStatus().build();
                }
            }
        });

        builder.accept(new TypedVisitor<ProjectBuilder>() {
            @Override
            public void visit(ProjectBuilder builder) {
                if (builder.buildStatus().getPhase().equals("active")) {
                    builder.editOrNewStatus().endStatus().build();
                }
            }
        });
    }
}