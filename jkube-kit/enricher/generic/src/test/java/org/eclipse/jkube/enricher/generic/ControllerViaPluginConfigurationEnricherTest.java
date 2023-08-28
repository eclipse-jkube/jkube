/*
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
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.openshift.api.model.DeploymentConfig;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.enricher.generic.openshift.DeploymentConfigEnricher;
import org.eclipse.jkube.enricher.generic.openshift.ImageChangeTriggerEnricher;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.EnricherContext;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ControllerViaPluginConfigurationEnricherTest {
  private ControllerViaPluginConfigurationEnricher controllerViaPluginConfigurationEnricher;
  private KubernetesListBuilder kubernetesListBuilder;
  private EnricherContext context;

  @BeforeEach
  void setUp() {
    context = JKubeEnricherContext.builder()
      .log(new KitLogger.SilentLogger())
      .project(JavaProject.builder()
        .artifactId("test-project")
        .build())
      .image(ImageConfiguration.builder()
        .name("test-project:tag")
        .build(BuildConfiguration.builder()
          .from("repository/image:tag")
          .build())
        .build())
      .resources(ResourceConfig.builder().build())
      .processorConfig(new ProcessorConfig())
      .build();
    kubernetesListBuilder = new KubernetesListBuilder();
    controllerViaPluginConfigurationEnricher = new ControllerViaPluginConfigurationEnricher(context);
  }

  @Test
  void create_withDeploymentFragment_shouldMergeOpinionatedDefaultsWithFragment() {
    // Given
    kubernetesListBuilder.addToItems(createNewDeploymentBuilder());
    // When
    controllerViaPluginConfigurationEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);
    // Then
    assertGeneratedListContainsDeploymentWithNameAndEnvVar(kubernetesListBuilder, "test-project");
  }

  @Test
  void create_withDeploymentFragmentAndConfiguredControllerName_shouldConsiderConfiguredNameInMergedResource() {
    // Given
    context.getProperties().put("jkube.enricher.jkube-controller-from-configuration.name", "configured-name");
    kubernetesListBuilder.addToItems(createNewDeploymentBuilder());
    // When
    controllerViaPluginConfigurationEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);
    // Then
    assertGeneratedListContainsDeploymentWithNameAndEnvVar(kubernetesListBuilder, "configured-name");
  }

  @Test
  void create_withDeploymentFragmentWithExistingNameAndConfiguredControllerName_shouldConsiderExistingNameInMergedResource() {
    // Given
    context.getProperties().put("jkube.enricher.jkube-controller-from-configuration.name", "configured-name");
    DeploymentBuilder deploymentFragment = createNewDeploymentBuilder().withNewMetadata()
        .withName("existing-name")
        .endMetadata();
    kubernetesListBuilder.addToItems(deploymentFragment);
    // When
    controllerViaPluginConfigurationEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);
    // Then
    assertGeneratedListContainsDeploymentWithNameAndEnvVar(kubernetesListBuilder, "existing-name");
  }

  @Test
  void create_withStatefulSetFragment_shouldMergeOpinionatedDefaultsWithFragment() {
    // Given
    kubernetesListBuilder.addToItems(createNewStatefulSetBuilder());
    // When
    controllerViaPluginConfigurationEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);
    // Then
    assertGeneratedListContainsStatefulSetWithNameAndEnvVar(kubernetesListBuilder, "test-project");
  }

  @Test
  void create_withStatefulSetFragmentAndConfiguredControllerName_shouldConsiderConfiguredNameInMergedResource() {
    // Given
    context.getProperties().put("jkube.enricher.jkube-controller-from-configuration.name", "configured-name");
    kubernetesListBuilder.addToItems(createNewStatefulSetBuilder());
    // When
    controllerViaPluginConfigurationEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);
    // Then
    assertGeneratedListContainsStatefulSetWithNameAndEnvVar(kubernetesListBuilder, "configured-name");
  }

  @Test
  void create_withStatefulSetFragmentWithExistingNameAndConfiguredControllerName_shouldConsiderExistingNameInMergedResource() {
    // Given
    context.getProperties().put("jkube.enricher.jkube-controller-from-configuration.name", "configured-name");
    StatefulSetBuilder statefulSetFragment = createNewStatefulSetBuilder().withNewMetadata()
        .withName("existing-name")
        .endMetadata();
    kubernetesListBuilder.addToItems(statefulSetFragment);
    // When
    controllerViaPluginConfigurationEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);
    // Then
    assertGeneratedListContainsStatefulSetWithNameAndEnvVar(kubernetesListBuilder, "existing-name");
  }

  @Test
  void create_withDeploymentFragmentAndImagePullPolicyPropertySet_shouldSendConfiguredPolicyToDeploymentHandler() {
    // Given
    context.getProperties().put("jkube.imagePullPolicy", "Never");
    DeploymentBuilder deploymentFragment = createNewDeploymentBuilder().withNewMetadata()
        .withName("existing-name")
        .endMetadata();
    kubernetesListBuilder.addToItems(deploymentFragment);
    // When
    controllerViaPluginConfigurationEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);
    // Then
    assertThat(kubernetesListBuilder.build().getItems()).asInstanceOf(InstanceOfAssertFactories.list(Deployment.class))
      .singleElement()
      .extracting("spec.template.spec.containers")
      .asInstanceOf(InstanceOfAssertFactories.list(Container.class))
      .singleElement()
      .hasFieldOrPropertyWithValue("imagePullPolicy", "Never");
  }

  @Test
  void enablesImageChangeTriggers() {
    // Given
    context.getProperties().put("jkube.internal.effective.platform.mode", "OPENSHIFT");
    kubernetesListBuilder.addToItems(createNewDeploymentBuilder().build());
    controllerViaPluginConfigurationEnricher.create(PlatformMode.openshift, kubernetesListBuilder);
    new DeploymentConfigEnricher(context).create(PlatformMode.openshift, kubernetesListBuilder);
    // When
    new ImageChangeTriggerEnricher(context).create(PlatformMode.openshift, kubernetesListBuilder);
    // Then
    assertThat(kubernetesListBuilder.build().getItems()).asInstanceOf(InstanceOfAssertFactories.list(DeploymentConfig.class))
      .singleElement()
      .extracting("spec.triggers").asList().hasSize(2)
      .extracting("type")
      .containsExactlyInAnyOrder("ImageChange", "ConfigChange");
  }

  private void assertGeneratedListContainsDeploymentWithNameAndEnvVar(KubernetesListBuilder kubernetesListBuilder, String name) {
    assertThat(kubernetesListBuilder.build())
        .extracting(KubernetesList::getItems)
        .asList()
        .singleElement(InstanceOfAssertFactories.type(Deployment.class))
        .hasFieldOrPropertyWithValue("metadata.name", name)
        .extracting(Deployment::getSpec)
        .extracting(DeploymentSpec::getTemplate)
        .extracting(PodTemplateSpec::getSpec)
        .extracting(PodSpec::getContainers)
        .asList()
        .first(InstanceOfAssertFactories.type(Container.class))
        .extracting(Container::getEnv)
        .asList()
        .contains(new EnvVarBuilder().withName("FOO").withValue("bar").build());
  }

  private void assertGeneratedListContainsStatefulSetWithNameAndEnvVar(KubernetesListBuilder kubernetesListBuilder, String name) {
    assertThat(kubernetesListBuilder.build())
        .extracting(KubernetesList::getItems)
        .asList()
        .singleElement(InstanceOfAssertFactories.type(StatefulSet.class))
        .hasFieldOrPropertyWithValue("metadata.name", name)
        .extracting(StatefulSet::getSpec)
        .extracting(StatefulSetSpec::getTemplate)
        .extracting(PodTemplateSpec::getSpec)
        .extracting(PodSpec::getContainers)
        .asList()
        .first(InstanceOfAssertFactories.type(Container.class))
        .extracting(Container::getEnv)
        .asList()
        .contains(new EnvVarBuilder().withName("FOO").withValue("bar").build());
  }

  private DeploymentBuilder createNewDeploymentBuilder() {
    return new DeploymentBuilder()
        .withNewSpec()
        .withNewTemplate()
        .withNewSpec()
        .addNewContainer()
        .addNewEnv()
        .withName("FOO")
        .withValue("bar")
        .endEnv()
        .endContainer()
        .endSpec()
        .endTemplate()
        .endSpec();
  }

  private StatefulSetBuilder createNewStatefulSetBuilder() {
    return new StatefulSetBuilder()
        .withNewSpec()
        .withNewTemplate()
        .withNewSpec()
        .addNewContainer()
        .addNewEnv()
        .withName("FOO")
        .withValue("bar")
        .endEnv()
        .endContainer()
        .endSpec()
        .endTemplate()
        .endSpec();
  }
}
