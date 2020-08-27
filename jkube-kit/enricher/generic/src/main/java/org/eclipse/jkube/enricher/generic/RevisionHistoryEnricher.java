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
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

/**
 * This enricher adds the 'revisionHistoryLimit' property to deployment spec of RCs / RSs for Kubernetes/OpenShift
 * resource descriptors.
 *
 * <p> This property determines number of previous ReplicaController to retain in history in order to rollback previous one.
 */
public class RevisionHistoryEnricher extends BaseEnricher {

    private static final String DEFAULT_NAME = "jkube-revision-history";
    private static final String DEFAULT_NUMBER_OF_REVISIONS = "2";

    @AllArgsConstructor
    private enum Config implements Configs.Config {
        LIMIT("limit", DEFAULT_NUMBER_OF_REVISIONS);

        @Getter
        protected String key;
        @Getter
        protected String defaultValue;
    }

    public RevisionHistoryEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, DEFAULT_NAME);
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        final Integer maxRevisionHistories = Configs.asInt(getConfig(Config.LIMIT));

        log.info("Adding revision history limit to %s", maxRevisionHistories);

        if(platformMode == PlatformMode.kubernetes) {
            builder.accept(new TypedVisitor<io.fabric8.kubernetes.api.model.apps.DeploymentBuilder>() {
                @Override
                public void visit(io.fabric8.kubernetes.api.model.apps.DeploymentBuilder item) {
                    item.editOrNewSpec()
                            .withRevisionHistoryLimit(maxRevisionHistories)
                            .endSpec();
                }
            });
            builder.accept(new TypedVisitor<io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder>() {
                @Override
                public void visit(io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder item) {
                    item.editOrNewSpec()
                            .withRevisionHistoryLimit(maxRevisionHistories)
                            .endSpec();
                }
            });
        } else {
            builder.accept(new TypedVisitor<DeploymentConfigBuilder>() {
                @Override
                public void visit(DeploymentConfigBuilder item) {
                    item.editOrNewSpec()
                            .withRevisionHistoryLimit(maxRevisionHistories)
                            .endSpec();
                }
            });
        }
    }
}