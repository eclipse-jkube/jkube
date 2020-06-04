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
package org.eclipse.jkube.kit.config.access;

import java.net.UnknownHostException;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ClusterAccessTest {

  @Mocked
  private KitLogger logger;

  @Mocked
  private DefaultKubernetesClient defaultKubernetesClient;

  @Mocked
  private DefaultOpenShiftClient defaultOpenShiftClient;

  @Test
  public void isOpenShiftOpenShiftClusterShouldReturnTrue() {
    // Given
    // @formatter:off
    new Expectations() {{
      defaultKubernetesClient.isAdaptable(OpenShiftClient.class); result = true;
    }};
    // @formatter:on
    // When
    final boolean result = new ClusterAccess(logger, null).isOpenShift();
    // Then
    assertTrue(result);
  }

  @Test
  public void isOpenShiftKubernetesClusterShouldReturnFalse() {
    // Given
    // @formatter:off
    new Expectations() {{
      defaultKubernetesClient.isAdaptable(OpenShiftClient.class); result = false;
    }};
    // @formatter:on
    // When
    final boolean result = new ClusterAccess(logger, null).isOpenShift();
    // Then
    assertFalse(result);
  }

  @Test
  public void isOpenShiftThrowsExceptionShouldReturnFalse() {
    // Given
    // @formatter:off
    new Expectations() {{
      defaultKubernetesClient.isAdaptable(OpenShiftClient.class); result = new KubernetesClientException("ERROR", new UnknownHostException());
    }};
    // @formatter:on
    // When
    final boolean result = new ClusterAccess(logger, null).isOpenShift();
    // Then
    assertFalse(result);
    // @formatter:off
    new Verifications() {{
      logger.warn(withPrefix("Cannot access cluster for detecting mode"), "Unknown host ", any);
    }};
    // @formatter:on
  }

  @Test
  public void createDefaultClientInKubernetesShouldReturnKubernetesClient() {
    // When
    final KubernetesClient result = new ClusterAccess(logger, null).createDefaultClient();
    // Then
    assertNotNull(result);
    assertFalse(result instanceof OpenShiftClient);
  }

  @Test
  public void createDefaultClientInOpenShiftShouldReturnOpenShiftClient() {
    // Given
    // @formatter:off
    new Expectations() {{
      defaultKubernetesClient.isAdaptable(OpenShiftClient.class); result = true;
    }};
    // @formatter:on

    // When
    final KubernetesClient result = new ClusterAccess(logger, null).createDefaultClient();
    // Then
    assertNotNull(result);
    assertTrue(result instanceof OpenShiftClient);
  }

  @Test
  public void resolveRuntimeModeWithAutoInKubernetesShouldReturnKubernetes() {
    // When
    final RuntimeMode result = new ClusterAccess(logger, null).resolveRuntimeMode(null);
    // Then
    assertEquals(RuntimeMode.kubernetes, result);
  }

  @Test
  public void resolveRuntimeModeWithAutoInOpenShiftShouldReturnOpenShift() {
    // Given
    // @formatter:off
    new Expectations() {{
      defaultKubernetesClient.isAdaptable(OpenShiftClient.class); result = true;
      defaultOpenShiftClient.supportsOpenShiftAPIGroup("image.openshift.io"); result = true;
    }};
    // @formatter:on
    // When
    final RuntimeMode result = new ClusterAccess(logger, null).resolveRuntimeMode(null);
    // Then
    assertEquals(RuntimeMode.openshift, result);
  }

  @Test
  public void resolveRuntimeModeWithSpecificShouldReturnSpecific() {
    // When
    final RuntimeMode result = new ClusterAccess(logger, null).resolveRuntimeMode(RuntimeMode.openshift);
    // Then
    assertEquals(RuntimeMode.openshift, result);
  }
}
