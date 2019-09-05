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
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.batch.JobBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.jshift.kit.common.util.GitUtil;
import io.jshift.kit.config.resource.JshiftAnnotations;
import io.jshift.kit.config.resource.PlatformMode;
import io.jshift.maven.enricher.api.BaseEnricher;
import io.jshift.maven.enricher.api.MavenEnricherContext;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Enricher for adding build metadata:
 *
 * <ul>
 *   <li>Git Branch</li>
 *   <li>Git Commit ID</li>
 * </ul>
 *
 * @since 01/05/16
 */
public class GitEnricher extends BaseEnricher {

    private String GIT_REMOTE = "jshift.remoteName";

    public GitEnricher(MavenEnricherContext buildContext) {
        super(buildContext, "jshift-git");
    }

    private Map<String, String> getAnnotations() {
        final Map<String, String> annotations = new HashMap<>();
        if (GitUtil.findGitFolder(getContext().getProjectDirectory()) != null) {
            Repository repository = null;
            try {
                // Git annotations (if git is used as SCM)
                repository = GitUtil.getGitRepository(getContext().getProjectDirectory());
                if (repository != null) {
                    String branch = repository.getBranch();
                    if (branch != null) {
                        annotations.put(JshiftAnnotations.GIT_BRANCH.value(), branch);
                    }
                    String id = GitUtil.getGitCommitId(repository);
                    if (id != null) {
                        annotations.put(JshiftAnnotations.GIT_COMMIT.value(), id);
                    }

                    String gitRemote = getContext().getConfiguration().getProperties().getProperty(GIT_REMOTE);
                    gitRemote = gitRemote == null? "origin" : gitRemote;
                    String gitRemoteUrl = repository.getConfig().getString("remote", gitRemote, "url");
                    if (gitRemoteUrl != null) {
                        annotations.put(JshiftAnnotations.GIT_URL.value(), gitRemoteUrl);
                    } else {
                        log.warn("Could not detect any git remote");
                    }
                }
                return annotations;
            } catch (IOException | GitAPIException e) {
                log.error("Cannot extract Git information for adding to annotations: " + e, e);
                return null;
            } finally {
                if (repository != null) {
                    try {
                        repository.close();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }

        return annotations;
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        builder.accept(new TypedVisitor<ServiceBuilder>() {
            @Override
            public void visit(ServiceBuilder serviceBuilder) {
                serviceBuilder.editMetadata().addToAnnotations(getAnnotations()).endMetadata();
            }
        });

        builder.accept(new TypedVisitor<DeploymentBuilder>() {
            @Override
            public void visit(DeploymentBuilder builder) {
                builder.editMetadata().addToAnnotations(getAnnotations()).endMetadata();
            }
        });

        builder.accept(new TypedVisitor<DeploymentConfigBuilder>() {
            @Override
            public void visit(DeploymentConfigBuilder builder) {
                builder.editMetadata().addToAnnotations(getAnnotations()).endMetadata();
            }
        });

        builder.accept(new TypedVisitor<ReplicaSetBuilder>() {
            @Override
            public void visit(ReplicaSetBuilder builder) {
                builder.editMetadata().addToAnnotations(getAnnotations()).endMetadata();
            }
        });

        builder.accept(new TypedVisitor<ReplicationControllerBuilder>() {
            @Override
            public void visit(ReplicationControllerBuilder builder) {
                builder.editMetadata().addToAnnotations(getAnnotations()).endMetadata();
            }
        });

        builder.accept(new TypedVisitor<DaemonSetBuilder>() {
            @Override
            public void visit(DaemonSetBuilder builder) {
                builder.editMetadata().addToAnnotations(getAnnotations()).endMetadata();
            }
        });

        builder.accept(new TypedVisitor<StatefulSetBuilder>() {
            @Override
            public void visit(StatefulSetBuilder builder) {
                builder.editMetadata().addToAnnotations(getAnnotations()).endMetadata();
            }
        });

        builder.accept(new TypedVisitor<JobBuilder>() {
            @Override
            public void visit(JobBuilder builder) {
                builder.editMetadata().addToAnnotations(getAnnotations()).endMetadata();
            }
        });
    }
}

