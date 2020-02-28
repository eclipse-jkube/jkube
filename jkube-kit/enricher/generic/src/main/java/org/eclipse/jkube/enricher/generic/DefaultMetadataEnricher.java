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

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.maven.enricher.api.BaseEnricher;
import org.eclipse.jkube.maven.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.maven.enricher.api.visitor.MetadataVisitor;
import org.eclipse.jkube.maven.enricher.api.visitor.SelectorVisitor;

public class DefaultMetadataEnricher extends BaseEnricher {

    // List of visitors used to create with labels
    private MetadataVisitor<?>[] metaDataVisitors = null;
    private SelectorVisitor<?>[] selectorVisitorCreators = null;

    // context used by enrichers
    private final ProcessorConfig defaultEnricherConfig;

    private final ResourceConfig resourceConfig;

    public DefaultMetadataEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-metadata");

        this.defaultEnricherConfig = buildContext.getConfiguration().getProcessorConfig().orElse(ProcessorConfig.EMPTY);
        this.resourceConfig = buildContext.getConfiguration().getResource().orElse(null);
    }

    private void init() {

        this.metaDataVisitors = new MetadataVisitor[] {
                new MetadataVisitor.DeploymentBuilderVisitor(resourceConfig),
                new MetadataVisitor.DeploymentConfigBuilderVisitor(resourceConfig),
                new MetadataVisitor.ReplicaSet(resourceConfig),
                new MetadataVisitor.ReplicationControllerBuilderVisitor(resourceConfig),
                new MetadataVisitor.ServiceBuilderVisitor(resourceConfig),
                new MetadataVisitor.PodTemplateSpecBuilderVisitor(resourceConfig),
                new MetadataVisitor.DaemonSetBuilderVisitor(resourceConfig),
                new MetadataVisitor.StatefulSetBuilderVisitor(resourceConfig),
                new MetadataVisitor.JobBuilderVisitor(resourceConfig),
                new MetadataVisitor.ImageStreamBuilderVisitor(resourceConfig),
                new MetadataVisitor.BuildConfigBuilderVisitor(resourceConfig),
                new MetadataVisitor.BuildBuilderVisitor(resourceConfig),
                new MetadataVisitor.IngressBuilderVisitor(resourceConfig)
        };
    }

    @Override
    public void enrich(PlatformMode platformMode, KubernetesListBuilder builder) {
        init();
        // Enrich labels
        enrichLabels(defaultEnricherConfig, builder);
    }

    /**
     * Enrich the given list with labels.
     *
     * @param builder the build to create with labels
     */
    private void enrichLabels(ProcessorConfig config, KubernetesListBuilder builder) {
        visit(config, builder, metaDataVisitors);
    }

    private void visit(ProcessorConfig config, KubernetesListBuilder builder, MetadataVisitor<?>[] visitors) {
        MetadataVisitor.setProcessorConfig(config);
        try {
            for (MetadataVisitor<?> visitor : visitors) {
                builder.accept(visitor);
            }
        } finally {
            MetadataVisitor.clearProcessorConfig();
        }
    }

}