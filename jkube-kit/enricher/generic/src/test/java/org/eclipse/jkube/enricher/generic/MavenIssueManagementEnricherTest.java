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
import org.eclipse.jkube.maven.enricher.api.JKubeEnricherContext;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

/**
 * @author kameshs
 */
public class MavenIssueManagementEnricherTest {

    @Mocked
    private JKubeEnricherContext context;

    @Test
    public void testMavenIssueManagementAll() {

        final JavaProject project = JavaProject.builder().build();

        project.setIssueManagementUrl("https://github.com/reactiverse/vertx-maven-plugin/issues/");
        project.setIssueManagementSystem("GitHub");
        // Setup mock behaviour
        new Expectations() {
            {
                {
                    context.getProject();
                    result = project;
                }
            }
        };

        MavenIssueManagementEnricher enricher = new MavenIssueManagementEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().withNewMetadata().withName("foo").endMetadata().build());

        enricher.create(PlatformMode.kubernetes, builder);

        Map<String, String> scmAnnotations = builder.buildFirstItem().getMetadata().getAnnotations();
        assertNotNull(scmAnnotations);
        Assert.assertEquals(2, scmAnnotations.size());
        assertEquals("GitHub",
                scmAnnotations.get(JKubeAnnotations.ISSUE_SYSTEM.value()));
        assertEquals("https://github.com/reactiverse/vertx-maven-plugin/issues/",
                scmAnnotations.get(JKubeAnnotations.ISSUE_TRACKER_URL.value()));
    }

    @Test
    public void testMavenIssueManagementOnlySystem() {

        final JavaProject project = JavaProject.builder().build();
        project.setIssueManagementSystem("GitHub");
        // Setup mock behaviour
        new Expectations() {
            {
                {
                    context.getProject();
                    result = project;
                }
            }
        };

        MavenIssueManagementEnricher enricher = new MavenIssueManagementEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().withNewMetadata().withName("foo").endMetadata().build());

        enricher.create(PlatformMode.kubernetes, builder);

        Map<String, String> scmAnnotations = builder.buildFirstItem().getMetadata().getAnnotations();
        assertNotNull(scmAnnotations);
        assertTrue(scmAnnotations.isEmpty());
    }

    @Test
    public void testMavenIssueManagementOnlyUrl() {

        final JavaProject project = JavaProject.builder().build();
        project.setIssueManagementUrl("https://github.com/fabric8org.eclipse.jkube-maven-plugin/issues/");
        // Setup mock behaviour
        new Expectations() {
            {
                {
                    context.getProject();
                    result = project;
                }
            }
        };

        MavenIssueManagementEnricher enricher = new MavenIssueManagementEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().withNewMetadata().withName("foo").endMetadata().build());

        enricher.create(PlatformMode.kubernetes, builder);

        Map<String, String> scmAnnotations = builder.buildFirstItem().getMetadata().getAnnotations();
        assertNotNull(scmAnnotations);
        assertTrue(scmAnnotations.isEmpty());
    }


    @Test
    public void testMavenNoIssueManagement() {

        final JavaProject project = JavaProject.builder().build();
        // Setup mock behaviour
        new Expectations() {
            {
                {
                    context.getProject();
                    result = project;
                }
            }
        };

        MavenIssueManagementEnricher enricher = new MavenIssueManagementEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().withNewMetadata().withName("foo").endMetadata().build());

        enricher.create(PlatformMode.kubernetes, builder);

        Map<String, String> scmAnnotations = builder.buildFirstItem().getMetadata().getAnnotations();
        assertNotNull(scmAnnotations);
        assertTrue(scmAnnotations.isEmpty());
    }


}
