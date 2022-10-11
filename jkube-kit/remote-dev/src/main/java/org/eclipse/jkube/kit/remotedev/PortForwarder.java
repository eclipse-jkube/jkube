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

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class PortForwarder implements Callable<Void> {

  private final KitLogger logger;
  private final SshClient sshClient;
  private final RemoteDevelopmentConfig remoteDevelopmentConfig;
  private final AtomicBoolean stop;

  PortForwarder(
    JKubeServiceHub jKubeServiceHub, SshClient sshClient, RemoteDevelopmentConfig remoteDevelopmentConfig) {
    this.logger = jKubeServiceHub.getLog();
    this.sshClient = sshClient;
    this.remoteDevelopmentConfig = remoteDevelopmentConfig;
    stop = new AtomicBoolean(false);
  }

  @Override
  public Void call() throws InterruptedException {
    waitForUser();
    while (true) {
      try (ClientSession session = createSession()) {
        session.auth().verify(10, TimeUnit.SECONDS);
        for (RemotePort remotePort : remoteDevelopmentConfig.getRemotePorts()) {
          session.startLocalPortForwarding(
            remotePort.getPort(), new SshdSocketAddress(remotePort.getHostname(), remotePort.getPort()));
          logger.info("Kubernetes Service %s:%s is now available at local port %s",
            remotePort.getHostname(), remotePort.getPort(), remotePort.getPort());
        }
        for (LocalService localService : remoteDevelopmentConfig.getLocalServices()) {
          session.startRemotePortForwarding(
            new SshdSocketAddress("", localService.getPort()),
            new SshdSocketAddress("localhost", localService.getPort()) // Extremely important for quarkus:dev
          );
          logger.info("Local port '%s' is now available at Kubernetes %s:%s",
            localService.getPort(), localService.getServiceName(), localService.getPort());
        }
        session.waitFor(
          Arrays.asList(ClientSession.ClientSessionEvent.CLOSED, ClientSession.ClientSessionEvent.TIMEOUT),
          Duration.ofHours(1));
      } catch (Exception ex) {
        logger.warn("SSH session disconnected, retrying in 5 seconds: %s", ex.getMessage());
      }
      if (stop.get()) {
        return null;
      }
      TimeUnit.SECONDS.sleep(5);
    }
  }

  void stop() {
    stop.set(true);
  }

  private void waitForUser() throws InterruptedException {
    logger.debug("Waiting for remote container to log current user");
    while (remoteDevelopmentConfig.getUser() == null) {
      TimeUnit.SECONDS.sleep(1);
    }
  }

  private ClientSession createSession() throws IOException {
    return sshClient
      .connect(remoteDevelopmentConfig.getUser(), "localhost", remoteDevelopmentConfig.getSshPort())
      .verify(10, TimeUnit.SECONDS)
      .getSession();
  }
}
