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

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigSpec;
import io.fabric8.openshift.api.model.DeploymentTriggerImageChangeParamsBuilder;
import io.fabric8.openshift.api.model.DeploymentTriggerPolicy;
import io.fabric8.openshift.api.model.DeploymentTriggerPolicyBuilder;
import mockit.Expectations;
import mockit.Mocked;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class ImageChangeTriggerEnricherTest {
  @Mocked
  private JKubeEnricherContext context;

  @Test
  public void create_shouldAddImageChangeTriggers_whenDeploymentConfigPresent() {
    // Given
    Properties properties = new Properties();
    properties.put("jkube.internal.effective.platform.mode", "OPENSHIFT");
    new Expectations() {{
      context.getProperties();
      result = properties;
      context.getProcessingInstructions();
      result = Collections.singletonMap("IMAGECHANGE_TRIGGER", "test-container");
    }};
    ImageChangeTriggerEnricher imageChangeTriggerEnricher = new ImageChangeTriggerEnricher(context);
    KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder();
    kubernetesListBuilder.addToItems(createNewDeploymentConfigBuilder());

    // When
    imageChangeTriggerEnricher.create(PlatformMode.openshift, kubernetesListBuilder);

    // Then
    assertDeploymentConfigHasTriggers(kubernetesListBuilder, 1, createNewDeploymentTriggerPolicy("test-container", "test-container:latest"));
  }

  @Test
  public void create_withEnricherAllTrue_shouldAddTriggersToAllContainers() {
    // Given
    Properties properties = new Properties();
    properties.put("jkube.internal.effective.platform.mode", "OPENSHIFT");
    new Expectations() {{
      context.getProperty("jkube.openshift.enrichAllWithImageChangeTrigger");
      result = "true";
      context.getProperties();
      result = properties;
    }};
    ImageChangeTriggerEnricher imageChangeTriggerEnricher = new ImageChangeTriggerEnricher(context);
    KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder();
    kubernetesListBuilder.addToItems(createNewDeploymentConfigBuilder().editSpec().editTemplate().editSpec().addNewContainer()
        .withName("second-container")
        .withImage("second-user/second-container:latest")
        .endContainer().endSpec().endTemplate().endSpec());

    // When
    imageChangeTriggerEnricher.create(PlatformMode.openshift, kubernetesListBuilder);

    // Then
    assertDeploymentConfigHasTriggers(kubernetesListBuilder, 2,
        createNewDeploymentTriggerPolicy("test-container", "test-container:latest"),
        createNewDeploymentTriggerPolicy("second-container", "second-container:latest"));
  }

  @Test
  public void create_withContainersInConfig_shouldAddImageChangeTriggerDeploymentConfigPresent() {
    // Given
    Properties properties = new Properties();
    properties.put("jkube.internal.effective.platform.mode", "OPENSHIFT");
    properties.put("jkube.enricher.jkube-openshift-imageChangeTrigger.containers", "test-container");
    new Expectations() {{
      context.getProperties();
      result = properties;
    }};
    ImageChangeTriggerEnricher imageChangeTriggerEnricher = new ImageChangeTriggerEnricher(context);
    KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder();
    kubernetesListBuilder.addToItems(createNewDeploymentConfigBuilder());

    // When
    imageChangeTriggerEnricher.create(PlatformMode.openshift, kubernetesListBuilder);

    // Then
    assertDeploymentConfigHasTriggers(kubernetesListBuilder, 1, createNewDeploymentTriggerPolicy("test-container", "test-container:latest"));
  }

  @Test
  public void create_withTrimImageInContainerSpec_shouldTrimImageInContainers() {
    // Given
    Properties properties = new Properties();
    properties.put("jkube.internal.effective.platform.mode", "OPENSHIFT");
    new Expectations() {{
      context.getProperties();
      result = properties;
      context.getProperty("jkube.openshift.trimImageInContainerSpec");
      result = "true";
      context.getProcessingInstructions();
      result = Collections.singletonMap("IMAGECHANGE_TRIGGER", "test-container");
    }};
    ImageChangeTriggerEnricher imageChangeTriggerEnricher = new ImageChangeTriggerEnricher(context);
    KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder();
    kubernetesListBuilder.addToItems(createNewDeploymentConfigBuilder());

    // When
    imageChangeTriggerEnricher.create(PlatformMode.openshift, kubernetesListBuilder);

    // Then
    assertDeploymentConfigHasTriggers(kubernetesListBuilder, 1, createNewDeploymentTriggerPolicy("test-container", "test-container:latest"));
    assertThat(kubernetesListBuilder.buildItems())
        .hasSize(1)
        .first(InstanceOfAssertFactories.type(DeploymentConfig.class))
        .extracting(DeploymentConfig::getSpec)
        .extracting(DeploymentConfigSpec::getTemplate)
        .extracting(PodTemplateSpec::getSpec)
        .extracting(PodSpec::getContainers)
        .asList()
        .first(InstanceOfAssertFactories.type(Container.class))
        .extracting(Container::getImage)
        .isEqualTo("");
  }

  @Test
  public void create_withJibBuildStrategy_thenNoImageChangeTriggerAdded() {
    // Given
    Properties properties = new Properties();
    properties.put("jkube.internal.effective.platform.mode", "OPENSHIFT");
    new Expectations() {{
      context.getProperties();
      result = properties;
      context.getConfiguration().getJKubeBuildStrategy();
      result = JKubeBuildStrategy.jib;
    }};
    ImageChangeTriggerEnricher imageChangeTriggerEnricher = new ImageChangeTriggerEnricher(context);
    KubernetesListBuilder kubernetesListBuilder = new KubernetesListBuilder();
    kubernetesListBuilder.addToItems(createNewDeploymentConfigBuilder());

    // When
    imageChangeTriggerEnricher.create(PlatformMode.openshift, kubernetesListBuilder);

    // Then
    assertThat(kubernetesListBuilder.buildItems()).hasSize(1).asList()
        .first()
        .extracting("spec.triggers")
        .asList()
        .isEmpty();
  }

  private DeploymentConfigBuilder createNewDeploymentConfigBuilder() {
    return new DeploymentConfigBuilder()
        .withNewMetadata().withName("test-dc").endMetadata()
        .withNewSpec()
        .withNewTemplate()
        .withNewSpec()
        .addNewContainer()
        .withName("test-container")
        .withImage("test-user/test-container:latest")
        .endContainer()
        .endSpec()
        .endTemplate()
        .endSpec();
  }

  private void assertDeploymentConfigHasTriggers(KubernetesListBuilder kubernetesListBuilder, int triggersSize, DeploymentTriggerPolicy... triggerPolicies) {
    List<HasMetadata> items = kubernetesListBuilder.buildItems();
    assertThat(items)
        .hasSize(1)
        .asList()
        .first()
        .extracting("spec.triggers")
        .asList()
        .hasSize(triggersSize)
        .containsExactlyInAnyOrder(triggerPolicies);
  }

  private DeploymentTriggerPolicy createNewDeploymentTriggerPolicy(String containerName, String tagName) {
    return new DeploymentTriggerPolicyBuilder()
        .withType("ImageChange")
        .withImageChangeParams(new DeploymentTriggerImageChangeParamsBuilder()
            .withAutomatic(true)
            .withContainerNames(containerName)
            .withNewFrom()
            .withKind("ImageStreamTag")
            .withName(tagName)
            .endFrom()
            .build())
        .build();
  }
}
