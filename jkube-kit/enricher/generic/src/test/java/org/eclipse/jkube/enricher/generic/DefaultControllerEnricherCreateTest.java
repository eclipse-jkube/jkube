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

import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Properties;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class DefaultControllerEnricherCreateTest {

  private JKubeEnricherContext buildContext;
  private Properties properties;
  private KubernetesListBuilder klb;

  @BeforeEach
  void setUp() {
    properties = new Properties();
    buildContext = JKubeEnricherContext.builder()
        .log(new KitLogger.SilentLogger())
        .resources(ResourceConfig.builder().build())
        .image(ImageConfiguration.builder().build())
        .project(JavaProject.builder()
            .properties(properties)
            .groupId("group")
            .artifactId("artifact-id")
            .build())
        .build();
    klb = new KubernetesListBuilder();
  }

  @Test
  void create_inKubernetesNoType_shouldCreateDeployment() {
    // When
    new DefaultControllerEnricher(buildContext).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.buildItems())
        .hasSize(1)
        .first()
        .isInstanceOf(Deployment.class);
  }

  @DisplayName("create with different controllers")
  @ParameterizedTest(name = "in ''{1}'' mode with ''{0}'' controller type, should create ''{0}''")
  @MethodSource("controllers")
  void create_withDifferentControllers(String controllerType, PlatformMode platformMode, Class<? extends KubernetesListBuilder> clazz) {
    // Given
    properties.put("jkube.enricher.jkube-controller.type", controllerType);
    // When
    new DefaultControllerEnricher(buildContext).create(platformMode, klb);
    // Then
    assertThat(klb.buildItems())
        .singleElement()
        .isInstanceOf(clazz);
  }

  static Stream<Arguments> controllers() {
    return Stream.of(
      /*
       * This test may seem odd, since what JKube produces for OpenShift environments are DeploymentConfig.
       *
       * However, this is taken care of by the DeploymentConfigEnricher which will convert Deployment to
       * DeploymentConfig if the environment and configuration is appropriate.
       */
        arguments("DeploymentConfig", PlatformMode.openshift, Deployment.class),
        arguments("StatefulSet", PlatformMode.kubernetes, StatefulSet.class),
        arguments("DAEMONSET", PlatformMode.kubernetes, DaemonSet.class),
        arguments("rePlicaSeT", PlatformMode.kubernetes, ReplicaSet.class),
        arguments("ReplicationController", PlatformMode.kubernetes, ReplicationController.class));
  }

  @Test
  void create_inKubernetesWithJobType_shouldCreateJob() {
    // Given
    properties.put("jkube.enricher.jkube-controller.type", "Job");
    // When
    new DefaultControllerEnricher(buildContext).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.buildItems())
        .singleElement()
        .isInstanceOf(Job.class)
        .hasFieldOrPropertyWithValue("spec.template.spec.restartPolicy", "OnFailure");
  }

  @Test
  void create_inKubernetesWithJobTypeAndConfiguredRestartPolicy_shouldCreateJobWithConfiguredRestartPolicy() {
    // Given
    buildContext = buildContext.toBuilder().resources(ResourceConfig.builder().restartPolicy("Never").build()).build();
    properties.put("jkube.enricher.jkube-controller.type", "Job");
    // When
    new DefaultControllerEnricher(buildContext).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.buildItems())
        .singleElement()
        .isInstanceOf(Job.class)
        .hasFieldOrPropertyWithValue("spec.template.spec.restartPolicy", "Never");
  }

  @Test
  void create_inKubernetesWithNoImages_shouldSkip() {
    // Given
    buildContext = buildContext.toBuilder().clearImages().build();
    // When
    new DefaultControllerEnricher(buildContext).create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.buildItems()).isEmpty();
  }
}
