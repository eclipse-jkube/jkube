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


import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.apps.DaemonSetSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ObjectAssert;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class ImagePullPolicyEnricherTest {

  @Test
  public void enrich_withJkubeImagePullPolicyProperty_shouldOverrideImagePullPolicy() {
    // Given
    Properties properties = new Properties();
    properties.put("jkube.imagePullPolicy", "Never");
    ImagePullPolicyEnricher imagePullPolicyEnricher = new ImagePullPolicyEnricher(createNewJKubeEnricherContextWithProperties(properties));

    KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder();
    kubernetesListBuilder.addToItems(createNewDaemonSet());
    kubernetesListBuilder.addToItems(createNewDeployment());
    kubernetesListBuilder.addToItems(createNewDeploymentWith2Containers());
    kubernetesListBuilder.addToItems(createNewJob());
    kubernetesListBuilder.addToItems(createNewReplicaSet());
    kubernetesListBuilder.addToItems(createNewReplicationController());
    kubernetesListBuilder.addToItems(createNewStatefulSet());

    // When
    imagePullPolicyEnricher.enrich(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    List<HasMetadata> buildItems = kubernetesListBuilder.buildItems();

    assertThat(buildItems).hasSize(7);

    assertDaemonSetImagePullPolicy(buildItems, "Never", 0);
    assertDeploymentImagePullPolicy(buildItems, "Never", 1);
    assertDeploymentWithTwoContainersImagePullPolicy(buildItems, "Never", 2);
    assertJobImagePullPolicy(buildItems, "Never", 3);
    assertReplicaSetImagePullPolicy(buildItems, "Never", 4);
    assertReplicationControllerImagePullPolicy(buildItems, "Never", 5);
    assertStatefulSetImagePullPolicy(buildItems, "Never", 6);

  }

  @Test
  public void enrich_withDefaults_shouldNotOverrideImagePullPolicy() {
    // Given
    Properties properties = new Properties();
    ImagePullPolicyEnricher imagePullPolicyEnricher = new ImagePullPolicyEnricher(createNewJKubeEnricherContextWithProperties(properties));

    KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder();
    kubernetesListBuilder.addToItems(createNewDaemonSet());
    kubernetesListBuilder.addToItems(createNewDeployment());
    kubernetesListBuilder.addToItems(createNewDeploymentWith2Containers());
    kubernetesListBuilder.addToItems(createNewJob());
    kubernetesListBuilder.addToItems(createNewReplicaSet());
    kubernetesListBuilder.addToItems(createNewReplicationController());
    kubernetesListBuilder.addToItems(createNewStatefulSet());

    // When
    imagePullPolicyEnricher.enrich(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    List<HasMetadata> buildItems = kubernetesListBuilder.buildItems();

    assertThat(buildItems).hasSize(7);

    assertDaemonSetImagePullPolicy(buildItems, "IfNotPresent", 0);
    assertDeploymentImagePullPolicy(buildItems, "IfNotPresent", 1);
    assertDeploymentWithTwoContainersImagePullPolicy(buildItems, "IfNotPresent", 2);
    assertJobImagePullPolicy(buildItems, "IfNotPresent", 3);
    assertReplicaSetImagePullPolicy(buildItems, "IfNotPresent", 4);
    assertReplicationControllerImagePullPolicy(buildItems, "IfNotPresent", 5);
    assertStatefulSetImagePullPolicy(buildItems, "IfNotPresent", 6);

  }

  private DaemonSet createNewDaemonSet() {
    return new DaemonSetBuilder()
        .withNewSpec()
        .withNewTemplate()
        .withNewSpec()
        .addNewContainer().withImagePullPolicy("IfNotPresent")
        .endContainer()
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
  }

  private Deployment createNewDeployment() {
    return new DeploymentBuilder()
        .withNewSpec()
        .withNewTemplate()
        .withNewSpec()
        .addNewContainer()
        .withImagePullPolicy("IfNotPresent")
        .endContainer()
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
  }

  private Deployment createNewDeploymentWith2Containers() {
    return new DeploymentBuilder()
        .withNewSpec()
        .withNewTemplate()
        .withNewSpec()
        .addNewContainer()
        .withImagePullPolicy("IfNotPresent")
        .endContainer()
        .addNewContainer()
        .withImagePullPolicy("IfNotPresent")
        .endContainer()
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
  }

  private Job createNewJob() {
    return new JobBuilder()
        .withNewSpec()
        .withNewTemplate()
        .withNewSpec()
        .addNewContainer().withImagePullPolicy("IfNotPresent")
        .endContainer()
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
  }

  private ReplicaSet createNewReplicaSet() {
    return new ReplicaSetBuilder()
        .withNewSpec()
        .withNewTemplate()
        .withNewSpec()
        .addNewContainer().withImagePullPolicy("IfNotPresent")
        .endContainer()
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
  }

  private ReplicationController createNewReplicationController() {
    return new ReplicationControllerBuilder()
        .withNewSpec()
        .withNewTemplate()
        .withNewSpec()
        .addNewContainer().withImagePullPolicy("IfNotPresent")
        .endContainer()
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
  }

  private StatefulSet createNewStatefulSet() {
    return new StatefulSetBuilder()
        .withNewSpec()
        .withNewTemplate()
        .withNewSpec()
        .addNewContainer().withImagePullPolicy("IfNotPresent")
        .endContainer()
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
  }

  private void assertDaemonSetImagePullPolicy(List<HasMetadata> buildItems, String expectedImagePullPolicy, int index) {
    assertThat(buildItems)
        .element(index, InstanceOfAssertFactories.type(DaemonSet.class))
        .extracting(DaemonSet::getSpec)
        .extracting(DaemonSetSpec::getTemplate)
        .extracting(PodTemplateSpec::getSpec)
        .extracting(PodSpec::getContainers)
        .asList()
        .hasSize(1)
        .first(InstanceOfAssertFactories.type(Container.class))
        .extracting(Container::getImagePullPolicy)
        .isEqualTo(expectedImagePullPolicy);
  }

  private void assertDeploymentImagePullPolicy(List<HasMetadata> buildItems, String expectedImagePullPolicy, int index) {
    assertThat(buildItems)
        .element(index, InstanceOfAssertFactories.type(Deployment.class))
        .extracting(Deployment::getSpec)
        .extracting(DeploymentSpec::getTemplate)
        .extracting(PodTemplateSpec::getSpec)
        .extracting(PodSpec::getContainers)
        .asList()
        .hasSize(1)
        .first(InstanceOfAssertFactories.type(Container.class))
        .extracting(Container::getImagePullPolicy)
        .isEqualTo(expectedImagePullPolicy);
  }

  private void assertDeploymentWithTwoContainersImagePullPolicy(List<HasMetadata> buildItems, String expectedImagePullPolicy, int index) {
    AbstractListAssert<?, List<?>, Object, ObjectAssert<Object>> assertThatContainerList =
        assertThat(buildItems)
            .element(index, InstanceOfAssertFactories.type(Deployment.class))
            .extracting(Deployment::getSpec)
            .extracting(DeploymentSpec::getTemplate)
            .extracting(PodTemplateSpec::getSpec)
            .extracting(PodSpec::getContainers)
            .asList();
    assertThatContainerList
        .hasSize(2);
    assertThatContainerList
        .first(InstanceOfAssertFactories.type(Container.class))
        .extracting(Container::getImagePullPolicy)
        .isEqualTo(expectedImagePullPolicy);
    assertThatContainerList
        .last(InstanceOfAssertFactories.type(Container.class))
        .extracting(Container::getImagePullPolicy)
        .isEqualTo(expectedImagePullPolicy);
  }

  private void assertJobImagePullPolicy(List<HasMetadata> buildItems, String expectedImagePullPolicy, int index) {
    assertThat(buildItems)
        .element(index, InstanceOfAssertFactories.type(Job.class))
        .extracting(Job::getSpec)
        .extracting(JobSpec::getTemplate)
        .extracting(PodTemplateSpec::getSpec)
        .extracting(PodSpec::getContainers)
        .asList()
        .hasSize(1)
        .first(InstanceOfAssertFactories.type(Container.class))
        .extracting(Container::getImagePullPolicy)
        .isEqualTo(expectedImagePullPolicy);
  }

  private void assertReplicaSetImagePullPolicy(List<HasMetadata> buildItems, String expectedImagePullPolicy, int index) {
    assertThat(buildItems)
        .element(index, InstanceOfAssertFactories.type(ReplicaSet.class))
        .extracting(ReplicaSet::getSpec)
        .extracting(ReplicaSetSpec::getTemplate)
        .extracting(PodTemplateSpec::getSpec)
        .extracting(PodSpec::getContainers)
        .asList()
        .hasSize(1)
        .first(InstanceOfAssertFactories.type(Container.class))
        .extracting(Container::getImagePullPolicy)
        .isEqualTo(expectedImagePullPolicy);
  }

  private void assertReplicationControllerImagePullPolicy(List<HasMetadata> buildItems, String expectedImagePullPolicy, int index) {
    assertThat(buildItems)
        .element(index, InstanceOfAssertFactories.type(ReplicationController.class))
        .extracting(ReplicationController::getSpec)
        .extracting(ReplicationControllerSpec::getTemplate)
        .extracting(PodTemplateSpec::getSpec)
        .extracting(PodSpec::getContainers)
        .asList()
        .hasSize(1)
        .first(InstanceOfAssertFactories.type(Container.class))
        .extracting(Container::getImagePullPolicy)
        .isEqualTo(expectedImagePullPolicy);
  }

  private void assertStatefulSetImagePullPolicy(List<HasMetadata> buildItems, String expectedImagePullPolicy, int index) {
    assertThat(buildItems)
        .element(index, InstanceOfAssertFactories.type(StatefulSet.class))
        .extracting(StatefulSet::getSpec)
        .extracting(StatefulSetSpec::getTemplate)
        .extracting(PodTemplateSpec::getSpec)
        .extracting(PodSpec::getContainers)
        .asList()
        .hasSize(1)
        .first(InstanceOfAssertFactories.type(Container.class))
        .extracting(Container::getImagePullPolicy)
        .isEqualTo(expectedImagePullPolicy);

  }

  private JKubeEnricherContext createNewJKubeEnricherContextWithProperties(Properties properties) {
    return JKubeEnricherContext.builder()
        .log(new KitLogger.SilentLogger())
        .project(JavaProject.builder()
            .properties(properties)
            .build())
        .build();
  }
}
