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

import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.jkube.kit.common.KitLogger;

import java.net.ServerSocket;
import java.util.concurrent.CompletableFuture;

import static org.eclipse.jkube.kit.common.util.AsyncUtil.async;

public class RemoteDevelopmentService {

  static final int CONTAINER_SSH_PORT = 2222;
  static final String REMOTE_DEVELOPMENT_APP = "jkube-remote-dev";
  static final String REMOTE_DEVELOPMENT_GROUP = "jkube-kit";
  private final RemoteDevelopmentContext context;
  private final KitLogger logger;
  private final KubernetesClient kubernetesClient;
  private final KubernetesSshServiceForwarder kubernetesSshServiceForwarder;
  private final PortForwarder portForwarder;
  private final LocalServiceManager localServiceManager;

  public RemoteDevelopmentService(KitLogger logger, KubernetesClient kubernetesClient,
    RemoteDevelopmentConfig remoteDevelopmentConfig
  ) { // Should be provided by JKubeServiceHub (TODO: Create SPI)
    this.context = new RemoteDevelopmentContext(logger, kubernetesClient, remoteDevelopmentConfig);
    this.logger = logger;
    this.kubernetesClient = kubernetesClient;
    kubernetesSshServiceForwarder = new KubernetesSshServiceForwarder(context);
    portForwarder = new PortForwarder(context);
    localServiceManager = new LocalServiceManager(context);
  }

  public CompletableFuture<Void> start() {
    checkEnvironment();
    localServiceManager.createOrReplaceServices();
    return CompletableFuture.anyOf(
      async(kubernetesSshServiceForwarder),
      async(portForwarder)
    ).thenApply(object -> null);
  }

  public void stop() {
    logger.info("Stopping remote development service...");
    localServiceManager.tearDownServices();
    portForwarder.stop();
    kubernetesSshServiceForwarder.stop();
    logger.info("Remote development service stopped");
  }

  private void checkEnvironment() {
    for (RemoteService remoteService : context.getRemoteDevelopmentConfig().getRemoteServices()) {
      try (ServerSocket ignore = new ServerSocket(remoteService.getPort())) {
        logger.debug("Local port '%s' for remote service '%s:%s' is available",
          remoteService.getLocalPort(), remoteService.getHostname(), remoteService.getPort());
      } catch (Exception e) {
        throw new IllegalStateException(
          "Local port '" + remoteService.getPort() + "' is already in use (" + remoteService.getHostname() + ")");
      }
      if (kubernetesClient.services().withName(remoteService.getHostname()).get() == null) {
        logger.warn("Service '%s' does not exist in the cluster, " +
          "you won't be able to access it until it is created", remoteService.getHostname());
      }
    }
  }

}
