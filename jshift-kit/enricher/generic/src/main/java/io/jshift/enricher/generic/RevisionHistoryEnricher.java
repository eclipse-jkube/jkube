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
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.jshift.kit.common.Configs;
import io.jshift.kit.config.resource.PlatformMode;
import io.jshift.maven.enricher.api.BaseEnricher;
import io.jshift.maven.enricher.api.MavenEnricherContext;

/**
 * This enricher adds the 'revisionHistoryLimit' property to deployment spec of RCs / RSs for KuberNetes/OpenShift resource descriptors.
 * This property determines number of previous ReplicaControlller to retain in history in order to rollback previous one.
 */

public class RevisionHistoryEnricher extends BaseEnricher {

    public static final String DEFAULT_NAME = "jshift-revision-history";
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