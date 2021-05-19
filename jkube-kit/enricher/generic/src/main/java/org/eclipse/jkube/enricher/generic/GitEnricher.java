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
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import org.eclipse.jkube.kit.common.util.GitUtil;
import org.eclipse.jkube.kit.config.resource.JKubeAnnotations;
import org.eclipse.jkube.kit.config.resource.OpenShiftAnnotations;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
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
 */
public class GitEnricher extends BaseEnricher {

    private String GIT_REMOTE = "jkube.remoteName";

    public GitEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-git");
    }

    private Map<String, String> getAnnotations(PlatformMode platformMode) {
        final Map<String, String> annotations = new HashMap<>();
        if (GitUtil.findGitFolder(getContext().getProjectDirectory()) != null) {
            try (Repository repository = GitUtil.getGitRepository(getContext().getProjectDirectory())) {
                // Git annotations (if git is used as SCM)
                if (repository != null) {
                    String gitRemoteUrl =  getGitRemoteUrl(repository);
                    if (gitRemoteUrl == null) {
                        log.warn("Could not detect any git remote");
                    }

                    annotations.putAll(getAnnotations(platformMode, gitRemoteUrl, repository.getBranch(), GitUtil.getGitCommitId(repository)));
                }
                return annotations;
            } catch (IOException | GitAPIException e) {
                log.error("Cannot extract Git information for adding to annotations: " + e, e);
                return null;
            }
        }

        return annotations;
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        builder.accept(new TypedVisitor<ServiceBuilder>() {
            @Override
            public void visit(ServiceBuilder serviceBuilder) {
                serviceBuilder.editMetadata().addToAnnotations(getAnnotations(platformMode)).endMetadata();
            }
        });

        builder.accept(new TypedVisitor<DeploymentBuilder>() {
            @Override
            public void visit(DeploymentBuilder builder) {
                builder.editMetadata().addToAnnotations(getAnnotations(platformMode)).endMetadata();
            }
        });

        builder.accept(new TypedVisitor<DeploymentConfigBuilder>() {
            @Override
            public void visit(DeploymentConfigBuilder builder) {
                builder.editMetadata().addToAnnotations(getAnnotations(platformMode)).endMetadata();
            }
        });

        builder.accept(new TypedVisitor<ReplicaSetBuilder>() {
            @Override
            public void visit(ReplicaSetBuilder builder) {
                builder.editMetadata().addToAnnotations(getAnnotations(platformMode)).endMetadata();
            }
        });

        builder.accept(new TypedVisitor<ReplicationControllerBuilder>() {
            @Override
            public void visit(ReplicationControllerBuilder builder) {
                builder.editMetadata().addToAnnotations(getAnnotations(platformMode)).endMetadata();
            }
        });

        builder.accept(new TypedVisitor<DaemonSetBuilder>() {
            @Override
            public void visit(DaemonSetBuilder builder) {
                builder.editMetadata().addToAnnotations(getAnnotations(platformMode)).endMetadata();
            }
        });

        builder.accept(new TypedVisitor<StatefulSetBuilder>() {
            @Override
            public void visit(StatefulSetBuilder builder) {
                builder.editMetadata().addToAnnotations(getAnnotations(platformMode)).endMetadata();
            }
        });

        builder.accept(new TypedVisitor<JobBuilder>() {
            @Override
            public void visit(JobBuilder builder) {
                builder.editMetadata().addToAnnotations(getAnnotations(platformMode)).endMetadata();
            }
        });
    }

    protected static Map<String, String> getAnnotations(PlatformMode platformMode, String gitRemoteUrl, String branch, String commitId) {
        Map<String, String> annotationsToBeAdded = new HashMap<>();
        annotationsToBeAdded.putAll(addAnnotation(JKubeAnnotations.GIT_BRANCH.value(), branch));
        annotationsToBeAdded.putAll(addAnnotation(JKubeAnnotations.GIT_COMMIT.value(), commitId));
        annotationsToBeAdded.putAll(addAnnotation(JKubeAnnotations.GIT_URL.value(), gitRemoteUrl));
        if (platformMode.equals(PlatformMode.openshift)) {
            annotationsToBeAdded.putAll(addAnnotation(OpenShiftAnnotations.VCS_URI.value(), gitRemoteUrl));
            annotationsToBeAdded.putAll(addAnnotation(OpenShiftAnnotations.VCS_REF.value(), branch));
        }
        return annotationsToBeAdded;
    }

    private static Map<String, String> addAnnotation(String key, String value) {
        Map<String, String> newAnnotation = new HashMap<>();
        if (value != null && key != null) {
            newAnnotation.put(key, value);
        }
        return newAnnotation;
    }

    private String getGitRemoteUrl(Repository repository) {
        String gitRemote = getContext().getProperty(GIT_REMOTE);
        gitRemote = gitRemote == null ? "origin" : gitRemote;
        return repository.getConfig().getString("remote", gitRemote, "url");
    }
}

