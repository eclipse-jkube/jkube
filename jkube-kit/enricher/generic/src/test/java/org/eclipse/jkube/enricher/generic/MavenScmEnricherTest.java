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
import org.eclipse.jkube.kit.common.JKubeProject;
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

/**
 * @author kameshs
 */
public class MavenScmEnricherTest {

    @Mocked
    private JKubeEnricherContext context;

    @Test
    public void testMavenScmAll() {

        final JKubeProject project = new JKubeProject();
        project.setScmUrl("git://github.com/jkubeio/kubernetes-maven-plugin.git");
        project.setScmTag("HEAD");
        // Setup mock behaviour
        new Expectations() {
            {
                {
                    context.getProject();
                    result = project;
                }
            }
        };

        MavenScmEnricher mavenScmEnricher = new MavenScmEnricher(context);

        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().withNewMetadata().withName("foo").endMetadata().build());

        mavenScmEnricher.create(PlatformMode.kubernetes, builder);

        Map<String, String> scmAnnotations = builder.buildFirstItem().getMetadata().getAnnotations();
        assertNotNull(scmAnnotations);

        Assert.assertEquals(2, scmAnnotations.size());
        assertEquals("HEAD",
                scmAnnotations.get(JKubeAnnotations.SCM_TAG.value()));
        assertEquals("git://github.com/jkubeio/kubernetes-maven-plugin.git",
                scmAnnotations.get(JKubeAnnotations.SCM_URL.value()));

    }

    @Test
    public void testMavenScmOnlyConnection() {

        final JKubeProject project = new JKubeProject();
        project.setScmUrl("scm:git:git://github.com/jkubeio/kubernetes-maven-plugin.git");
        // Setup mock behaviour
        new Expectations() {
            {
                {
                    context.getProject();
                    result = project;
                }
            }
        };

        MavenScmEnricher mavenScmEnricher = new MavenScmEnricher(context);

        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().withNewMetadata().withName("foo").endMetadata().build());

        mavenScmEnricher.create(PlatformMode.kubernetes, builder);

        Map<String, String> scmAnnotations = builder.buildFirstItem().getMetadata().getAnnotations();
        assertNotNull(scmAnnotations);

        Assert.assertEquals(1, scmAnnotations.size());
        Assert.assertFalse(scmAnnotations.containsKey(JKubeAnnotations.SCM_TAG.value()));
        Assert.assertEquals("scm:git:git://github.com/jkubeio/kubernetes-maven-plugin.git", scmAnnotations.get(JKubeAnnotations.SCM_URL.value()));

    }

    @Test
    public void testMavenScmOnlyDevConnection() {

        final JKubeProject project = new JKubeProject();
        project.setScmUrl("git://github.com/jkubeio/kubernetes-maven-plugin.git");
        // Setup mock behaviour
        new Expectations() {
            {
                {
                    context.getProject();
                    result = project;
                }
            }
        };

        MavenScmEnricher mavenScmEnricher = new MavenScmEnricher(context);

        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().withNewMetadata().withName("foo").endMetadata().build());

        mavenScmEnricher.create(PlatformMode.kubernetes, builder);

        Map<String, String> scmAnnotations = builder.buildFirstItem().getMetadata().getAnnotations();
        assertNotNull(scmAnnotations);

        Assert.assertEquals(1, scmAnnotations.size());
        assertEquals("git://github.com/jkubeio/kubernetes-maven-plugin.git",
                scmAnnotations.get(JKubeAnnotations.SCM_URL.value()));
        Assert.assertFalse(scmAnnotations.containsKey(JKubeAnnotations.SCM_TAG.value()));
    }

    @Test
    public void testMavenScmOnlyUrl() {

        final JKubeProject project = new JKubeProject();
        project.setScmUrl("scm:git:git://github.com/jkubeio/kubernetes-maven-plugin.git");
        // Setup mock behaviour
        new Expectations() {
            {
                {
                    context.getProject();
                    result = project;
                }
            }
        };

        MavenScmEnricher mavenScmEnricher = new MavenScmEnricher(context);

        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().withNewMetadata().withName("foo").endMetadata().build());

        mavenScmEnricher.create(PlatformMode.kubernetes, builder);

        Map<String, String> scmAnnotations = builder.buildFirstItem().getMetadata().getAnnotations();
        assertNotNull(scmAnnotations);

        Assert.assertEquals(1, scmAnnotations.size());
        Assert.assertFalse(scmAnnotations.containsKey(JKubeAnnotations.SCM_TAG.value()));
        Assert.assertEquals("scm:git:git://github.com/jkubeio/kubernetes-maven-plugin.git", scmAnnotations.get(JKubeAnnotations.SCM_URL.value()));
    }

    @Test
    public void testMavenNoScm() {

        final JKubeProject project = new JKubeProject();
        // Setup mock behaviour
        new Expectations() {
            {
                {
                    context.getProject();
                    result = project;
                }
            }
        };

        MavenScmEnricher mavenScmEnricher = new MavenScmEnricher(context);

        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().withNewMetadata().withName("foo").endMetadata().build());

        mavenScmEnricher.create(PlatformMode.kubernetes, builder);

        Map<String, String> scmAnnotations = builder.buildFirstItem().getMetadata().getAnnotations();
        assertNotNull(scmAnnotations);

    }

}
