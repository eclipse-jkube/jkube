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
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.EnricherContext;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.handler.DeploymentHandler;
import org.eclipse.jkube.kit.enricher.handler.StatefulSetHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Properties;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ControllerViaPluginConfigurationEnricherTest {
  private ControllerViaPluginConfigurationEnricher controllerViaPluginConfigurationEnricher;
  private KubernetesListBuilder kubernetesListBuilder;
  private EnricherContext context;
  private DeploymentHandler mockedDeploymentHandler;

  @BeforeEach
  void setUp() {
    context = mock(JKubeEnricherContext.class, RETURNS_DEEP_STUBS);
    when(context.getGav().getSanitizedArtifactId()).thenReturn("test-project");
    kubernetesListBuilder = new KubernetesListBuilder();
  }

  @Test
  void create_withDeploymentFragment_shouldMergeOpinionatedDefaultsWithFragment() {
    // Given
    mockDeploymentHandler();
    controllerViaPluginConfigurationEnricher = new ControllerViaPluginConfigurationEnricher(context);
    kubernetesListBuilder.addToItems(createNewDeploymentBuilder());

    // When
    controllerViaPluginConfigurationEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    assertGeneratedListContainsDeploymentWithNameAndEnvVar(kubernetesListBuilder, "test-project");
  }

  @Test
  void create_withDeploymentFragmentAndConfiguredControllerName_shouldConsiderConfiguredNameInMergedResource() {
    // Given
    mockDeploymentHandler();
    controllerViaPluginConfigurationEnricher = new ControllerViaPluginConfigurationEnricher(context);
    Properties properties = new Properties();
    properties.put("jkube.enricher.jkube-controller-from-configuration.name", "configured-name");
    when(context.getProperties()).thenReturn(properties);
    kubernetesListBuilder.addToItems(createNewDeploymentBuilder());

    // When
    controllerViaPluginConfigurationEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    assertGeneratedListContainsDeploymentWithNameAndEnvVar(kubernetesListBuilder, "configured-name");
  }

  @Test
  void create_withDeploymentFragmentWithExistingNameAndConfiguredControllerName_shouldConsiderExistingNameInMergedResource() {
    // Given
    mockDeploymentHandler();
    controllerViaPluginConfigurationEnricher = new ControllerViaPluginConfigurationEnricher(context);
    Properties properties = new Properties();
    properties.put("jkube.enricher.jkube-controller-from-configuration.name", "configured-name");
    when(context.getProperties()).thenReturn(properties);
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
    mockStatefulSetHandler();
    controllerViaPluginConfigurationEnricher = new ControllerViaPluginConfigurationEnricher(context);
    kubernetesListBuilder.addToItems(createNewStatefulSetBuilder());

    // When
    controllerViaPluginConfigurationEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    assertGeneratedListContainsStatefulSetWithNameAndEnvVar(kubernetesListBuilder, "test-project");
  }

  @Test
  void create_withStatefulSetFragmentAndConfiguredControllerName_shouldConsiderConfiguredNameInMergedResource() {
    // Given
    mockStatefulSetHandler();
    controllerViaPluginConfigurationEnricher = new ControllerViaPluginConfigurationEnricher(context);
    Properties properties = new Properties();
    properties.put("jkube.enricher.jkube-controller-from-configuration.name", "configured-name");
    when(context.getProperties()).thenReturn(properties);
    kubernetesListBuilder.addToItems(createNewStatefulSetBuilder());

    // When
    controllerViaPluginConfigurationEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    assertGeneratedListContainsStatefulSetWithNameAndEnvVar(kubernetesListBuilder, "configured-name");
  }

  @Test
  void create_withStatefulSetFragmentWithExistingNameAndConfiguredControllerName_shouldConsiderExistingNameInMergedResource() {
    // Given
    mockStatefulSetHandler();
    controllerViaPluginConfigurationEnricher = new ControllerViaPluginConfigurationEnricher(context);
    Properties properties = new Properties();
    properties.put("jkube.enricher.jkube-controller-from-configuration.name", "configured-name");
    when(context.getProperties()).thenReturn(properties);
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
    mockDeploymentHandler();
    ArgumentCaptor<ResourceConfig> resourceConfigArgumentCaptor = ArgumentCaptor.forClass(ResourceConfig.class);
    controllerViaPluginConfigurationEnricher = new ControllerViaPluginConfigurationEnricher(context);
    when(context.getProperty("jkube.imagePullPolicy")).thenReturn("Never");
    DeploymentBuilder deploymentFragment = createNewDeploymentBuilder().withNewMetadata()
        .withName("existing-name")
        .endMetadata();
    kubernetesListBuilder.addToItems(deploymentFragment);

    // When
    controllerViaPluginConfigurationEnricher.create(PlatformMode.kubernetes, kubernetesListBuilder);

    // Then
    verify(mockedDeploymentHandler, times(1))
        .get(resourceConfigArgumentCaptor.capture(), any());
    assertThat(resourceConfigArgumentCaptor.getValue())
        .hasFieldOrPropertyWithValue("imagePullPolicy", "Never");
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

  private void mockDeploymentHandler() {
    mockedDeploymentHandler = mock(DeploymentHandler.class, RETURNS_DEEP_STUBS);
    when(context.getHandlerHub().getHandlerFor(Deployment.class)).thenReturn(mockedDeploymentHandler);
    when(mockedDeploymentHandler.get(any(), any())).thenReturn(createOpinionatedDeployment());
  }

  private void mockStatefulSetHandler() {
    StatefulSetHandler mockedStatefulSetHandler = mock(StatefulSetHandler.class, RETURNS_DEEP_STUBS);
    when(context.getHandlerHub().getHandlerFor(StatefulSet.class)).thenReturn(mockedStatefulSetHandler);
    when(mockedStatefulSetHandler.get(any(), any())).thenReturn(createOpinionatedStatefulSet());
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

  private Deployment createOpinionatedDeployment() {
    return new DeploymentBuilder()
        .withNewMetadata().withName("test-project").endMetadata()
        .withNewSpec()
        .endSpec()
        .build();
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

  private StatefulSet createOpinionatedStatefulSet() {
    return new StatefulSetBuilder()
        .withNewMetadata().withName("test-project").endMetadata()
        .withNewSpec()
        .endSpec()
        .build();
  }
}
