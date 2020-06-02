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
package org.eclipse.jkube.enricher.generic.openshift;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.openshift.api.model.DeploymentConfig;
import mockit.Mocked;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeploymentConfigEnricherTest {
    @Mocked
    private JKubeEnricherContext context;

    @Test
    public void testConversionFromAppsV1Deployment() {
        // Given
        DeploymentConfigEnricher deploymentConfigEnricher = new DeploymentConfigEnricher(context);
        io.fabric8.kubernetes.api.model.apps.Deployment appsV1Deployment = new io.fabric8.kubernetes.api.model.apps.DeploymentBuilder()
                .withNewMetadata().withName("test-app").addToLabels("app", "test-app").endMetadata()
                .withNewSpec()
                .withReplicas(3)
                .withRevisionHistoryLimit(2)
                .withNewSelector().addToMatchLabels("app", "test-app").endSelector()
                .withNewTemplate()
                .withNewMetadata().addToLabels("app", "test-app").endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName("test-container")
                .withImage("test-image:1.0.0")
                .addNewPort()
                .withContainerPort(80)
                .endPort()
                .endContainer()
                .endSpec()
                .endTemplate()
                .withNewStrategy()
                .withType("Rolling")
                .withNewRollingUpdate().withMaxSurge(new IntOrString(5)).endRollingUpdate()
                .endStrategy()
                .endSpec()
                .build();
        KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder().addToItems(appsV1Deployment);

        // When
        deploymentConfigEnricher.create(PlatformMode.openshift, kubernetesListBuilder);

        // Then
        assertEquals(1, kubernetesListBuilder.getItems().size());
        HasMetadata result = kubernetesListBuilder.buildFirstItem();
        assertTrue(result instanceof DeploymentConfig);
        assertDeploymentConfig((DeploymentConfig) result, "Rolling");
    }

    @Test
    public void testConversionFromAppsV1DeploymentWithRecreateStrategy() {
        // Given
        DeploymentConfigEnricher deploymentConfigEnricher = new DeploymentConfigEnricher(context);
        io.fabric8.kubernetes.api.model.apps.Deployment appsV1Deployment = new io.fabric8.kubernetes.api.model.apps.DeploymentBuilder()
                .withNewMetadata().withName("test-app").addToLabels("app", "test-app").endMetadata()
                .withNewSpec()
                .withReplicas(3)
                .withRevisionHistoryLimit(2)
                .withNewSelector().addToMatchLabels("app", "test-app").endSelector()
                .withNewTemplate()
                .withNewMetadata().addToLabels("app", "test-app").endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName("test-container")
                .withImage("test-image:1.0.0")
                .addNewPort()
                .withContainerPort(80)
                .endPort()
                .endContainer()
                .endSpec()
                .endTemplate()
                .withNewStrategy()
                .withType("Recreate")
                .endStrategy()
                .endSpec()
                .build();
        KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder().addToItems(appsV1Deployment);

        // When
        deploymentConfigEnricher.create(PlatformMode.openshift, kubernetesListBuilder);

        // Then
        assertEquals(1, kubernetesListBuilder.getItems().size());
        HasMetadata result = kubernetesListBuilder.buildFirstItem();
        assertTrue(result instanceof DeploymentConfig);
        assertDeploymentConfig((DeploymentConfig) result, "Recreate");
    }

    @Test
    public void testConvertionFromExtensionsV1beta1Deployment() {
        // Given
        DeploymentConfigEnricher deploymentConfigEnricher = new DeploymentConfigEnricher(context);
        io.fabric8.kubernetes.api.model.extensions.Deployment appsV1Deployment = new io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder()
                .withNewMetadata().withName("test-app").addToLabels("app", "test-app").endMetadata()
                .withNewSpec()
                .withReplicas(3)
                .withRevisionHistoryLimit(2)
                .withNewSelector().addToMatchLabels("app", "test-app").endSelector()
                .withNewTemplate()
                .withNewMetadata().addToLabels("app", "test-app").endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName("test-container")
                .withImage("test-image:1.0.0")
                .addNewPort()
                .withContainerPort(80)
                .endPort()
                .endContainer()
                .endSpec()
                .endTemplate()
                .withNewStrategy()
                .withType("Rolling")
                .withNewRollingUpdate().withMaxSurge(new IntOrString(5)).endRollingUpdate()
                .endStrategy()
                .endSpec()
                .build();
        KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder().addToItems(appsV1Deployment);

        // When
        deploymentConfigEnricher.create(PlatformMode.openshift, kubernetesListBuilder);

        // Then
        assertEquals(1, kubernetesListBuilder.getItems().size());
        HasMetadata result = kubernetesListBuilder.buildFirstItem();
        assertTrue(result instanceof DeploymentConfig);
        assertDeploymentConfig((DeploymentConfig) result, "Rolling");
    }

    private void assertDeploymentConfig(DeploymentConfig deploymentConfig, String strategyType) {
        assertEquals("test-app", deploymentConfig.getMetadata().getName());
        assertEquals(3, deploymentConfig.getSpec().getReplicas().intValue());
        assertEquals(2, deploymentConfig.getSpec().getRevisionHistoryLimit().intValue());
        assertEquals("test-app", deploymentConfig.getSpec().getTemplate().getMetadata().getLabels().get("app"));
        assertEquals("test-container", deploymentConfig.getSpec().getTemplate().getSpec().getContainers().get(0).getName());
        assertEquals("test-image:1.0.0", deploymentConfig.getSpec().getTemplate().getSpec().getContainers().get(0).getImage());
        assertEquals(strategyType, deploymentConfig.getSpec().getStrategy().getType());
    }
}
