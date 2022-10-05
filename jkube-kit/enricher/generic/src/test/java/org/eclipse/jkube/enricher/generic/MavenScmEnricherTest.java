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
public class MavenScmEnricherTest {

    private JKubeEnricherContext context;
    @Before
    public void setup() {
        context = mock(JKubeEnricherContext.class,RETURNS_DEEP_STUBS);
    }
    @Test
    public void testMavenScmAll() {

        final JavaProject project = JavaProject.builder()
            .scmUrl("git://github.com/jkubeio/kubernetes-maven-plugin.git")
            .scmTag("HEAD")
            .build();
        // Setup mock behaviour
        when(context.getProject()).thenReturn(project);
        MavenScmEnricher mavenScmEnricher = new MavenScmEnricher(context);

        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().withNewMetadata().withName("foo").endMetadata().build());

        mavenScmEnricher.create(PlatformMode.kubernetes, builder);

        Map<String, String> scmAnnotations = builder.buildFirstItem().getMetadata().getAnnotations();
        assertThat(scmAnnotations)
                .isNotNull()
                .hasSize(2)
                .containsEntry(JKubeAnnotations.SCM_TAG.value(),"HEAD").containsEntry(JKubeAnnotations.SCM_URL.value(),"git://github.com/jkubeio/kubernetes-maven-plugin.git");

    }

    @Test
    public void testMavenScmOnlyConnection() {

        final JavaProject project = JavaProject.builder()
            .scmUrl("scm:git:git://github.com/jkubeio/kubernetes-maven-plugin.git")
            .build();
        // Setup mock behaviour
        when(context.getProject()).thenReturn(project);
        MavenScmEnricher mavenScmEnricher = new MavenScmEnricher(context);

        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().withNewMetadata().withName("foo").endMetadata().build());

        mavenScmEnricher.create(PlatformMode.kubernetes, builder);

        Map<String, String> scmAnnotations = builder.buildFirstItem().getMetadata().getAnnotations();

        assertThat(scmAnnotations).isNotNull()
                .hasSize(1)
                .containsEntry(JKubeAnnotations.SCM_URL.value(),"scm:git:git://github.com/jkubeio/kubernetes-maven-plugin.git")
                .doesNotContainKey(JKubeAnnotations.SCM_TAG.value());

    }

    @Test
    public void testMavenScmOnlyDevConnection() {

        final JavaProject project = JavaProject.builder()
            .scmUrl("git://github.com/jkubeio/kubernetes-maven-plugin.git")
            .build();
        // Setup mock behaviour
        when(context.getProject()).thenReturn(project);
        MavenScmEnricher mavenScmEnricher = new MavenScmEnricher(context);

        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().withNewMetadata().withName("foo").endMetadata().build());

        mavenScmEnricher.create(PlatformMode.kubernetes, builder);

        Map<String, String> scmAnnotations = builder.buildFirstItem().getMetadata().getAnnotations();
        assertThat(scmAnnotations).isNotNull().hasSize(1)
                .containsEntry(JKubeAnnotations.SCM_URL.value(),"git://github.com/jkubeio/kubernetes-maven-plugin.git")
                .doesNotContainKey(JKubeAnnotations.SCM_TAG.value());
    }

    @Test
    public void testMavenScmOnlyUrl() {

        final JavaProject project = JavaProject.builder()
            .scmUrl("scm:git:git://github.com/jkubeio/kubernetes-maven-plugin.git")
            .build();
        // Setup mock behaviour
        when(context.getProject()).thenReturn(project);
        MavenScmEnricher mavenScmEnricher = new MavenScmEnricher(context);

        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().withNewMetadata().withName("foo").endMetadata().build());

        mavenScmEnricher.create(PlatformMode.kubernetes, builder);

        Map<String, String> scmAnnotations = builder.buildFirstItem().getMetadata().getAnnotations();

        assertThat(scmAnnotations).isNotNull().hasSize(1)
                .containsEntry(JKubeAnnotations.SCM_URL.value(),"scm:git:git://github.com/jkubeio/kubernetes-maven-plugin.git")
                .doesNotContainKey(JKubeAnnotations.SCM_TAG.value());
    }

    @Test
    public void testMavenNoScm() {
        final JavaProject project = JavaProject.builder().build();
        // Setup mock behaviour
        when(context.getProject()).thenReturn(project);
        MavenScmEnricher mavenScmEnricher = new MavenScmEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().withNewMetadata().withName("foo").endMetadata().build());
        mavenScmEnricher.create(PlatformMode.kubernetes, builder);
        Map<String, String> scmAnnotations = builder.buildFirstItem().getMetadata().getAnnotations();
        assertThat(scmAnnotations).isNotNull();

    }

}
