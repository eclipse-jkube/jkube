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
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.server.forward.AcceptAllForwardingFilter;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import java.net.ServerSocket;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemoteDevelopmentService {

  static final int CONTAINER_SSH_PORT = 2222;
  static final String SSH_SERVER_APP = "ssh-server";
  static final String SSH_SERVER_GROUP = "jkube-kit";
  private final JKubeServiceHub jKubeServiceHub;
  private final RemoteDevelopmentConfig remoteDevelopmentConfig;
  private Pod sshService;
  private ExecutorService executorService;
  private SshClient sshClient;
  private KubernetesSshServiceForwarder kubernetesSshServiceForwarder;
  private PortForwarder portForwarder;

  public RemoteDevelopmentService(JKubeServiceHub jKubeServiceHub,
    RemoteDevelopmentConfig remoteDevelopmentConfig // Should be provided by JKubeServiceHub
  ) {
    this.jKubeServiceHub = jKubeServiceHub;
    this.remoteDevelopmentConfig = remoteDevelopmentConfig;
  }

  public void start() {
    Objects.requireNonNull(remoteDevelopmentConfig, "remoteDevelopmentConfig is required");
    Objects.requireNonNull(remoteDevelopmentConfig.getUser(), "user is required");
    Objects.requireNonNull(remoteDevelopmentConfig.getPassword(), "password is required");
    Objects.requireNonNull(remoteDevelopmentConfig.getSshPort(), "localService is required");
    checkEnvironment();
    sshService = deploySshServerPod();
    startSshClient();
    kubernetesSshServiceForwarder = new KubernetesSshServiceForwarder(
      jKubeServiceHub, remoteDevelopmentConfig, sshService);
    portForwarder = new PortForwarder(jKubeServiceHub, sshClient, remoteDevelopmentConfig);
    executorService = Executors.newFixedThreadPool(2);
    executorService.submit(kubernetesSshServiceForwarder);
    executorService.submit(portForwarder);
  }

  public void stop() {
    portForwarder.stop();
    kubernetesSshServiceForwarder.stop();
    executorService.shutdownNow();
    sshClient.stop();
    jKubeServiceHub.getClient().resource(sshService).delete();
  }

  private void checkEnvironment() {
    for (RemotePort port : remoteDevelopmentConfig.getRemotePorts()) {
      try (ServerSocket socket = new ServerSocket(port.getPort())) {
        jKubeServiceHub.getLog().debug("Local port '%s' for remote service '%s' is available",
          port.getPort(), port.getHostname());
      } catch (Exception e) {
        throw new IllegalStateException(
          "Local port '" + port.getPort() + "' is already in use (" + port.getHostname() + ")");
      }
    }
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
      .addToEnv(new EnvVarBuilder().withName("DOCKER_MODS")
        .withValue("linuxserver/mods:openssh-server-ssh-tunnel").build())
      .addToEnv(new EnvVarBuilder().withName("PUID").withValue("1000").build())
      .addToEnv(new EnvVarBuilder().withName("PGID").withValue("1000").build())
      .addToEnv(new EnvVarBuilder().withName("PASSWORD_ACCESS").withValue("true").build())
      .addToEnv(new EnvVarBuilder().withName("USER_NAME")
        .withValue(remoteDevelopmentConfig.getUser()).build())
      .addToEnv(new EnvVarBuilder().withName("USER_PASSWORD")
        .withValue(remoteDevelopmentConfig.getPassword()).build())
      .withImage("linuxserver/openssh-server:latest")
      .addNewPort().withContainerPort(CONTAINER_SSH_PORT).withProtocol("TCP").endPort()
      .endContainer()
      .endSpec();
    // This is just informational, maybe it's not worth adding them
    for (LocalService localService : remoteDevelopmentConfig.getLocalServices()) {
      pod.editSpec().editFirstContainer()
        .addNewPort().withContainerPort(localService.getPort()).withProtocol("TCP").endPort()
        .endContainer().endSpec();
    }
    return jKubeServiceHub.getClient().pods().withName(name)
      .patch(PatchContext.of(PatchType.SERVER_SIDE_APPLY), pod.build());
  }

  private void startSshClient() {
    sshClient = SshClient.setUpDefaultClient();
    sshClient.setForwardingFilter(AcceptAllForwardingFilter.INSTANCE);
    sshClient.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
    sshClient.start();
  }

}
