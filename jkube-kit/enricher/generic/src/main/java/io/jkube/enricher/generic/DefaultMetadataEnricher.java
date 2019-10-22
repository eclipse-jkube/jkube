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

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.jkube.kit.config.resource.PlatformMode;
import io.jkube.kit.config.resource.ProcessorConfig;
import io.jkube.kit.config.resource.ResourceConfig;
import io.jkube.maven.enricher.api.BaseEnricher;
import io.jkube.maven.enricher.api.MavenEnricherContext;
import io.jkube.maven.enricher.api.visitor.MetadataVisitor;
import io.jkube.maven.enricher.api.visitor.SelectorVisitor;

public class DefaultMetadataEnricher extends BaseEnricher {

    // List of visitors used to create with labels
    private MetadataVisitor<?>[] metaDataVisitors = null;
    private SelectorVisitor<?>[] selectorVisitorCreators = null;

    // context used by enrichers
    private final ProcessorConfig defaultEnricherConfig;

    private final ResourceConfig resourceConfig;

    public DefaultMetadataEnricher(MavenEnricherContext buildContext) {
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