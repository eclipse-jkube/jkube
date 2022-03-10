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
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetSpec;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigSpec;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.EnricherContext;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class PodAnnotationEnricherTest {
  private KubernetesListBuilder klb;
  private PodAnnotationEnricher podAnnotationEnricher;

  @Before
  public void setUp() {
    Properties properties = new Properties();
    klb = new KubernetesListBuilder();
    EnricherContext context = JKubeEnricherContext.builder()
        .project(JavaProject.builder()
            .properties(properties)
            .build())
        .build();
    podAnnotationEnricher = new PodAnnotationEnricher(context);
  }

  @Test
  public void enrich_withDeployment_shouldAddAnnotationsToPodTemplateSpec() {
    // Given
    klb.addToItems(new DeploymentBuilder()
        .withMetadata(createResourceMetadata())
        .withNewSpec()
        .withNewTemplate().withMetadata(createPodTemplateSpecMetadata()).endTemplate()
        .endSpec());

    // When
    podAnnotationEnricher.enrich(PlatformMode.kubernetes, klb);

    // Then
    KubernetesList kubernetesList = klb.build();
    assertThat(kubernetesList.getItems())
        .hasSize(1)
        .first()
        .isInstanceOf(Deployment.class);
    Deployment deployment = (Deployment) kubernetesList.getItems().get(0);
    assertThat(deployment).extracting(Deployment::getSpec)
        .extracting(DeploymentSpec::getTemplate)
        .extracting(PodTemplateSpec::getMetadata)
        .extracting(ObjectMeta::getAnnotations)
        .hasFieldOrPropertyWithValue("key1", "value1")
        .hasFieldOrPropertyWithValue("key2", "value2");
  }

  @Test
  public void enrich_withDeploymentConfig_shouldAddAnnotationsToPodTemplateSpec() {
    // Given
    klb.addToItems(new DeploymentConfigBuilder()
        .withMetadata(createResourceMetadata())
        .withNewSpec()
        .withNewTemplate().withMetadata(createPodTemplateSpecMetadata()).endTemplate()
        .endSpec());

    // When
    podAnnotationEnricher.enrich(PlatformMode.openshift, klb);

    // Then
    KubernetesList kubernetesList = klb.build();
    assertThat(kubernetesList.getItems())
        .hasSize(1)
        .first()
        .isInstanceOf(DeploymentConfig.class);
    DeploymentConfig deploymentConfig = (DeploymentConfig) kubernetesList.getItems().get(0);
    assertThat(deploymentConfig).extracting(DeploymentConfig::getSpec)
        .extracting(DeploymentConfigSpec::getTemplate)
        .extracting(PodTemplateSpec::getMetadata)
        .extracting(ObjectMeta::getAnnotations)
        .hasFieldOrPropertyWithValue("key1", "value1")
        .hasFieldOrPropertyWithValue("key2", "value2");
  }

  @Test
  public void enrich_withReplicaSet_shouldAddAnnotationsToPodTemplateSpec() {
    // Given
    klb.addToItems(new ReplicaSetBuilder()
        .withMetadata(createResourceMetadata())
        .withNewSpec()
        .withNewTemplate().withMetadata(createPodTemplateSpecMetadata()).endTemplate()
        .endSpec());

    // When
    podAnnotationEnricher.enrich(PlatformMode.kubernetes, klb);

    // Then
    KubernetesList kubernetesList = klb.build();
    assertThat(kubernetesList.getItems())
        .hasSize(1)
        .first()
        .isInstanceOf(ReplicaSet.class);
    ReplicaSet replicaSet = (ReplicaSet) kubernetesList.getItems().get(0);
    assertThat(replicaSet).extracting(ReplicaSet::getSpec)
        .extracting(ReplicaSetSpec::getTemplate)
        .extracting(PodTemplateSpec::getMetadata)
        .extracting(ObjectMeta::getAnnotations)
        .hasFieldOrPropertyWithValue("key1", "value1")
        .hasFieldOrPropertyWithValue("key2", "value2");
  }

  private ObjectMeta createResourceMetadata() {
    return new ObjectMetaBuilder()
        .addToAnnotations("key1", "value1")
        .build();
  }

  private ObjectMeta createPodTemplateSpecMetadata() {
    return new ObjectMetaBuilder()
        .addToAnnotations("key2", "value2")
        .build();
  }
}
