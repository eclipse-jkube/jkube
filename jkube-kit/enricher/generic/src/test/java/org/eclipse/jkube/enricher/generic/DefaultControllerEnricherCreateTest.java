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
import mockit.Expectations;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultControllerEnricherCreateTest {

  @SuppressWarnings("unused")
  @Mocked
  private JKubeEnricherContext buildContext;
  private Properties properties;
  private ResourceConfig resourceConfig;
  private DefaultControllerEnricher defaultControllerEnricher;
  private KubernetesListBuilder klb;

  @Before
  public void setUp() throws Exception {
    properties = new Properties();
    resourceConfig = ResourceConfig.builder().build();
    // @formatter:off
    new Expectations() {{
      buildContext.getProperties(); result = properties;
      buildContext.getConfiguration().getResource(); result = resourceConfig;
      buildContext.getConfiguration().getImages();
      result = Collections.singletonList(ImageConfiguration.builder().build());
      buildContext.getGav().getSanitizedArtifactId(); result = "artifact-id";
    }};
    // @formatter:on
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
        .isInstanceOf(Job.class);
  }

  @Test
  public void create_inKubernetesWithNoImages_shouldSkip() {
    // Given
    // @formatter:off
    new Expectations() {{
      buildContext.getConfiguration().getImages(); result = null;
    }};
    // @formatter:on
    // When
    defaultControllerEnricher.create(PlatformMode.kubernetes, klb);
    // Then
    assertThat(klb.buildItems()).isEmpty();
  }
}
