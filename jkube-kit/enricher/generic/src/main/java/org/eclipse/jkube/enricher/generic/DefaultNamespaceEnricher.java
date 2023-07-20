/*
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
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.NamespaceStatus;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.openshift.api.model.ProjectBuilder;
import io.fabric8.openshift.api.model.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.EnricherContext;
import org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil;

import java.util.Optional;

public class DefaultNamespaceEnricher extends BaseEnricher {

    private static final String NAMESPACE = "namespace";
    protected static final String[] NAMESPACE_KINDS = {"Project", "Namespace" };

    private final ResourceConfig config;
    @AllArgsConstructor
    private enum Config implements Configs.Config {
        NAMESPACE(DefaultNamespaceEnricher.NAMESPACE, null),
        FORCE("force", "false"),
        TYPE("type", DefaultNamespaceEnricher.NAMESPACE);

        @Getter
        protected String key;
        @Getter
        protected String defaultValue;
    }

    public DefaultNamespaceEnricher(EnricherContext buildContext) {
        super(buildContext, "jkube-namespace");

        config = Optional.ofNullable(getConfiguration().getResource()).orElse(ResourceConfig.builder().build());
    }

    /**
     * This method will create a default Namespace or Project if a namespace property is
     * specified in the xml resourceConfig or as a parameter to a mojo.
     * @param platformMode platform mode whether it's Kubernetes or OpenShift
     * @param builder list of kubernetes resources
     */
    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        String newNamespaceToCreate = getConfig(Config.NAMESPACE, null);

        if (StringUtils.isEmpty(newNamespaceToCreate)) {
            return;
        }

        addNewNamespaceToBuilderIfProvided(platformMode, newNamespaceToCreate, builder);
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
                return getNamespace(config, null);
            }

            private boolean shouldConfigureNamespaceInMetadata() {
                return StringUtils.isNotEmpty(getNamespaceName());
            }

            @Override
            public void visit(ObjectMetaBuilder metaBuilder) {
                if (!shouldConfigureNamespaceInMetadata()) {
                    return;
                }

                boolean forceModifyNamespace = Boolean.parseBoolean(getConfig(Config.FORCE));
                if (StringUtils.isBlank(metaBuilder.getNamespace()) || forceModifyNamespace) {
                    metaBuilder.withNamespace(getNamespaceName()).build();
                }
            }
        });

        // Removing namespace annotation from the namespace and project objects being generated.
        // to avoid unnecessary trouble while applying these resources.
        builder.accept(new TypedVisitor<NamespaceBuilder>() {
            @Override
            public void visit(NamespaceBuilder builder) {
                // Set status as empty
                NamespaceStatus status = builder.buildStatus();
                if (status != null && status.getPhase().equals("active")) {
                    builder.editOrNewStatus().endStatus().build();
                }
                // Set metadata.namespace as null
                builder.editOrNewMetadata().withNamespace(null).endMetadata();
            }
        });

        builder.accept(new TypedVisitor<ProjectBuilder>() {
            @Override
            public void visit(ProjectBuilder builder) {
                // Set status as empty
                ProjectStatus status = builder.buildStatus();
                if (status != null && status.getPhase().equals("active")) {
                    builder.editOrNewStatus().endStatus().build();
                }
                // Set metadata.namespace as null
                builder.editOrNewMetadata().withNamespace(null).endMetadata();
            }
        });
    }

    private void addNewNamespaceToBuilderIfProvided(PlatformMode platformMode, String newNamespaceToCreate, KubernetesListBuilder builder) {
        if (!KubernetesResourceUtil.checkForKind(builder, NAMESPACE_KINDS)) {
            String type = getConfig(Config.TYPE);
            if (StringUtils.isNotEmpty(newNamespaceToCreate)) {
                addNamespaceToBuilder(platformMode, newNamespaceToCreate, builder, type);
            }
        }
    }

    private void addNamespaceToBuilder(PlatformMode platformMode, String newNamespaceToCreate, KubernetesListBuilder builder, String type) {
        HasMetadata namespaceOrProject = getNamespaceOrProject(platformMode, type, newNamespaceToCreate);
        if (namespaceOrProject != null) {
            builder.addToItems(namespaceOrProject);
        }
    }

    private HasMetadata getNamespaceOrProject(PlatformMode platformMode, String type, String ns) {
        if ("project".equalsIgnoreCase(type) || NAMESPACE.equalsIgnoreCase(type)) {
            if (platformMode == PlatformMode.kubernetes) {
                log.info("Adding a default Namespace: %s", ns);
                return getContext().getHandlerHub().getNamespaceHandler().getNamespace(ns);
            } else {
                log.info("Adding a default Project %s", ns);
                return getContext().getHandlerHub().getProjectHandler().getProject(ns);
            }
        }
        return null;
    }

}
