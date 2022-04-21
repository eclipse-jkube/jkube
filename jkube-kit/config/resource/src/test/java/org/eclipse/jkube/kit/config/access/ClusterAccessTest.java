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
import java.util.function.Consumer;

import org.eclipse.jkube.kit.common.KitLogger;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedConstruction;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClusterAccessTest {

  private KitLogger logger;
  private MockedConstruction<DefaultKubernetesClient> kubernetesClientMockedConstruction;
  private Consumer<OpenShiftClient> onInstantiate;

  @Before
  public void setUp() throws Exception {
    logger = spy(new KitLogger.SilentLogger());
    onInstantiate = oc -> {
    };
    kubernetesClientMockedConstruction = mockConstruction(DefaultKubernetesClient.class, (mock, ctx) -> {
      final OpenShiftClient oc = mock(OpenShiftClient.class);
      when(mock.adapt(OpenShiftClient.class)).thenReturn(oc);
      onInstantiate.accept(oc);
    });
  }

  @After
  public void tearDown() throws Exception {
    kubernetesClientMockedConstruction.close();
  }

  @Test
  public void isOpenShiftOpenShiftClusterShouldReturnTrue() {
    // Given
    onInstantiate = oc -> when(oc.isSupported()).thenReturn(true);
    // When
    final boolean result = new ClusterAccess(logger, null).isOpenShift();
    // Then
    assertTrue(result);
  }

  @Test
  public void isOpenShiftKubernetesClusterShouldReturnFalse() {
    // Given
    onInstantiate = oc -> when(oc.isSupported()).thenReturn(false);
    // When
    final boolean result = new ClusterAccess(logger, null).isOpenShift();
    // Then
    assertFalse(result);
  }

  @Test
  public void isOpenShiftThrowsExceptionShouldReturnFalse() {
    // Given
    onInstantiate = oc -> when(oc.isSupported())
        .thenThrow(new KubernetesClientException("ERROR", new UnknownHostException()));
    // When
    final boolean result = new ClusterAccess(logger, null).isOpenShift();
    // Then
    assertFalse(result);
    verify(logger, times(1))
        .warn(startsWith("Cannot access cluster for detecting mode"), eq("Unknown host "), isNull());
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
    onInstantiate = oc -> when(oc.isSupported()).thenReturn(true);;
    // When
    final KubernetesClient result = new ClusterAccess(logger, null).createDefaultClient();
    // Then
    assertNotNull(result);
    assertTrue(result instanceof OpenShiftClient);
  }
}
