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
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerFluent;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentFluent;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetFluent;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.JkubeProjectUtil;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.maven.enricher.api.BaseEnricher;
import org.eclipse.jkube.maven.enricher.api.JkubeEnricherContext;
import org.apache.commons.lang3.StringUtils;

/**
 * Enricher for adding a "name" to the metadata to various objects we create.
 * The name is only added if not already set.
 *
 * The name is added to the following objects:
 *
 * @author roland
 * @since 25/05/16
 */
public class NameEnricher extends BaseEnricher {

    public NameEnricher(JkubeEnricherContext buildContext) {
        super(buildContext, "jkube-name");
    }

    private enum Config implements Configs.Key {
        name;
        public String def() { return d; } protected String d;
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        final String defaultName = getConfig(Config.name, JkubeProjectUtil.createDefaultResourceName(getContext().getGav().getSanitizedArtifactId()));

        builder.accept(new TypedVisitor<HasMetadata>() {
            @Override
            public void visit(HasMetadata resource) {
                ObjectMeta metadata = getOrCreateMetadata(resource);
                if (StringUtils.isBlank(metadata.getName())) {
                    metadata.setName(defaultName);
                }
            }
        });

        // TODO not sure why this is required for Deployment?
        builder.accept(new TypedVisitor<DeploymentBuilder>() {
            @Override
            public void visit(DeploymentBuilder resource) {
                DeploymentFluent.MetadataNested<DeploymentBuilder> metadata = resource.editMetadata();
                if (metadata == null) {
                    resource.withNewMetadata().withName(defaultName).endMetadata();
                } else {
                    if (StringUtils.isBlank(metadata.getName())) {
                        metadata.withName(defaultName).endMetadata();
                    }
                }
            }
        });
        builder.accept(new TypedVisitor<ReplicationControllerBuilder>() {
            @Override
            public void visit(ReplicationControllerBuilder resource) {
                ReplicationControllerFluent.MetadataNested<ReplicationControllerBuilder> metadata = resource.editMetadata();
                if (metadata == null) {
                    resource.withNewMetadata().withName(defaultName).endMetadata();
                } else {
                    if (StringUtils.isBlank(metadata.getName())) {
                        metadata.withName(defaultName).endMetadata();
                    }
                }
            }
        });
        builder.accept(new TypedVisitor<ReplicaSetBuilder>() {
            @Override
            public void visit(ReplicaSetBuilder resource) {
                ReplicaSetFluent.MetadataNested<ReplicaSetBuilder> metadata = resource.editMetadata();
                if (metadata == null) {
                    resource.withNewMetadata().withName(defaultName).endMetadata();
                } else {
                    if (StringUtils.isBlank(metadata.getName())) {
                        metadata.withName(defaultName).endMetadata();
                    }
                }
            }
        });
    }

    private ObjectMeta getOrCreateMetadata(HasMetadata resource) {
        ObjectMeta metadata = resource.getMetadata();
        if (metadata == null) {
            metadata = new ObjectMeta();
            resource.setMetadata(metadata);
        }
        return metadata;
    }
}
