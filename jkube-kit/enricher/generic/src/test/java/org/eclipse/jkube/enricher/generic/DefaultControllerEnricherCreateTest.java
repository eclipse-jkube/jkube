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

import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;

public class DefaultControllerEnricherCreateTest {

  private JKubeEnricherContext buildContext;
  private Properties properties;
  private DefaultControllerEnricher defaultControllerEnricher;
  private KubernetesListBuilder klb;

  @Before
  public void setUp() throws Exception {
    properties = new Properties();
    ResourceConfig resourceConfig = ResourceConfig.builder().build();
    buildContext = mock(JKubeEnricherContext.class, RETURNS_DEEP_STUBS);
    when(buildContext.getProperties()).thenReturn(properties);
    when(buildContext.getConfiguration().getResource()).thenReturn(resourceConfig);
    when(buildContext.getConfiguration().getImages()).thenReturn(Collections.singletonList(ImageConfiguration.builder().build()));
    when(buildContext.getGav().getSanitizedArtifactId()).thenReturn("artifact-id");
    defaultControllerEnricher = new DefaultControllerEnricher(buildContext);
    klb = new KubernetesListBuilder();
  }

  @Test
  public void create_inKubernetesNoType_shouldCreateDeployment() {
    // When
    defaultControllerEnricher.create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.buildItems())
        .hasSize(1)
        .first()
        .isInstanceOf(Deployment.class);
  }

  /*
   * This test may seem odd, since what JKube produces for OpenShift environments are DeploymentConfig.
   *
   * However, this is taken care of by the DeploymentConfigEnricher which will convert Deployment to
   * DeploymentConfig if the environment and configuration is appropriate.
   */
  @Test
  public void create_inOpenShiftWithDeploymentConfigType_shouldCreateDeployment() {
    // Given
    properties.put("jkube.enricher.jkube-controller.type", "DeploymentConfig");
    // When
    defaultControllerEnricher.create(PlatformMode.openshift, klb);
    // Then
    assertThat(klb.buildItems())
        .hasSize(1)
        .first()
        .isInstanceOf(Deployment.class);
  }

  @Test
  public void create_inKubernetesWithStatefulSetType_shouldCreateStatefulSet() {
    // Given
    properties.put("jkube.enricher.jkube-controller.type", "StatefulSet");
    // When
    defaultControllerEnricher.create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.buildItems())
        .hasSize(1)
        .first()
        .isInstanceOf(StatefulSet.class);
  }

  @Test
  public void create_inKubernetesWithDaemonSetType_shouldCreateDaemonSet() {
    // Given
    properties.put("jkube.enricher.jkube-controller.type", "DAEMONSET");
    // When
    defaultControllerEnricher.create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.buildItems())
        .hasSize(1)
        .first()
        .isInstanceOf(DaemonSet.class);
  }

  @Test
  public void create_inKubernetesWithReplicaSetType_shouldCreateReplicaSet() {
    // Given
    properties.put("jkube.enricher.jkube-controller.type", "rePlicaSeT");
    // When
    defaultControllerEnricher.create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.buildItems())
        .hasSize(1)
        .first()
        .isInstanceOf(ReplicaSet.class);
  }

  @Test
  public void create_inKubernetesWithReplicationControllerType_shouldCreateReplicationController() {
    // Given
    properties.put("jkube.enricher.jkube-controller.type", "ReplicationController");
    // When
    defaultControllerEnricher.create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.buildItems())
        .hasSize(1)
        .first()
        .isInstanceOf(ReplicationController.class);
  }

  @Test
  public void create_inKubernetesWithJobType_shouldCreateJob() {
    // Given
    properties.put("jkube.enricher.jkube-controller.type", "Job");
    // When
    defaultControllerEnricher.create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.buildItems())
        .hasSize(1)
        .first()
        .isInstanceOf(Job.class)
        .hasFieldOrPropertyWithValue("spec.template.spec.restartPolicy", "OnFailure");
  }

  @Test
  public void create_inKubernetesWithJobTypeAndConfiguredRestartPolicy_shouldCreateJobWithConfiguredRestartPolicy() {
    // Given
    when(buildContext.getConfiguration().getResource()).thenReturn(ResourceConfig.builder().restartPolicy("Never").build());
    properties.put("jkube.enricher.jkube-controller.type", "Job");
    // When
    defaultControllerEnricher.create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.buildItems())
        .hasSize(1)
        .first()
        .isInstanceOf(Job.class)
        .hasFieldOrPropertyWithValue("spec.template.spec.restartPolicy", "Never");
  }

  @Test
  public void create_inKubernetesWithNoImages_shouldSkip() {
    // Given
    when(buildContext.getConfiguration().getImages()).thenReturn(null);
    // When
    defaultControllerEnricher.create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.buildItems()).isEmpty();
  }
}
