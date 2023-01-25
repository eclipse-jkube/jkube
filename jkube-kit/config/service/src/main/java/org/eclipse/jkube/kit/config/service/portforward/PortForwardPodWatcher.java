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

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jkube.kit.common.KitLogger;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;

import static org.eclipse.jkube.kit.common.util.KubernetesHelper.getName;
import static org.eclipse.jkube.kit.common.util.KubernetesHelper.isPodReady;
import static org.eclipse.jkube.kit.common.util.KubernetesHelper.isPodRunning;
import static org.eclipse.jkube.kit.common.util.PodHelper.firstContainerHasEnvVars;
import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.getPodStatusDescription;
import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.getPodStatusMessagePostfix;

public class PortForwardPodWatcher implements Watcher<Pod> {
  private final KitLogger log;
  private final Map<String, String> envVars;
  private final CountDownLatch podReadyLatch;
  private final AtomicReference<Pod> foundPod;

  public PortForwardPodWatcher(KitLogger log, Map<String, String> envVars) {
    this.log = log;
    this.envVars = envVars;
    this.podReadyLatch = new CountDownLatch(1);
    this.foundPod = new AtomicReference<>(null);
  }

  @Override
  public void eventReceived(Action action, Pod pod) {
    log.info(getName(pod) + " status: " + getPodStatusDescription(pod) + getPodStatusMessagePostfix(action));
    if (isAddOrModified(action) && isPodRunning(pod) && isPodReady(pod) && firstContainerHasEnvVars(pod, envVars)) {
      log.info("Debug Pod ready : %s", pod.getMetadata().getName());
      foundPod.set(pod);
      podReadyLatch.countDown();
    }
  }

  @Override
  public void onClose(WatcherException e) {
    // Ignore
  }

  private boolean isAddOrModified(Watcher.Action action) {
    return action.equals(Watcher.Action.ADDED) || action.equals(Watcher.Action.MODIFIED);
  }

  public Pod getFoundPod() {
    return this.foundPod.get();
  }

  public CountDownLatch getPodReadyLatch() {
    return podReadyLatch;
  }
}
