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
import org.junit.Before;
import org.junit.Test;

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
public class MavenProjectEnricherTest {
    private JKubeEnricherContext context;

    @Before
    public void setupExpectations() {
        context = mock(JKubeEnricherContext.class,RETURNS_DEEP_STUBS);
        when(context.getGav()).thenReturn(new GroupArtifactVersion("groupId", "artifactId", "version"));
    }

    @Test
    public void testGeneratedResources() {
        ProjectLabelEnricher projectEnricher = new ProjectLabelEnricher(context);

        KubernetesListBuilder builder = createListWithDeploymentConfig();
        projectEnricher.enrich(PlatformMode.kubernetes, builder);
        KubernetesList list = builder.build();

        Map<String, String> labels = list.getItems().get(0).getMetadata().getLabels();

        assertThat(labels).isNotNull();
        assertThat(labels.get("group")).isEqualTo("groupId");
        assertThat(labels.get("app")).isEqualTo("artifactId");
        assertThat(labels.get("version")).isEqualTo("version");
        assertThat(labels.get("project")).isNull();

        builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().build());
        projectEnricher.create(PlatformMode.kubernetes, builder);

        Deployment deployment = (Deployment)builder.buildFirstItem();
        Map<String, String> selectors = deployment.getSpec().getSelector().getMatchLabels();
        assertThat( selectors.get("group")).isEqualTo("groupId");
        assertThat(selectors.get("app")).isEqualTo("artifactId");
        assertThat(selectors.get("version")).isNull();
        assertThat(selectors.get("project")).isNull();
    }

    @Test
    public void testOldStyleGeneratedResources() {

        final Properties properties = new Properties();
        properties.setProperty("jkube.enricher.jkube-project-label.useProjectLabel", "true");
        when(context.getProperties()).thenReturn(properties);

        ProjectLabelEnricher projectEnricher = new ProjectLabelEnricher(context);

        KubernetesListBuilder builder = createListWithDeploymentConfig();
        projectEnricher.enrich(PlatformMode.kubernetes, builder);
        KubernetesList list = builder.build();

        Map<String, String> labels = list.getItems().get(0).getMetadata().getLabels();
        assertThat(labels).isNotNull();
        assertThat(labels.get("project")).isEqualTo("artifactId");
        assertThat(labels.get("version")).isEqualTo("version");
        assertThat(labels.get("group")).isEqualTo("groupId");
        assertThat(labels.get("app")).isNull();

        builder = new KubernetesListBuilder().withItems(new DeploymentConfigBuilder().build());
        projectEnricher.create(PlatformMode.kubernetes, builder);

        DeploymentConfig deploymentConfig = (DeploymentConfig)builder.buildFirstItem();
        Map<String, String> selectors = deploymentConfig.getSpec().getSelector();
        assertThat(selectors.get("group")).isEqualTo("groupId");
        assertThat( selectors.get("project")).isEqualTo("artifactId");
        assertThat(selectors.get("version")).isNull();
        assertThat(selectors.get("app")).isNull();
    }

    private KubernetesListBuilder createListWithDeploymentConfig() {
        return new KubernetesListBuilder().addToItems(new DeploymentConfigBuilder()
            .withNewMetadata().endMetadata()
            .withNewSpec().endSpec()
            .build());
    }

}
