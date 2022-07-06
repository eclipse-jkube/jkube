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

<<<<<<< HEAD
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PortForwardServicePortOrderTest {
  private NamespacedKubernetesClient kubernetesClient;
=======
import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.jkube.kit.common.KitLogger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
@RunWith(MockitoJUnitRunner.class)
public class PortForwardServicePortOrderTest {
    @Mock
    private KubernetesClient kubernetesClient;
>>>>>>> 341499c2 (JMockit to Mockito)

  @Test
  void portsSpecifiedInCorrectOrderPortForward() {
    // When
    PortForwardService.forwardPortAsync(kubernetesClient, "foo-pod", 8080, 312323);

<<<<<<< HEAD
    // Then
    verify(kubernetesClient,times(1)).pods().withName("foo-pod").portForward(8080, 312323);
  }
}
=======
    @Test
    public void testPortsSpecifiedInCorrectOrderPortForward() {
        // Given
        PortForwardService portForwardService = new PortForwardService(kubernetesClient, logger);
        // When
        portForwardService.forwardPortAsync("foo-pod", "foo-ns", 8080, 312323);

        // Then
        verify(kubernetesClient,times(1)).pods().inNamespace("foo-ns").withName("foo-pod").portForward(8080, 312323);
    }
}
>>>>>>> 341499c2 (JMockit to Mockito)
