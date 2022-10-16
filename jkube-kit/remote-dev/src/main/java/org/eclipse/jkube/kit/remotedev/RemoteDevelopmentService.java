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
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import java.net.ServerSocket;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemoteDevelopmentService {

  static final int CONTAINER_SSH_PORT = 2222;
  static final String SSH_SERVER_APP = "ssh-server";
  static final String SSH_SERVER_GROUP = "jkube-kit";
  private final RemoteDevelopmentContext context;
  private final KitLogger logger;
  private final KubernetesClient kubernetesClient;
  private final KubernetesSshServiceForwarder kubernetesSshServiceForwarder;
  private final PortForwarder portForwarder;
  private final LocalServiceManager localServiceManager;
  private ExecutorService executorService;

  public RemoteDevelopmentService(JKubeServiceHub jKubeServiceHub,
    RemoteDevelopmentConfig remoteDevelopmentConfig // Should be provided by JKubeServiceHub
  ) {
    this.context = new RemoteDevelopmentContext(jKubeServiceHub, remoteDevelopmentConfig);
    logger = jKubeServiceHub.getLog();
    kubernetesClient = jKubeServiceHub.getClient();
    kubernetesSshServiceForwarder = new KubernetesSshServiceForwarder(context);
    portForwarder = new PortForwarder(context);
    localServiceManager = new LocalServiceManager(context);
  }

  public CompletableFuture<Void> start() {
    checkEnvironment();
    localServiceManager.createOrReplaceServices();
    executorService = Executors.newFixedThreadPool(2);
    return CompletableFuture.anyOf(
      async(kubernetesSshServiceForwarder, executorService),
      async(portForwarder, executorService)
    ).thenApply(object -> null);
  }

  public void stop() {
    logger.info("Stopping remote development service...");
    localServiceManager.tearDownServices();
    portForwarder.stop();
    kubernetesSshServiceForwarder.stop();
    Optional.ofNullable(executorService).ifPresent(ExecutorService::shutdownNow);
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

  private static <T> CompletableFuture<T> async(Callable<T> callable, Executor executor) {
    final CompletableFuture<T> future = new CompletableFuture<>();
    executor.execute(() -> {
      try {
        future.complete(callable.call());
      } catch (Exception ex) {
        future.completeExceptionally(ex);
      }
    });
    return future;
  }
}
