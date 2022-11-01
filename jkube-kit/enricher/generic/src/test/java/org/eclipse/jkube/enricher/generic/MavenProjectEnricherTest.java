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

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test label generation.
 *
 * @author nicola
 */
class MavenProjectEnricherTest {
    private JKubeEnricherContext context;

    @BeforeEach
    void setupExpectations() {
        context = mock(JKubeEnricherContext.class,RETURNS_DEEP_STUBS);
        when(context.getGav()).thenReturn(new GroupArtifactVersion("groupId", "artifactId", "version"));
    }

    @Test
    void generatedResources() {
        ProjectLabelEnricher projectEnricher = new ProjectLabelEnricher(context);

        KubernetesListBuilder builder = createListWithDeploymentConfig();
        projectEnricher.enrich(PlatformMode.kubernetes, builder);
        KubernetesList list = builder.build();

        Map<String, String> labels = list.getItems().get(0).getMetadata().getLabels();
        assertThat(labels).isNotNull()
            .containsEntry("group", "groupId")
            .containsEntry("app", "artifactId")
            .containsEntry("version", "version")
            .doesNotContainKey("project");

        builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().build());
        projectEnricher.create(PlatformMode.kubernetes, builder);

        Deployment deployment = (Deployment) builder.buildFirstItem();
        Map<String, String> selectors = deployment.getSpec().getSelector().getMatchLabels();
        assertThat(selectors)
            .containsEntry("group", "groupId")
            .containsEntry("app", "artifactId")
            .doesNotContainKey("version")
            .doesNotContainKey("project");
    }

    @Test
    void oldStyleGeneratedResources() {

        final Properties properties = new Properties();
        properties.setProperty("jkube.enricher.jkube-project-label.useProjectLabel", "true");
        when(context.getProperties()).thenReturn(properties);

        ProjectLabelEnricher projectEnricher = new ProjectLabelEnricher(context);

        KubernetesListBuilder builder = createListWithDeploymentConfig();
        projectEnricher.enrich(PlatformMode.kubernetes, builder);
        KubernetesList list = builder.build();

        Map<String, String> labels = list.getItems().get(0).getMetadata().getLabels();
        assertThat(labels).isNotNull()
            .containsEntry("group", "groupId")
            .containsEntry("project", "artifactId")
            .containsEntry("version", "version")
            .doesNotContainKey("app");

        builder = new KubernetesListBuilder().withItems(new DeploymentConfigBuilder().build());
        projectEnricher.create(PlatformMode.kubernetes, builder);

        DeploymentConfig deploymentConfig = (DeploymentConfig)builder.buildFirstItem();
        Map<String, String> selectors = deploymentConfig.getSpec().getSelector();
        assertThat(selectors)
            .containsEntry("group", "groupId")
            .containsEntry("project", "artifactId")
            .doesNotContainKey("app")
            .doesNotContainKey("version");
    }

    private KubernetesListBuilder createListWithDeploymentConfig() {
        return new KubernetesListBuilder().addToItems(new DeploymentConfigBuilder()
            .withNewMetadata().endMetadata()
            .withNewSpec().endSpec()
            .build());
    }

}
