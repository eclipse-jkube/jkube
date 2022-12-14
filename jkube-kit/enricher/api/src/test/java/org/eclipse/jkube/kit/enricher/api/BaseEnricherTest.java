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
package org.eclipse.jkube.kit.enricher.api;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.PrefixedLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.ControllerResourceConfig;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.enricher.api.BaseEnricher.getNamespace;
import static org.eclipse.jkube.kit.enricher.api.BaseEnricher.getReplicaCount;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BaseEnricherTest {
  private BaseEnricher baseEnricher;
  private ControllerResourceConfig controllerResourceConfig;
  private JKubeEnricherContext context;
  private KitLogger logger;
  private Properties properties;

  private static class TestEnricher extends BaseEnricher {
    @AllArgsConstructor
    public enum Config implements Configs.Config {
      TEST_PROPERTY("testProperty", null);
      @Getter
      private final String key;
      @Getter
      private final String defaultValue;
    }
    public TestEnricher(EnricherContext enricherContext) {
      super(enricherContext, "test-enricher");
    }
  }

  @BeforeEach
  void setup() {
    logger = new KitLogger.SilentLogger();
    controllerResourceConfig = mock(ControllerResourceConfig.class, RETURNS_DEEP_STUBS);
    context = mock(JKubeEnricherContext.class, RETURNS_DEEP_STUBS);
    properties = new Properties();
    when(context.getConfiguration().getImages()).thenReturn(Collections.emptyList());
    when(context.getLog()).thenReturn(logger);
    when(context.getProperties()).thenReturn(properties);
    baseEnricher = createNewBaseEnricher();
  }

  @Test
  void getName_whenInvoked_shouldReturnEnricherName() {
    assertThat(baseEnricher.getName()).isEqualTo("base-enricher");
  }

  @Test
  void getLog_whenInvoked_shouldReturnPrefixedLogger() {
    assertThat(baseEnricher.getLog())
        .isInstanceOf(PrefixedLogger.class)
        .hasFieldOrPropertyWithValue("log", logger);
  }

  @Test
  void enrich_whenInvoked_shouldDoNothing() {
    // Given
    KubernetesListBuilder klb = new KubernetesListBuilder();
    // When
    baseEnricher.enrich(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.buildItems()).isEmpty();
  }

  @Test
  void create_whenInvoked_shouldDoNothing() {
    // Given
    KubernetesListBuilder klb = new KubernetesListBuilder();
    // When
    baseEnricher.create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.buildItems()).isEmpty();
  }

  @Test
  void getImages_whenNoImageInConfiguration_thenReturnEmptyList() {
    assertThat(baseEnricher.getImages()).isEmpty();
  }

  @Test
  void getImages_whenImageInConfiguration_thenReturnImage() {
    // Given
    ImageConfiguration ic = ImageConfiguration.builder().name("foo/bar:latest").build();
    when(context.getConfiguration().getImages()).thenReturn(Collections.singletonList(ic));
    baseEnricher = createNewBaseEnricher();
    // When
    List<ImageConfiguration> imageConfigurationList = baseEnricher.getImages();
    // then
    assertThat(imageConfigurationList).hasSize(1).contains(ic);
  }

  @Test
  void hasImageConfiguration_whenNoImageInConfiguration_thenReturnFalse() {
    assertThat(baseEnricher.hasImageConfiguration()).isFalse();
  }

  @Test
  void hasImageConfiguration_whenImageInConfiguration_thenReturnTrue() {
    // Given
    ImageConfiguration ic = ImageConfiguration.builder().name("foo/bar:latest").build();
    when(context.getConfiguration().getImages()).thenReturn(Collections.singletonList(ic));
    baseEnricher = createNewBaseEnricher();
    // When
    boolean result = baseEnricher.hasImageConfiguration();
    // then
    assertThat(result).isTrue();
  }

  @Test
  void getConfig_whenConfigProvided_thenReturnConfigValue() {
    // Given
    properties.put("jkube.enricher.test-enricher.testProperty", "testValue");
    TestEnricher testEnricher = new TestEnricher(context);

    // When
    String result = testEnricher.getConfig(TestEnricher.Config.TEST_PROPERTY);

    // Then
    assertThat(result).isEqualTo("testValue");
  }

  @Test
  void getConfig_whenNoValueProvided_thenReturnDefaultValue() {
    // Given
    TestEnricher testEnricher = new TestEnricher(context);

    // When
    String result = testEnricher.getConfig(TestEnricher.Config.TEST_PROPERTY, "defaultValue");

    // Then
    assertThat(result).isEqualTo("defaultValue");
  }

  @Test
  void getConfigWithFallback_whenFallbackPropertyProvided_thenReturnFallbackValue() {
    // Given
    properties.put("fallback.property", "fallbackValue");
    TestEnricher testEnricher = new TestEnricher(context);

    // When
    String result = testEnricher.getConfigWithFallback(TestEnricher.Config.TEST_PROPERTY, "fallback.property", "defaultValue");

    // Then
    assertThat(result).isEqualTo("fallbackValue");
  }

  @Test
  void getConfigWithFallback_whenNothingProvided_thenReturnDefaultValue() {
    // Given
    TestEnricher testEnricher = new TestEnricher(context);

    // When
    String result = testEnricher.getConfigWithFallback(TestEnricher.Config.TEST_PROPERTY, "fallback.property", "defaultValue");

    // Then
    assertThat(result).isEqualTo("defaultValue");
  }

  @Test
  void isOpenShiftMode_ifOpenShiftPropertyPresent_thenReturnTrue() {
    // Given
    properties.put("jkube.internal.effective.platform.mode", "OPENSHIFT");
    // When
    boolean result = baseEnricher.isOpenShiftMode();
    // Then
    assertThat(result).isTrue();
  }

  @Test
  void isOpenShiftMode_ifNoPropertyPresent_thenReturnTrue() {
    // Given + When
    when(context.getProperties()).thenReturn(null);
    boolean result = baseEnricher.isOpenShiftMode();
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void getProcessingInstructionViaKey_whenInstructionPresent_thenShouldReturnList() {
    // Given
    when(context.getProcessingInstructions()).thenReturn(Collections.singletonMap("pi1", "instruction1,instruction2"));
    // When
    List<String> result = baseEnricher.getProcessingInstructionViaKey("pi1");
    // Then
    assertThat(result).contains("instruction1", "instruction2");
  }

  @Test
  void setProcessingInstruction_whenInstructionProvided_thenShouldAddItToProcessingInstructionMap() {
    // Given + When
    baseEnricher.setProcessingInstruction("pi1", Arrays.asList("c1", "c2", "c3"));
    // Then
    verify(context).setProcessingInstructions(Collections.singletonMap("pi1", "c1,c2,c3"));
  }

  @Test
  void getOpenshiftDeployTimeoutInSeconds_whenNothingProvided_thenReturnDefaultTimeout() {
    // Given + When
    long deployTimeout = baseEnricher.getOpenshiftDeployTimeoutInSeconds(10L);
    // Then
    assertThat(deployTimeout).isEqualTo(10L);
  }

  @Test
  void getOpenshiftDeployTimeoutInSeconds_whenTimeoutProvidedInProperty_thenReturnTimeout() {
    // Given
    when(context.getProperty("jkube.openshift.deployTimeoutSeconds")).thenReturn("3");
    // When
    long deployTimeout = baseEnricher.getOpenshiftDeployTimeoutInSeconds(10L);
    // Then
    assertThat(deployTimeout).isEqualTo(3L);
  }

  @Test
  void getControllerName_whenNameProvidedInConfig_thenReturnControllerName() {
    // Given
    when(controllerResourceConfig.getControllerName()).thenReturn("name-from-config");
    // When
    String controllerName = baseEnricher.getControllerName(controllerResourceConfig, "default-name");
    // Then
    assertThat(controllerName).isEqualTo("name-from-config");
  }

  @Test
  void getControllerName_whenNullConfig_thenReturnDefaultName() {
    // Given + When
    String controllerName = baseEnricher.getControllerName(controllerResourceConfig, "default-name");
    // Then
    assertThat(controllerName).isEqualTo("default-name");
  }

  @Test
  void getCreateExternalUrls_whenPropertyProvided_thenReturnValueFromProperty() {
    // Given
    when(context.getProperty("jkube.createExternalUrls")).thenReturn("true");
    // When
    boolean createExternalUrls = baseEnricher.getCreateExternalUrls();
    // Then
    assertThat(createExternalUrls).isTrue();
  }

  @Test
  void getCreateExternalUrls_whenConfigProvided_thenReturnValueFromProperty() {
    // Given
    when(context.getConfiguration().getResource()).thenReturn(ResourceConfig.builder()
            .createExternalUrls(true)
        .build());
    // When
    boolean createExternalUrls = baseEnricher.getCreateExternalUrls();
    // Then
    assertThat(createExternalUrls).isTrue();
  }

  @Test
  void getCreateExternalUrls_whenNothingProvided_thenReturnFalse() {
    // Given
    when(context.getConfiguration().getResource()).thenReturn(ResourceConfig.builder().build());
    // When
    boolean createExternalUrls = baseEnricher.getCreateExternalUrls();
    // Then
    assertThat(createExternalUrls).isFalse();
  }

  @Test
  void getReplicaCount_whenReplicaProvidedInDeployment_thenReturnDeploymentReplica() {
    // Given
    KubernetesListBuilder klb = new KubernetesListBuilder();
    klb.addToItems(new DeploymentBuilder().withNewSpec().withReplicas(5).endSpec().build());

    // When
    int replicaCount = getReplicaCount(klb, controllerResourceConfig, 1);

    // Then
    assertThat(replicaCount).isEqualTo(5);
  }

  @Test
  void getReplicaCount_whenReplicaProvidedInDeploymentConfig_thenReturnDeploymentConfigReplica() {
    // Given
    KubernetesListBuilder klb = new KubernetesListBuilder();
    klb.addToItems(new DeploymentConfigBuilder().withNewSpec().withReplicas(5).endSpec().build());

    // When
    int replicaCount = getReplicaCount(klb, controllerResourceConfig, 1);

    // Then
    assertThat(replicaCount).isEqualTo(5);
  }

  @Test
  void getReplicaCount_whenReplicaProvidedInControllerConfig_thenReturnControllerConfigReplica() {
    // Given
    KubernetesListBuilder klb = new KubernetesListBuilder();
    when(controllerResourceConfig.getReplicas()).thenReturn(5);
    // When
    int replicaCount = getReplicaCount(klb, controllerResourceConfig, 1);
    // Then
    assertThat(replicaCount).isEqualTo(5);
  }

  @Test
  void getReplicaCount_whenNullReplicaProvidedInControllerConfig_thenReturnDefaultReplica() {
    // Given
    KubernetesListBuilder klb = new KubernetesListBuilder();
    when(controllerResourceConfig.getReplicas()).thenReturn(null);
    // When
    int replicaCount = getReplicaCount(klb, controllerResourceConfig, 1);
    // Then
    assertThat(replicaCount).isEqualTo(1);
  }

  @Test
  void getReplicaCount_whenNullControllerConfig_thenReturnDefaultReplica() {
    // Given
    KubernetesListBuilder klb = new KubernetesListBuilder();
    // When
    int replicaCount = getReplicaCount(klb, null, 1);
    // Then
    assertThat(replicaCount).isEqualTo(1);
  }

  @Test
  void getNamespace_whenNamespaceInResourceConfig_thenReturnResourceConfigNamespace() {
    // Given
    ResourceConfig resourceConfig = ResourceConfig.builder()
        .namespace("namespace-from-config")
        .build();
    // When
    String namespace = getNamespace(resourceConfig, "default-namespace");
    // Then
    assertThat(namespace).isEqualTo("namespace-from-config");
  }

  @Test
  void getNamespace_whenNothingProvided_thenReturnDefaultNamespace() {
    // Given + When
    String namespace = getNamespace(null, "default-namespace");
    // Then
    assertThat(namespace).isEqualTo("default-namespace");
  }

  @Test
  void getValueFromConfig_whenBooleanPropertyProvided_thenReturnPropertyValue() {
    // Given
    when(context.getProperty("test.property")).thenReturn("true");
    // When
    boolean result = baseEnricher.getValueFromConfig("test.property", false);
    // Then
    assertThat(result).isTrue();
  }

  @Test
  void useDeploymentForOpenShift_whenSwitchDeploymentEnabled_thenReturnTrue() {
    // Given
    when(context.getProperty("jkube.build.switchToDeployment")).thenReturn("true");
    // When
    boolean result = baseEnricher.useDeploymentForOpenShift();
    // Then
    assertThat(result).isTrue();
  }

  @Test
  void useDeploymentForOpenShift_whenNoPropertyProvided_thenReturnFalse() {
    // Given + When
    boolean result = baseEnricher.useDeploymentForOpenShift();
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void getImagePullPolicy_whenNoConfigPresent_shouldReturnDefaultImagePullPolicy() {
    // Given + When
    String value = baseEnricher.getImagePullPolicy(null, null);

    // Then
    assertThat(value).isEqualTo("IfNotPresent");
  }

  @Test
  void getImagePullPolicy_whenPullPolicySpecifiedInControllerResourceConfig_shouldReturnPullPolicy() {
    // Given
    when(controllerResourceConfig.getImagePullPolicy()).thenReturn("Never");

    // When
    String value = baseEnricher.getImagePullPolicy(controllerResourceConfig, null);

    // Then
    assertThat(value).isEqualTo("Never");
  }

  @Test
  void getImagePullPolicy_whenPullPolicySpecifiedViaProperty_shouldReturnPullPolicy() {
    // Given
    when(context.getProperty("jkube.imagePullPolicy")).thenReturn("Always");

    // When
    String value = baseEnricher.getImagePullPolicy(controllerResourceConfig, null);

    // Then
    assertThat(value).isEqualTo("Always");
  }

  @Test
  void getControllerResourceConfig_whenNullResourceProvided_thenReturnsEmptyControllerResourceConfig() {
    // Given
    when(context.getConfiguration().getResource()).thenReturn(null);

    // When
    ControllerResourceConfig config = baseEnricher.getControllerResourceConfig();

    // Then
    assertThat(config).isNotNull();
  }

  @Test
  void getControllerResourceConfig_whenValidControllerResourceProvided_thenReturnsControllerResourceConfig() {
    // Given
    when(context.getConfiguration().getResource()).thenReturn(ResourceConfig.builder()
            .controller(controllerResourceConfig)
        .build());

    // When
    ControllerResourceConfig config = baseEnricher.getControllerResourceConfig();

    // Then
    assertThat(config).isEqualTo(controllerResourceConfig);
  }

  private BaseEnricher createNewBaseEnricher() {
    return new BaseEnricher(context, "base-enricher");
  }
}
