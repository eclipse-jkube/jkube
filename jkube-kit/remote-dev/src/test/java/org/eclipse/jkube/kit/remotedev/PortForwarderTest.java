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
import org.apache.sshd.common.kex.KexProposalOption;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.SessionListener;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.config.keys.DefaultAuthorizedKeysAuthenticator;
import org.apache.sshd.server.forward.AcceptAllForwardingFilter;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.eclipse.jkube.kit.common.KitLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PortForwarderTest {

  private static final String AUTHORIZED_USER = "authorized-user";
  @TempDir
  private Path sshPath;
  private RemoteDevelopmentContext context;
  private PortForwarder portForwarder;
  private SshServer sshServer;

  private ExecutorService executorService;

  @BeforeEach
  void setUp() throws Exception {
    context = new RemoteDevelopmentContext(
      new KitLogger.SilentLogger(), mock(KubernetesClient.class), RemoteDevelopmentConfig.builder()
//      .remoteService(RemoteService.builder()
//        .hostname("www.google.com").port(80).localPort(IoUtil.getFreeRandomPort()).build())
      .build());
    portForwarder = new PortForwarder(context);
    executorService = Executors.newSingleThreadExecutor();
    // Server setup
    final Path authorizedKeys = Files.createFile(sshPath.resolve("authorized_keys"));
    Files.write(authorizedKeys, context.getSshRsaPublicKey().getBytes(StandardCharsets.UTF_8));
    sshServer = SshServer.setUpDefaultServer();
    sshServer.setPort(context.getSshPort());
    sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
    sshServer.setPublickeyAuthenticator(new DefaultAuthorizedKeysAuthenticator(
      AUTHORIZED_USER, authorizedKeys ,false));
    sshServer.setForwardingFilter(AcceptAllForwardingFilter.INSTANCE);
    sshServer.start();
  }

  @AfterEach
  void tearDown() throws Exception {
    sshServer.stop();
    portForwarder.stop();
    executorService.shutdownNow();
  }

  @Test
  void validUserAuthenticatesInServer() throws Exception {
    // Given
    context.setUser(AUTHORIZED_USER);
    final CompletableFuture<Boolean> sessionOpened = new CompletableFuture<>();
    sshServer.addSessionListener(new SessionListener() {
      @Override
      public void sessionEvent(Session session, Event event) {
        if (event == Event.Authenticated) {
          sessionOpened.complete(true);
        }
      }
    });
    // When
    executorService.submit(portForwarder);
    // Then
    assertThat(sessionOpened.get(10, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  void invalidUserIsDisconnectedFromServer() throws Exception {
    // Given
    context.setUser("invalid-user");
    final CompletableFuture<Boolean> sessionDisconnected = new CompletableFuture<>();
    sshServer.addSessionListener(new SessionListener() {
      @Override
      public void sessionNegotiationEnd(Session session, Map<KexProposalOption, String> clientProposal, Map<KexProposalOption, String> serverProposal, Map<KexProposalOption, String> negotiatedOptions, Throwable reason) {
        if (!session.isAuthenticated()) {
          sessionDisconnected.complete(true);
        }
      }

    });
    // When
    executorService.submit(portForwarder);
    // Then
    assertThat(sessionDisconnected.get(10, TimeUnit.SECONDS)).isTrue();
  }
}
