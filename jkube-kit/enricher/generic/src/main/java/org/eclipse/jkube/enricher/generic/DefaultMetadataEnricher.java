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

import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.visitor.MetadataVisitor;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;

public class DefaultMetadataEnricher extends BaseEnricher {

    private final ResourceConfig resourceConfig;

    public DefaultMetadataEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-metadata");
        this.resourceConfig = buildContext.getConfiguration().getResource();
    }

    private MetadataVisitor<?>[]  visitors() {
        return new MetadataVisitor[] {
            MetadataVisitor.deployment(resourceConfig),
            MetadataVisitor.extensionsDeployment(resourceConfig),
            MetadataVisitor.deploymentConfig(resourceConfig),
            MetadataVisitor.replicaSet(resourceConfig),
            MetadataVisitor.replicationController(resourceConfig),
            MetadataVisitor.service(resourceConfig),
            MetadataVisitor.podTemplateSpec(resourceConfig),
            MetadataVisitor.daemonSet(resourceConfig),
            MetadataVisitor.statefulSet(resourceConfig),
            MetadataVisitor.job(resourceConfig),
            MetadataVisitor.imageStream(resourceConfig),
            MetadataVisitor.buildConfig(resourceConfig),
            MetadataVisitor.build(resourceConfig),
            MetadataVisitor.extensionsIngress(resourceConfig),
            MetadataVisitor.ingressV1beta1(resourceConfig),
            MetadataVisitor.ingressV1(resourceConfig),
            MetadataVisitor.serviceAccount(resourceConfig),
            MetadataVisitor.route(resourceConfig),
            // Apply last: Other MetadataVisitor might initiate the metadata field for the item
            MetadataVisitor.metadata(resourceConfig)
        };
    }

    @Override
    public void enrich(PlatformMode platformMode, KubernetesListBuilder builder) {
        enrichLabelsAndAnnotations(builder);
    }

    /**
     * Enrich the given list with labels.
     *
     * @param builder the build to create with labels and annotations
     */
    private void enrichLabelsAndAnnotations(KubernetesListBuilder builder) {
        visit(builder, visitors());
    }

    private void visit(KubernetesListBuilder builder, MetadataVisitor<?>[] visitors) {
        for (MetadataVisitor<?> visitor : visitors) {
            builder.accept(visitor);
        }
    }

}