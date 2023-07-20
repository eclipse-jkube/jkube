/*
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

import java.util.concurrent.CountDownLatch;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PortForwardMonitor implements Watcher<Pod> {
  private final KitLogger logger;
  private final String podName;
  private final CountDownLatch podAvailableLatch;

  @Override
  public void eventReceived(Action action, Pod pod) {
    if (!podName.equals(pod.getMetadata().getName())) {
      return;
    }
    if (action == Action.DELETED || pod.getMetadata().getDeletionTimestamp() != null) {
      logger.error("Pod %s no longer available", podName);
      podAvailableLatch.countDown();
    }
    if (!KubernetesHelper.isPodRunning(pod)) {
      logger.error("Pod %s no longer in Running state", podName);
      podAvailableLatch.countDown();
    }
  }

  @Override
  public void onClose() {
    podAvailableLatch.countDown();
  }

  @Override
  public void onClose(WatcherException cause) {
    logger.error("Error in getting Debug Pod details from Kubernetes API: %s", cause.getMessage());
    podAvailableLatch.countDown();
  }
}

