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

import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
import org.eclipse.jkube.kit.common.KitLogger;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.eclipse.jkube.kit.remotedev.RemoteDevelopmentService.CONTAINER_SSH_PORT;
import static org.eclipse.jkube.kit.remotedev.RemoteDevelopmentService.SSH_SERVER_APP;
import static org.eclipse.jkube.kit.remotedev.RemoteDevelopmentService.SSH_SERVER_GROUP;

class KubernetesSshServiceForwarder implements Callable<Void> {

  private final KitLogger logger;
  private final KubernetesClient kubernetesClient;
  private final RemoteDevelopmentContext context;
  private final AtomicBoolean stop;
  private Pod sshService;

  KubernetesSshServiceForwarder(RemoteDevelopmentContext context) {
    this.logger = context.getjKubeServiceHub().getLog();
    this.kubernetesClient = context.getjKubeServiceHub().getClient();
    this.context = context;
    stop = new AtomicBoolean(false);
  }

  @Override
  public Void call() throws IOException, InterruptedException {
    final InetAddress allInterfaces = InetAddress.getByName("0.0.0.0");
    while (true) {
      if (sshService == null || kubernetesClient.pods().resource(sshService).fromServer().get() == null) {
        context.reset();
        sshService = deploySshServerPod();
      }
      logger.info("Waiting for Pod [%s] to be ready...", sshService.getMetadata().getName());
      kubernetesClient.pods().resource(sshService).waitUntilReady(10, TimeUnit.SECONDS);
      logger.info("Pod [%s] is ready", sshService.getMetadata().getName());
      context.setUser(waitForUser());
      logger.info("Opening remote-development connection to Kubernetes: %s:%s%n",
        sshService.getMetadata().getName(), context.getSshPort());
      try (LocalPortForward localPortForward = kubernetesClient.pods().resource(sshService)
        .portForward(CONTAINER_SSH_PORT, allInterfaces, context.getSshPort())) {
        while (!shouldRestart(sshService, localPortForward)) {
          TimeUnit.SECONDS.sleep(1);
          if (stop.get()) {
            return null;
          }
        }
      }
    }
  }

  final void stop() {
    if (sshService != null) {
      logger.info("Removing Pod [%s]...", sshService.getMetadata().getName());
      kubernetesClient.pods().withName(sshService.getMetadata().getName()).delete();
    }
    stop.set(true);
  }

  private Pod deploySshServerPod() {
    final String name = "openssh-server-" + UUID.randomUUID();
    final PodBuilder pod = new PodBuilder()
      .withNewMetadata()
      .withName(name)
      .addToLabels("app", SSH_SERVER_APP)
      .addToLabels("group", SSH_SERVER_GROUP)
      .endMetadata()
      .withNewSpec()
      .addNewContainer()
      .withName("openssh-server")
      .addToEnv(new EnvVarBuilder().withName("PUBLIC_KEY").withValue(context.getSshRsaPublicKey()).build())
      .withImage("marcnuri/openssh-server:latest")
      .addNewPort().withContainerPort(CONTAINER_SSH_PORT).withProtocol("TCP").endPort()
      .endContainer()
      .endSpec();
    // This is just informational, maybe it's not worth adding them
    for (LocalService localService : context.getRemoteDevelopmentConfig().getLocalServices()) {
      pod.editSpec().editFirstContainer()
        .addNewPort().withContainerPort(localService.getPort()).withProtocol("TCP").endPort()
        .endContainer().endSpec();
    }
    return kubernetesClient.pods().withName(name).patch(PatchContext.of(PatchType.SERVER_SIDE_APPLY), pod.build());
  }

  private String waitForUser() throws InterruptedException {
    logger.debug("Waiting for Pod to log current user");
    int retry = 0;
    String log;
    while (!(log = kubernetesClient.pods().resource(sshService).getLog()).contains("Current container user is:")) {
      if (retry++ > 60) {
        throw new IllegalStateException("Unable to retrieve current user from Pod");
      }
      TimeUnit.SECONDS.sleep(1);
    }
    int i = log.indexOf("Current container user is:");
    return log.substring(i + 26, log.indexOf("\n") + i).trim();
  }

  private boolean shouldRestart(Pod sshService, LocalPortForward localPortForward) {
    if (kubernetesClient.pods().resource(sshService).fromServer().get() == null) {
      logger.warn("Kubernetes tunneling service Pod is gone, recreating");
      return true;
    }
    if (localPortForward.errorOccurred()) {
      logger.warn("Kubernetes tunneling service error, restarting");
      return true;
    }
    if (!localPortForward.isAlive()) {
      logger.warn("Kubernetes tunneling service dead, restarting");
      return true;
    }
    return false;
  }
}
