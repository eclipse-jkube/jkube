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

import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.Test;

public class PortForwardServicePortOrderTest {
    @Mocked
    private NamespacedKubernetesClient kubernetesClient;

    @Test
    public void testPortsSpecifiedInCorrectOrderPortForward() {
        // When
        PortForwardService.forwardPortAsync(kubernetesClient, "foo-pod", 8080, 312323);

        // Then
        new Verifications() {{
            kubernetesClient.pods().withName("foo-pod").portForward(8080, 312323);
            times = 1;
        }};
    }
}
