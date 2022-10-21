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
package org.eclipse.jkube.kit.config.service;

import io.fabric8.kubernetes.client.LocalPortForward;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PortForwardServicePortOrderTest {
  private NamespacedKubernetesClient kubernetesClient;

  @BeforeEach
  public void setUp() throws Exception{
    kubernetesClient = mock(NamespacedKubernetesClient.class);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Test
  void portsSpecifiedInCorrectOrderPortForward() {
    // Given
    MixedOperation mixedOperation = mock(MixedOperation.class);
    PodResource podResource = mock(PodResource.class);
    LocalPortForward localPortForward = mock(LocalPortForward.class);
    when(kubernetesClient.pods()).thenReturn(mixedOperation);
    when(mixedOperation.withName("foo-pod")).thenReturn(podResource);
    when(podResource.portForward(8080, 312323)).thenReturn(localPortForward);
    // When
    PortForwardService.forwardPortAsync(kubernetesClient, "foo-pod", 8080, 312323);
    // Then
    verify(kubernetesClient).pods();
    verify(mixedOperation).withName("foo-pod");
    verify(podResource).portForward(8080, 312323);
  }
}
