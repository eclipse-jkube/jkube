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
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import org.eclipse.jkube.kit.common.KitLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PortForwardServicePortOrderTest {
  private NamespacedKubernetesClient kubernetesClient;
    private KitLogger logger;

    @BeforeEach
    public void setUp() throws Exception{
        logger = new KitLogger.SilentLogger();
        kubernetesClient = mock(NamespacedKubernetesClient.class);
    }

  @Test
  void portsSpecifiedInCorrectOrderPortForward() {
    // When
    PortForwardService.forwardPortAsync(kubernetesClient, "foo-pod", 8080, 312323);

    // Then
    verify(kubernetesClient,times(1)).pods().withName("foo-pod").portForward(8080, 312323);
  }

    @Test
    void testPortsSpecifiedInCorrectOrderPortForward() {
        // Given
        MixedOperation mixedOperation = mock(MixedOperation.class);
        NonNamespaceOperation nonNamespaceOperation = mock(NonNamespaceOperation.class);
        PodResource podPodResource = mock(PodResource.class);
        LocalPortForward localPortForward = mock(LocalPortForward.class);

        when(kubernetesClient.pods()).thenReturn(mixedOperation);
        when(mixedOperation.inNamespace(any())).thenReturn(nonNamespaceOperation);
        when(nonNamespaceOperation.withName(any())).thenReturn(podPodResource);
        when(podPodResource.portForward(any(),any())).thenReturn(localPortForward);
        PortForwardService portForwardService = new PortForwardService(logger);
        // When
//        portForwardService.forwardPortAsync(kubernetesClient, "foo-pod", "foo-ns", 8080, 312323);
        // Then
        verify(kubernetesClient,times(1)).pods().inNamespace("foo-ns").withName("foo-pod").portForward(8080, 312323);
    }
}
