///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.fabric8:kubernetes-client:6.1.1
//DEPS org.apache.sshd:sshd-core:2.9.1

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
import io.fabric8.kubernetes.client.extended.run.RunConfigBuilder;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.forward.PortForwardingEventListener;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.apache.sshd.server.forward.AcceptAllForwardingFilter;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

class RemoteDev {

  private static final String DEFAULT_USER = "remote-dev";
  private static final String APP_GROUP = "remote-dev";

  public static void main(String[] args) throws Exception {
    try (
      KubernetesClient client = new KubernetesClientBuilder().build();
      AutoCloseable ignore = postgresService(client);
      AutoCloseable ignore2 = mysqlService(client)
    ) {
      final Pod sshServer = sshServer(client);
      try (SshClient sshClient = SshClient.setUpDefaultClient()) {
        sshClient.setForwardingFilter(AcceptAllForwardingFilter.INSTANCE);
        sshClient.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
        sshClient.start();
        CompletableFuture.allOf(
          retryableKubernetesPortForward(client, sshServer, 2222),
          retryablePortForwarding(sshClient,
            Arrays.asList(new RemoteService("postgres", 5432), new RemoteService("mysql", 3306)),
            Collections.singletonList(new LocalService("spring-boot", 8080)))
        ).get();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      } catch (Exception ex) {
        ex.printStackTrace();
      } finally {
        client.pods().resource(sshServer).delete();
      }
    }
    System.exit(0);
  }
  private static CompletableFuture<Void> retryablePortForwarding(
    SshClient sshClient, List<RemoteService> remoteServices, List<LocalService> localServices) {
    return CompletableFuture.runAsync(() -> {
      while (true) {
        System.out.println("Opening SSH session");
        try (ClientSession session = sshClient
          .connect(DEFAULT_USER, "localhost", 2222)
          .verify(10, TimeUnit.SECONDS)
          .getSession()
        ) {
          session.addPasswordIdentity("password");
          session.auth().verify(10, TimeUnit.SECONDS);
          session.addPortForwardingEventListener(new PortForwardingEventListener() {
            @Override
            public void establishedExplicitTunnel(Session session, SshdSocketAddress local, SshdSocketAddress remote, boolean localForwarding, SshdSocketAddress boundAddress, Throwable reason) throws IOException {
              PortForwardingEventListener.super.establishedExplicitTunnel(session, local, remote, localForwarding, boundAddress, reason);
            }
          });
          for (RemoteService remoteService : remoteServices) {
            session.startLocalPortForwarding(remoteService.port,
              SshdSocketAddress.toSshdSocketAddress(new InetSocketAddress(remoteService.host, remoteService.port)));
            System.out.printf("SSH local port forward to %s:%s is ready%n", remoteService.host, remoteService.port);
          }
          for (LocalService localService : localServices) {
            session.startRemotePortForwarding(
              new SshdSocketAddress(localService.port),
              new SshdSocketAddress( localService.port));
            System.out.printf("SSH remote port forward for port %s is ready%n",  localService.port);
          }
          session.waitFor(
            Arrays.asList(ClientSession.ClientSessionEvent.CLOSED, ClientSession.ClientSessionEvent.TIMEOUT),
            Duration.ofHours(1));
        } catch (Exception e) {
          System.out.printf("SSH Session disconnected, retrying: %s%n", e.getMessage());
        }
        try {
          TimeUnit.SECONDS.sleep(5);
        } catch(InterruptedException ex) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    });
  }

  private static CompletableFuture<Void> retryableKubernetesPortForward(KubernetesClient client, Pod pod, int port)
    throws UnknownHostException, InterruptedException {
    System.out.printf("Waiting for Pod [%s] to be ready...%n", pod.getMetadata().getName());
    client.pods().resource(pod).waitUntilReady(10, TimeUnit.SECONDS);
    int retry = 0;
    while (client.pods().resource(pod).getLog().contains("[ls.io-init] done.") && retry < 10) {
      TimeUnit.SECONDS.sleep(1);
    }
    InetAddress allInterfaces = Inet4Address.getByName("0.0.0.0");
    return CompletableFuture.runAsync(() -> {
      while (true) {
        System.out.printf("Opening a connection to: %s%n", pod.getMetadata().getName());
        final LocalPortForward localPortForward = client.pods().resource(pod).portForward(port, allInterfaces, port);
        while (true) {
          try {
            if (localPortForward.errorOccurred()) {
              System.out.println("ERROR OCCURRED");
              break;
              // Not cleared after retrieval
  //                lpf.getServerThrowables().forEach(Throwable::printStackTrace);
  //                lpf.getClientThrowables().forEach(Throwable::printStackTrace);
            }
            if (!localPortForward.isAlive()) {
              System.out.println("LOCAL PORT FORWARD IS NOT ALIVE");
              break;
            }
            TimeUnit.SECONDS.sleep(1);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
          }
        }
      }
    });
  }


  /**
   * Exposing a remote cluster service via ssh
   * - ssh -p 2222 -L 0.0.0.0:8080:app:8080 remote-dev@localhost
   * - ssh -p 2222 -L 0.0.0.0:5432:postgres:5432 remote-dev@localhost
   * Exposing a local service to the cluster via ssh
   * ssh -p 2222 -R 8080:localhost:8080 remote-dev@localhost
   */
  private static Pod sshServer(KubernetesClient client) {
    client.pods().withName("openssh-server").delete();
    client.pods().withName("openssh-server").waitUntilCondition(Objects::isNull, 10, TimeUnit.SECONDS);
    return client.run()
      .withRunConfig(new RunConfigBuilder()
        .withName("openssh-server")
        .addToLabels("app", "openssh-server")
        .addToLabels("group", APP_GROUP)
        .addToEnv("DOCKER_MODS", "linuxserver/mods:openssh-server-ssh-tunnel")
        .addToEnv("PUID", "1000")
        .addToEnv("PGID", "1000")
        .addToEnv("PASSWORD_ACCESS", "true")
        .addToEnv("USER_NAME", DEFAULT_USER)
        .addToEnv("USER_PASSWORD", "password")
        .withImage("linuxserver/openssh-server:latest")
        .withPort(2222)
        .build())
      .done();
  }

  private static AutoCloseable postgresService(KubernetesClient client) {
    client.pods().withName("postgres").delete();
    client.pods().withName("postgres").waitUntilCondition(Objects::isNull, 10, TimeUnit.SECONDS);
    client.run()
      .withRunConfig(new RunConfigBuilder()
        .withName("postgres")
        .addToLabels("app", "postgres")
        .addToLabels("group", APP_GROUP)
        .addToEnv("POSTGRES_USER", DEFAULT_USER)
        .addToEnv("POSTGRES_PASSWORD", "password")
        .withImage("postgres:14.2-alpine")
        .withPort(5432)
        .build())
      .done();
    client.services().withName("postgres")
      .patch(PatchContext.of(PatchType.SERVER_SIDE_APPLY), service("postgres", 5432));
    return () -> {
      client.pods().withName("postgres").delete();
      client.services().withName("postgres").delete();
    };
  }

  private static AutoCloseable mysqlService(KubernetesClient client) {
    client.pods().withName("mysql").delete();
    client.pods().withName("mysql").waitUntilCondition(Objects::isNull, 10, TimeUnit.SECONDS);
    client.run()
      .withRunConfig(new RunConfigBuilder()
        .withName("mysql")
        .addToLabels("app", "mysql")
        .addToLabels("group", APP_GROUP)
        .addToEnv("MYSQL_USER", DEFAULT_USER)
        .addToEnv("MYSQL_PASSWORD", "password")
        .addToEnv("MYSQL_RANDOM_ROOT_PASSWORD", "true")
        .withImage("mysql:latest")
        .withPort(3306)
        .build())
      .done();
    client.services().withName("mysql")
      .patch(PatchContext.of(PatchType.SERVER_SIDE_APPLY), service("mysql", 3306));
    return () -> {
      client.pods().withName("name").delete();
      client.services().withName("name").delete();
    };
  }

  private static Service service(String name, int port) {
    return new ServiceBuilder()
      .withNewMetadata()
      .withName(name)
      .addToLabels("app", name)
      .addToLabels("group", APP_GROUP)
      .endMetadata()
      .withNewSpec()
      .withType("ClusterIP")
      .addToSelector("app", name)
      .addToSelector("group", APP_GROUP)
      .addNewPort()
      .withPort(port)
      .withTargetPort(new IntOrString(port))
      .endPort()
      .endSpec()
      .build();
  }

  private static final class LocalService {
    String name;
    int port;

    public LocalService(String name, int port) {
      this.name = name;
      this.port = port;
    }
  }

  private static final class RemoteService {
    String host;
    int port;

    public RemoteService(String host, int port) {
      this.host = host;
      this.port = port;
    }
  }
}

