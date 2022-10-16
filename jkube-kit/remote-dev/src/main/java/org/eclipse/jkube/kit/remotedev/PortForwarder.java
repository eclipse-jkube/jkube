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
import org.apache.sshd.client.auth.pubkey.UserAuthPublicKeyFactory;
import org.apache.sshd.client.config.hosts.HostConfigEntryResolver;
import org.apache.sshd.client.global.OpenSshHostKeysHandler;
import org.apache.sshd.client.keyverifier.StaticServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.channel.RequestHandler;
import org.apache.sshd.common.io.BuiltinIoServiceFactoryFactories;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;
import org.apache.sshd.common.session.ConnectionService;
import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.server.forward.AcceptAllForwardingFilter;
import org.eclipse.jkube.kit.common.KitLogger;

import java.io.IOException;
import java.net.SocketAddress;
import java.security.PublicKey;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class PortForwarder implements Callable<Void> {

  private final RemoteDevelopmentContext context;
  private final KitLogger logger;
  private final AtomicBoolean stop;

  PortForwarder(RemoteDevelopmentContext context) {
    this.context = context;
    this.logger = context.getjKubeServiceHub().getLog();
    stop = new AtomicBoolean(false);
  }

  @Override
  public Void call() throws InterruptedException {
    while (true) {
      waitForUser();
      final SshClient sshClient = startSshClient();
      try (ClientSession session = createSession(sshClient)) {
        session.auth().verify(10, TimeUnit.SECONDS);
        forwardRemotePorts(session);
        forwardLocalPorts(session);
        session.waitFor(
          Arrays.asList(ClientSession.ClientSessionEvent.CLOSED, ClientSession.ClientSessionEvent.TIMEOUT),
          Duration.ofHours(1));
      } catch (Exception ex) {
        logger.warn("SSH session disconnected, retrying in 5 seconds: %s", ex.getMessage());
      }
      if (stop.get()) {
        sshClient.stop();
        return null;
      }
      TimeUnit.SECONDS.sleep(5);
    }
  }

  void stop() {
    stop.set(true);
  }

  private SshClient startSshClient() {
    // Use only BouncyCastle as security provider (EdDSA requires additional dependencies)
    SecurityUtils.setAPrioriDisabledProvider(SecurityUtils.EDDSA, true);
    final SshClient sshClient = SshClient.setUpDefaultClient();
    // Limit the authentication methods to public key
    sshClient.setUserAuthFactories(Collections.singletonList(UserAuthPublicKeyFactory.INSTANCE));
    // Provide the default (prevents log showing that the default -NIO2- is used)
    sshClient.setIoServiceFactoryFactory(BuiltinIoServiceFactoryFactories.NIO2.create());
    // Prevent the usage of ~/.ssh/config file
    sshClient.setHostConfigEntryResolver(HostConfigEntryResolver.EMPTY);
    // Prevent log messages from the default OpenSshHostKeysHandler handler
    sshClient.setGlobalRequestHandlers(Collections.singletonList(new GlobalRequestHandler(logger)));
    sshClient.setForwardingFilter(AcceptAllForwardingFilter.INSTANCE);
    sshClient.setServerKeyVerifier(AcceptAllNoLoggingServerKeyVerifier.INSTANCE);
    sshClient.setKeyIdentityProvider(KeyIdentityProvider.wrapKeyPairs(context.getClientKeys()));
    sshClient.start();
    return sshClient;
  }

  private ClientSession createSession(SshClient sshClient) throws IOException {
    return sshClient
      .connect(context.getUser(), "localhost", context.getSshPort())
      .verify(10, TimeUnit.SECONDS)
      .getSession();
  }

  private void waitForUser() throws InterruptedException {
    logger.debug("Waiting for remote container to log current user");
    while (context.getUser() == null) {
      TimeUnit.SECONDS.sleep(1);
    }
  }

  private void forwardRemotePorts(ClientSession session) throws IOException {
    for (RemotePort remotePort : context.getRemoteDevelopmentConfig().getRemotePorts()) {
      session.startLocalPortForwarding(
        remotePort.getPort(), new SshdSocketAddress(remotePort.getHostname(), remotePort.getPort()));
      logger.info("Kubernetes Service %s:%s is now available at local port %s",
        remotePort.getHostname(), remotePort.getPort(), remotePort.getPort());
    }
  }

  private void forwardLocalPorts(ClientSession session) throws IOException {
    for (LocalService localService : context.getRemoteDevelopmentConfig().getLocalServices()) {
      session.startRemotePortForwarding(
        new SshdSocketAddress("", localService.getPort()),
        new SshdSocketAddress("localhost", localService.getPort()) // Extremely important for quarkus:dev
      );
      logger.info("Local port '%s' is now available at Kubernetes %s:%s",
        localService.getPort(), localService.getServiceName(), localService.getPort());
    }
  }

  /**
   * Ignores requests for "hostkeys-00@openssh.com".
   * Prevents unsupported EdDSA key messages to be logged.
   */
  private static final class GlobalRequestHandler implements RequestHandler<ConnectionService> {

    private final KitLogger logger;

    public GlobalRequestHandler(KitLogger logger) {
      this.logger = logger;
    }

    @Override
    public Result process(ConnectionService o, String request, boolean wantReply, Buffer buffer) {
      if (!request.equals(OpenSshHostKeysHandler.REQUEST)) {
        logger.warn("Received unknown global request: %s", request);
      }
      return RequestHandler.Result.ReplyFailure;
    }
  }

  private static final class AcceptAllNoLoggingServerKeyVerifier extends StaticServerKeyVerifier {
    private static final AcceptAllNoLoggingServerKeyVerifier INSTANCE = new AcceptAllNoLoggingServerKeyVerifier();

    public AcceptAllNoLoggingServerKeyVerifier() {
      super(true);
    }
    protected void handleAcceptance(ClientSession sshClientSession, SocketAddress remoteAddress, PublicKey serverKey) {
      // NO OP
    }
  }
}
