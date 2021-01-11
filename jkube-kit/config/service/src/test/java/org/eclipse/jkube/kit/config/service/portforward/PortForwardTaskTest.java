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
package org.eclipse.jkube.kit.config.service.portforward;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.eclipse.jkube.kit.common.KitLogger;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.LocalPortForward;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.Before;
import org.junit.Test;

public class PortForwardTaskTest {
  @Mocked
  private KubernetesClient kubernetesClient;
  @Mocked
  private LocalPortForward localPortForward;
  @Mocked
  private KitLogger logger;

  private PortForwardTask portForwardTask;

  @Before
  public void setUp() throws Exception {
    portForwardTask = new PortForwardTask(
        kubernetesClient, "pod-name", "namespace", localPortForward, logger);
  }

  @Test
  public void run(@Mocked CountDownLatch cdl) throws Exception {
    // When
    portForwardTask.run();
    // Then
    // @formatter:off
    new Verifications() {{
      cdl.await(); times = 1;
      localPortForward.close(); times = 1;
    }};
    // @formatter:on
  }

  @Test
  public void close() throws IOException {
    // When
    portForwardTask.close();
    // Then
    // @formatter:off
    new Verifications() {{
      localPortForward.close(); times = 1;
    }};
    // @formatter:on
  }
}