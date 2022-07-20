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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClusterAccessTest {

  private KitLogger logger;
  private MockedConstruction<DefaultKubernetesClient> kubernetesClientMockedConstruction;
  private Consumer<OpenShiftClient> onInstantiate;

  @BeforeEach
  void setUp() {
    logger = spy(new KitLogger.SilentLogger());
    onInstantiate = oc -> {
    };
    kubernetesClientMockedConstruction = mockConstruction(DefaultKubernetesClient.class, (mock, ctx) -> {
      final OpenShiftClient oc = mock(OpenShiftClient.class);
      when(mock.adapt(OpenShiftClient.class)).thenReturn(oc);
      onInstantiate.accept(oc);
    });
  }

  @AfterEach
  void tearDown() {
    kubernetesClientMockedConstruction.close();
  }

  @Test
  void isOpenShiftOpenShiftClusterShouldReturnTrue() {
    // Given
    onInstantiate = oc -> when(oc.isSupported()).thenReturn(true);
    // When
    final boolean result = new ClusterAccess(logger, null).isOpenShift();
    // Then
    assertThat(result).isTrue();
  }

  @Test
  void isOpenShiftKubernetesClusterShouldReturnFalse() {
    // Given
    onInstantiate = oc -> when(oc.isSupported()).thenReturn(false);
    // When
    final boolean result = new ClusterAccess(logger, null).isOpenShift();
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void isOpenShiftThrowsExceptionShouldReturnFalse() {
    // Given
    onInstantiate = oc -> when(oc.isSupported())
        .thenThrow(new KubernetesClientException("ERROR", new UnknownHostException()));
    // When
    final boolean result = new ClusterAccess(logger, null).isOpenShift();
    // Then
    assertThat(result).isFalse();
    verify(logger, times(1))
        .warn(startsWith("Cannot access cluster for detecting mode"), eq("Unknown host "), isNull());
  }

  @Test
  void createDefaultClientInKubernetesShouldReturnKubernetesClient() {
    // When
    final KubernetesClient result = new ClusterAccess(logger, null).createDefaultClient();
    // Then
    assertThat(result).isNotNull().isNotInstanceOf(OpenShiftClient.class);
  }

  @Test
  void createDefaultClientInOpenShiftShouldReturnOpenShiftClient() {
    // Given
    onInstantiate = oc -> when(oc.isSupported()).thenReturn(true);
    // When
    final KubernetesClient result = new ClusterAccess(logger, null).createDefaultClient();
    // Then
    assertThat(result).isNotNull().isInstanceOf(OpenShiftClient.class);
  }
}
