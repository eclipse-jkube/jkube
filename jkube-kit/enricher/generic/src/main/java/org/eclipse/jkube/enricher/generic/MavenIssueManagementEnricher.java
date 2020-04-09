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
import io.fabric8.kubernetes.api.model.batch.JobBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.resource.JKubeAnnotations;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.maven.enricher.api.BaseEnricher;
import org.eclipse.jkube.maven.enricher.api.JKubeEnricherContext;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * This enricher will add the maven &gt;IssueManagement&lt; related metadata as annotations
 * the typical values will be like
 * <ul>
 * <li>system</li>
 * <li>url</li>
 * </ul>
 *
 * @author kameshs
 */
public class MavenIssueManagementEnricher extends BaseEnricher {
    static final String ENRICHER_NAME = "jkube-maven-issue-mgmt";

    public MavenIssueManagementEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, ENRICHER_NAME);
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

    private Map<String, String> getAnnotations() {
        Map<String, String> annotations = new HashMap<>();

        if (getContext() instanceof JKubeEnricherContext) {
            JKubeEnricherContext jkubeEnricherContext = (JKubeEnricherContext) getContext();
            JavaProject rootProject = jkubeEnricherContext.getProject();
            if (hasIssueManagement(rootProject)) {
                String system = rootProject.getIssueManagementSystem();
                String url = rootProject.getIssueManagementUrl();
                if (StringUtils.isNotEmpty(system) && StringUtils.isNotEmpty(url)) {
                    annotations.put(JKubeAnnotations.ISSUE_SYSTEM.value(), system);
                    annotations.put(JKubeAnnotations.ISSUE_TRACKER_URL.value(), url);
                }
            }
        }
        return annotations;
    }

    private boolean hasIssueManagement(JavaProject project) {
        return project.getIssueManagementSystem() != null ||
                project.getIssueManagementUrl() != null;
    }

}
