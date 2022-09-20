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
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.resource.JKubeAnnotations;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author kameshs
 */
public class MavenIssueManagementEnricherTest {
    private JKubeEnricherContext context;
    @Before
    public void setUp() throws Exception {
        context = mock(JKubeEnricherContext.class,RETURNS_DEEP_STUBS);
    }
    @Test
    public void testMavenIssueManagementAll() {

        final JavaProject project = JavaProject.builder().build();

        project.setIssueManagementUrl("https://github.com/reactiverse/vertx-maven-plugin/issues/");
        project.setIssueManagementSystem("GitHub");
        // Setup mock behaviour
        when(context.getProject()).thenReturn(project);
        MavenIssueManagementEnricher enricher = new MavenIssueManagementEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().withNewMetadata().withName("foo").endMetadata().build());

        enricher.create(PlatformMode.kubernetes, builder);

        Map<String, String> scmAnnotations = builder.buildFirstItem().getMetadata().getAnnotations();
        assertThat(scmAnnotations).isNotNull();
        Assert.assertEquals(2, scmAnnotations.size());
        assertThat(scmAnnotations).containsEntry(JKubeAnnotations.ISSUE_SYSTEM.value(), "GitHub")
                .containsEntry(JKubeAnnotations.ISSUE_TRACKER_URL.value(), "https://github.com/reactiverse/vertx-maven-plugin/issues/");
    }
    @Test
    public void testMavenIssueManagementOnlySystem() {

        final JavaProject project = JavaProject.builder().build();
        project.setIssueManagementSystem("GitHub");
        // Setup mock behaviour
        when(context.getProject()).thenReturn(project);
        MavenIssueManagementEnricher enricher = new MavenIssueManagementEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().withNewMetadata().withName("foo").endMetadata().build());

        enricher.create(PlatformMode.kubernetes, builder);

        Map<String, String> scmAnnotations = builder.buildFirstItem().getMetadata().getAnnotations();
        assertThat(scmAnnotations).isNotNull().isEmpty();
    }

    @Test
    public void testMavenIssueManagementOnlyUrl() {

        final JavaProject project = JavaProject.builder().build();
        project.setIssueManagementUrl("https://github.com/fabric8org.eclipse.jkube-maven-plugin/issues/");
        // Setup mock behaviour
        when(context.getProject()).thenReturn(project);
        MavenIssueManagementEnricher enricher = new MavenIssueManagementEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().withNewMetadata().withName("foo").endMetadata().build());

        enricher.create(PlatformMode.kubernetes, builder);

        Map<String, String> scmAnnotations = builder.buildFirstItem().getMetadata().getAnnotations();
        assertThat(scmAnnotations).isNotNull().isEmpty();
    }


    @Test
    public void testMavenNoIssueManagement() {

        final JavaProject project = JavaProject.builder().build();
        // Setup mock behaviour
        when(context.getProject()).thenReturn(project);
        MavenIssueManagementEnricher enricher = new MavenIssueManagementEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().withNewMetadata().withName("foo").endMetadata().build());

        enricher.create(PlatformMode.kubernetes, builder);

        Map<String, String> scmAnnotations = builder.buildFirstItem().getMetadata().getAnnotations();
        assertThat(scmAnnotations).isNotNull();
        assertThat(scmAnnotations).isEmpty();
    }


}
