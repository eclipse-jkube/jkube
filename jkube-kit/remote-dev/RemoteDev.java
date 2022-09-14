///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.fabric8:kubernetes-client:6.1.1

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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

class RemoteDev {

  private static final String APP_GROUP = "remote-dev";

  public static void main(String[] args) throws Exception {
    try (
      KubernetesClient client = new KubernetesClientBuilder().build();
      AutoCloseable ignore = postgresService(client);
      AutoCloseable ignore2 = mysqlService(client)
    ) {
      final Pod sshServer = sshServer(client);
      try {
        clusterToLocal(client, sshServer, 2222).get();
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

  private static CompletableFuture<LocalPortForward> clusterToLocal(KubernetesClient client, Pod pod, int port) throws UnknownHostException {
    System.out.println("Opening a connection to: " + pod.getMetadata().getName());
    final InetAddress allInterfaces = Inet4Address.getByName("0.0.0.0");
    return CompletableFuture.supplyAsync(() -> {
      final LocalPortForward lpf = client.pods().resource(pod)
        .portForward(port, allInterfaces, port);
      while (true) {
        try {
          if (lpf.errorOccurred()) {
            System.out.println("ERROR OCCURRED");
            // Not cleared after retrieval
//                lpf.getServerThrowables().forEach(Throwable::printStackTrace);
//                lpf.getClientThrowables().forEach(Throwable::printStackTrace);
          }
          if (!lpf.isAlive()) {
            throw new RuntimeException("Not alive");
          }
          TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return lpf;
        }
      }
    });
  }


  /**
   * Exposing a remote cluster service via ssh
   * - ssh -p 2222 -L 0.0.0.0:8080:app:8080 remote-dev@localhost
   * - ssh -p 2222 -L 0.0.0.0:5432:postgres:5432 remote-dev@localhost
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
        .addToEnv("USER_NAME", "remote-dev")
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
        .addToEnv("POSTGRES_USER", "remote-dev")
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
        .addToEnv("MYSQL_USER", "remote-dev")
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
}

