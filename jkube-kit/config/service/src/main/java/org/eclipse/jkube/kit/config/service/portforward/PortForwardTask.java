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

import io.fabric8.kubernetes.client.LocalPortForward;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import lombok.AllArgsConstructor;
import org.eclipse.jkube.kit.common.KitLogger;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@AllArgsConstructor
public class PortForwardTask implements Runnable, AutoCloseable {
  private final NamespacedKubernetesClient kubernetesClient;
  private final String podName;
  private final LocalPortForward localPortForward;
  private final KitLogger logger;
  private final CountDownLatch podAvailableLatch = new CountDownLatch(1);
  private final AtomicBoolean closed = new AtomicBoolean(false);

  @Override
  public void run() {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      if (!closed.get()) {
        logger.info("Shutting down");
        close();
      }
    }));
    try(
        Watch ignore = kubernetesClient.pods()
            .watch(new PortForwardMonitor(logger, podName, podAvailableLatch))
    ) {
      podAvailableLatch.await();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    } catch (Exception exception) {
      logger.warn("Not able to port forward: %s", exception.getMessage());
    } finally {
      close();
    }
  }

  @Override
  public void close() {
    try {
      logger.info("Closing port forward for Debug Session ...");
      localPortForward.close();
      closed.set(true);
    } catch (IOException exception) {
      logger.warn("Not able to close Port forward gracefully : %s", exception.getMessage());
    }
  }
}
