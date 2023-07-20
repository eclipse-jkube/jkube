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

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.io.IOUtils;
import org.apache.sshd.common.kex.KexProposalOption;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.SessionListener;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.TestHttpStaticServer;
import org.eclipse.jkube.kit.common.util.IoUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.remotedev.SshServerExtension.AUTHORIZED_USER;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class PortForwarderTest implements Supplier<RemoteDevelopmentContext> {

  @RegisterExtension
  private final SshServerExtension sshServer = new SshServerExtension(this);
  private PortForwarder portForwarder;
  private ExecutorService executorService;
  private RemoteDevelopmentContext context;

  @BeforeEach
  void setUp() {
    executorService = Executors.newSingleThreadExecutor();
  }

  @AfterEach
  void tearDown() {
    portForwarder.stop();
    executorService.shutdownNow();
  }

  @Override
  public RemoteDevelopmentContext get() {
    return context;
  }

  @Nested
  @DisplayName("With no Ports declared")
  class WithNoPortsDeclared {

    @BeforeEach
    void setContext() {
      context = new RemoteDevelopmentContext(
        spy(new KitLogger.SilentLogger()), mock(KubernetesClient.class), RemoteDevelopmentConfig.builder()
        .build());
      portForwarder = new PortForwarder(context);
    }

    @Test
    void validUserAuthenticatesInServer() throws Exception {
      // Given
      context.setUser(AUTHORIZED_USER);
      final CompletableFuture<Boolean> sessionOpened = new CompletableFuture<>();
      sshServer.getSshServer().addSessionListener(new SessionListener() {
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
      sshServer.getSshServer().addSessionListener(new SessionListener() {
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

    @Test
    void doesntStartSocksProxy() {
      // Given
      context.setUser(AUTHORIZED_USER);
      // When
      executorService.submit(new PortForwarder(context));
      // Then
      verify(context.getLogger(), timeout(10000).times(1))
        .debug("SOCKS 5 proxy is disabled");
    }

  }

  @Nested
  @DisplayName("With Ports declared")
  class WithPortsDeclared {

    @BeforeEach
    void setContext() {
      context = new RemoteDevelopmentContext(
        spy(new KitLogger.SilentLogger()), mock(KubernetesClient.class), RemoteDevelopmentConfig.builder()
        .socksPort(IoUtil.getFreeRandomPort())
        .remoteService(RemoteService.builder()
          .hostname("localhost")
          .port(IoUtil.getFreeRandomPort())
          .localPort(IoUtil.getFreeRandomPort()).build())
        .localService(LocalService.builder()
          .serviceName("test-service")
          .port(IoUtil.getFreeRandomPort()).build())
        .build());
      context.getManagedServices()
        .put(context.getRemoteDevelopmentConfig().getLocalServices().iterator().next(), new Service());
      portForwarder = new PortForwarder(context);
    }

    @Test
    void startsSocksProxy() throws Exception {
      // Given
      final int socksPort = context.getRemoteDevelopmentConfig().getSocksPort();
      context.setUser(AUTHORIZED_USER);
      // When
      executorService.submit(portForwarder);
      // Then
      try (TestHttpStaticServer server = new TestHttpStaticServer(new File("."))) {
        // Then
        verify(context.getLogger(), timeout(10000).times(1))
          .info("SOCKS 5 proxy is now available at 'localhost:%s'", socksPort);
        final InputStream is = new URL("http://localhost:" + server.getPort() + "/health")
          .openConnection(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("localhost", socksPort)))
          .getInputStream();
        assertThat(IOUtils.toString(is, StandardCharsets.UTF_8))
          .isEqualTo("READY");
      }
    }

    @Test
    void waitsForManagedServices() {
      // Given
      context.getManagedServices().clear();
      context.setUser(AUTHORIZED_USER);
      // When
      executorService.submit(portForwarder);
      // Then
      verify(context.getLogger(), timeout(10000).times(1))
        .debug("Waiting for remote services to be created");
    }

    @Test
    void forwardsLocalPorts() {
      // Given
      final LocalService localService = context.getRemoteDevelopmentConfig().getLocalServices().get(0);
      context.setUser(AUTHORIZED_USER);
      // When
      executorService.submit(portForwarder);
      // Then
      verify(context.getLogger(), timeout(10000).times(1))
        .info("Local port '%s' is now available as a Kubernetes Service at %s:%s",
          localService.getPort(), "test-service", localService.getPort());
    }

    @Test
    void forwardsLocalPortsInferringRemotePort() {
      // Given
      final LocalService localService = context.getRemoteDevelopmentConfig().getLocalServices().get(0);
      context.getManagedServices().put(localService, new ServiceBuilder()
          .withNewSpec()
          .addNewPort().withTargetPort(new IntOrString(1337)).endPort()
        .endSpec().build());
      context.setUser(AUTHORIZED_USER);
      // When
      executorService.submit(portForwarder);
      // Then
      verify(context.getLogger(), timeout(10000).times(1))
        .info("Local port '%s' is now available as a Kubernetes Service at %s:%s",
          localService.getPort(), "test-service", 1337);
    }

    @Test
    void forwardsRemotePorts() {
      // Given
      final int remotePort = context.getRemoteDevelopmentConfig().getRemoteServices().iterator().next().getPort();
      final int localPort = context.getRemoteDevelopmentConfig().getRemoteServices().iterator().next().getLocalPort();
      context.setUser(AUTHORIZED_USER);
      // When
      executorService.submit(portForwarder);
      // Then
      verify(context.getLogger(), timeout(10000).times(1))
        .info("Kubernetes Service %s:%s is now available at local port %s",
          "localhost", remotePort, localPort);
    }
  }

}
