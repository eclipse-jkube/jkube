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
package org.eclipse.jkube.kit.remotedev;

import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.fabric8.kubernetes.client.dsl.NonDeletingOperation;
import org.eclipse.jkube.kit.common.KitLogger;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.eclipse.jkube.kit.common.util.AsyncUtil.await;
import static org.eclipse.jkube.kit.remotedev.RemoteDevelopmentService.LABEL_INSTANCE;
import static org.eclipse.jkube.kit.remotedev.RemoteDevelopmentService.LABEL_NAME;
import static org.eclipse.jkube.kit.remotedev.RemoteDevelopmentService.LABEL_PART_OF;
import static org.eclipse.jkube.kit.remotedev.RemoteDevelopmentService.REMOTE_DEVELOPMENT_APP;
import static org.eclipse.jkube.kit.remotedev.RemoteDevelopmentService.REMOTE_DEVELOPMENT_GROUP;

class KubernetesSshServiceForwarder implements Callable<Void> {

  private final KitLogger logger;
  private final KubernetesClient kubernetesClient;
  private final RemoteDevelopmentContext context;
  private final AtomicBoolean stop;
  private Pod sshService;

  KubernetesSshServiceForwarder(RemoteDevelopmentContext context) {
    this.logger = context.getLogger();
    this.kubernetesClient = context.getKubernetesClient();
    this.context = context;
    stop = new AtomicBoolean(false);
  }

  @Override
  public Void call() throws IOException, InterruptedException {
    logger.debug("Starting Kubernetes SSH service forwarder...");
    final InetAddress allInterfaces = InetAddress.getByName("0.0.0.0");
    while (!stop.get()) {
      if (sshService == null || kubernetesClient.pods().resource(sshService).get() == null) {
        context.reset();
        sshService = deploySshServerPod();
      }
      logger.info("Waiting for JKube remote development Pod [%s] to be ready...",
        sshService.getMetadata().getName());
      kubernetesClient.pods().resource(sshService).waitUntilReady(10, TimeUnit.SECONDS);
      logger.info("JKube remote development Pod [%s] is ready", sshService.getMetadata().getName());
      context.setUser(waitForUser());
      logger.info("Opening remote development connection to Kubernetes: %s:%s%n",
        sshService.getMetadata().getName(), context.getSshPort());
      try (LocalPortForward localPortForward = kubernetesClient.pods().resource(sshService)
        .portForward(context.getRemoteDevPodPort(), allInterfaces, context.getSshPort())) {
        while (!shouldRestart(sshService, localPortForward)) {
          TimeUnit.SECONDS.sleep(1);
          if (stop.get()) {
            return null;
          }
        }
      }
    }
    return null;
  }

  final void stop() {
    if (sshService != null) {
      logger.info("Removing JKube remote development Pod [%s]...", sshService.getMetadata().getName());
      kubernetesClient.pods().withName(sshService.getMetadata().getName()).delete();
    }
    stop.set(true);
  }

  private Pod deploySshServerPod() {
    final String name = "jkube-remote-dev-" + context.getSessionID();
    final PodBuilder pod = new PodBuilder()
      .withNewMetadata()
      .withName(name)
      .addToLabels(LABEL_INSTANCE, context.getSessionID().toString())
      .addToLabels(LABEL_NAME, REMOTE_DEVELOPMENT_APP)
      .addToLabels(LABEL_PART_OF, REMOTE_DEVELOPMENT_GROUP)
      .endMetadata()
      .withNewSpec()
      .addNewContainer()
      .withName(REMOTE_DEVELOPMENT_APP)
      .addToEnv(new EnvVarBuilder().withName("PUBLIC_KEY").withValue(context.getSshRsaPublicKey()).build())
      .withImage(context.getRemoteDevPodImage())
      .addNewPort().withContainerPort(context.getRemoteDevPodPort()).withProtocol("TCP").endPort()
      .endContainer()
      .endSpec();
    // This is just informational, maybe it's not worth adding them
    for (LocalService localService : context.getRemoteDevelopmentConfig().getLocalServices()) {
      pod.editSpec().editFirstContainer()
        .addNewPort().withContainerPort(localService.getPort()).withProtocol("TCP").endPort()
        .endContainer().endSpec();
    }
    return kubernetesClient.pods().resource(pod.build()).createOr(NonDeletingOperation::update);
      // Using createOrReplace instead of SSA because MockServer doesn't support this PATCH
      // unless the resource already exists
      // .patch(PatchContext.of(PatchType.SERVER_SIDE_APPLY), pod.build());
  }

  private String waitForUser() throws InterruptedException {
    logger.debug("Waiting for Pod to log current user");
    try {
      final String log = await(() -> kubernetesClient.pods().resource(sshService).getLog())
        .apply(l -> l.contains("Current container user is:"))
        .get(60, TimeUnit.SECONDS);
      int i = log.indexOf("Current container user is:");
      return log.substring(i + 26, log.indexOf("\n") + i).trim();
    } catch (ExecutionException | TimeoutException ex) {
      throw new IllegalStateException("Unable to retrieve current user from Pod", ex);
    }
  }

  private boolean shouldRestart(Pod sshService, LocalPortForward localPortForward) {
    if (kubernetesClient.pods().resource(sshService).get() == null) {
      logger.warn("JKube remote development Pod is gone, recreating");
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
