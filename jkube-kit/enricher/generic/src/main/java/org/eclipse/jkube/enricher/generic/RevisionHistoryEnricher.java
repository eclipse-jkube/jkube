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
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.maven.enricher.api.BaseEnricher;
import org.eclipse.jkube.maven.enricher.api.MavenEnricherContext;

/**
 * This enricher adds the 'revisionHistoryLimit' property to deployment spec of RCs / RSs for KuberNetes/OpenShift resource descriptors.
 * This property determines number of previous ReplicaControlller to retain in history in order to rollback previous one.
 */

public class RevisionHistoryEnricher extends BaseEnricher {

    public static final String DEFAULT_NAME = "jkube-revision-history";
    private static final String DEFAULT_NUMBER_OF_REVISIONS = "2";

    // config keys
    enum Config implements Configs.Key {
        limit {{ d = DEFAULT_NUMBER_OF_REVISIONS; }};

        protected String d;
        public String def() { return d; }
    }

    public RevisionHistoryEnricher(MavenEnricherContext buildContext) {
        super(buildContext, DEFAULT_NAME);
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        final Integer maxRevisionHistories = Configs.asInt(getConfig(Config.limit));

        log.info("Adding revision history limit to %s", maxRevisionHistories);

        if(platformMode == PlatformMode.kubernetes) {
            builder.accept(new TypedVisitor<DeploymentBuilder>() {
                @Override
                public void visit(DeploymentBuilder item) {
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