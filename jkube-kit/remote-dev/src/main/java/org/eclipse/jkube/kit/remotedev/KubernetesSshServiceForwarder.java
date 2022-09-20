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
package org.eclipse.jkube.kit.remotedev;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.LocalPortForward;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.eclipse.jkube.kit.remotedev.RemoteDevelopmentService.CONTAINER_SSH_PORT;

class KubernetesSshServiceForwarder implements Callable<Void> {

  private final KitLogger logger;
  private final KubernetesClient kubernetesClient;
  private final RemoteDevelopmentConfig remoteDevelopmentConfig;
  private final Pod sshService;
  private final AtomicBoolean stop;

  KubernetesSshServiceForwarder(
    JKubeServiceHub jKubeServiceHub, RemoteDevelopmentConfig remoteDevelopmentConfig, Pod sshService) {
    this.logger = jKubeServiceHub.getLog();
    this.kubernetesClient = jKubeServiceHub.getClient();
    this.remoteDevelopmentConfig = remoteDevelopmentConfig;
    this.sshService = sshService;
    stop = new AtomicBoolean(false);
  }

  @Override
  public Void call() throws UnknownHostException, InterruptedException {
    logger.info("Waiting for Pod [%s] to be ready...", sshService.getMetadata().getName());
    kubernetesClient.pods().resource(sshService).waitUntilReady(10, TimeUnit.SECONDS);
    int retry = 0;
    while (kubernetesClient.pods().resource(sshService).getLog().contains("[ls.io-init] done.") && retry < 10) {
      TimeUnit.SECONDS.sleep(1);
    }
    final InetAddress allInterfaces = InetAddress.getByName("0.0.0.0");
    while (true) {
      logger.info("Opening an SSH connection to: %s%n", sshService.getMetadata().getName());
      final LocalPortForward localPortForward = kubernetesClient.pods().resource(sshService)
        .portForward(CONTAINER_SSH_PORT, allInterfaces, remoteDevelopmentConfig.getSshPort());
      while (true) {
        boolean shouldRestart = false;
        if (localPortForward.errorOccurred()) {
          logger.warn("Kubernetes SSH forwarding service error, restarting");
          shouldRestart = true;
        }
        if (!localPortForward.isAlive()) {
          logger.warn("Kubernetes SSH forwarding service dead, restarting");
          shouldRestart = true;
        }
        if (shouldRestart) {
          break;
        }
        TimeUnit.SECONDS.sleep(1);
        if (stop.get()) {
          return null;
        }
      }
    }
  }

  void stop() {
    stop.set(true);
  }
}
