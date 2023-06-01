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

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.Serialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.spy;

@EnableKubernetesMockClient(crud = true)
class LocalServiceManagerTest {

  @SuppressWarnings("unused")
  private KubernetesMockServer mockServer;
  @SuppressWarnings("unused")
  private KubernetesClient kubernetesClient;
  private KitLogger logger;

  @BeforeEach
  void setUp() {
    mockServer.reset();
    logger = spy(new KitLogger.StdoutLogger());
  }

  @Test
  void createOrReplaceServices_createsNewService() {
    // Given
    final RemoteDevelopmentConfig config = RemoteDevelopmentConfig.builder()
      .localService(LocalService.builder().serviceName("service").type("NodePort").port(1337).build())
      .build();
    // When
    new LocalServiceManager(new RemoteDevelopmentContext(logger, kubernetesClient, config)).createOrReplaceServices();
    // Then
    assertThat(kubernetesClient.services().withName("service").get())
      .hasFieldOrPropertyWithValue("metadata.name", "service")
      .hasFieldOrPropertyWithValue("spec.type", "NodePort")
      .extracting(Service::getMetadata)
      .extracting(ObjectMeta::getAnnotations)
      .asInstanceOf(InstanceOfAssertFactories.MAP)
      .isNullOrEmpty();
  }

  @Test
  void createOrReplaceServices_usesJKubeSelectors() {
    // Given
    final RemoteDevelopmentConfig config = RemoteDevelopmentConfig.builder()
      .localService(LocalService.builder().serviceName("service").port(1337).build())
      .build();
    final RemoteDevelopmentContext context = new RemoteDevelopmentContext(logger, kubernetesClient, config);
    // When
    new LocalServiceManager(context).createOrReplaceServices();
    // Then
    assertThat(kubernetesClient.services().withName("service").get())
      .hasFieldOrPropertyWithValue("metadata.name", "service")
      .extracting(Service::getSpec)
      .extracting(ServiceSpec::getSelector)
      .asInstanceOf(InstanceOfAssertFactories.MAP)
      .containsEntry("app.kubernetes.io/name", "jkube-remote-dev")
      .containsEntry("app.kubernetes.io/part-of", "jkube-kit")
      .containsEntry("app.kubernetes.io/instance", context.getSessionID().toString());
  }

  @Test
  void createOrReplaceServices_withExistingService_replacesService() {
    // Given
    kubernetesClient.services().resource(new ServiceBuilder()
      .withNewMetadata().withName("service").addToAnnotations("k8s", "io").endMetadata()
      .withNewSpec()
      .addNewPort().withPort(31337).endPort()
      .endSpec().build()).create();
    final RemoteDevelopmentConfig config = RemoteDevelopmentConfig.builder()
      .localService(LocalService.builder().serviceName("service").port(1337).build())
      .build();
    // When
    new LocalServiceManager(new RemoteDevelopmentContext(logger, kubernetesClient, config)).createOrReplaceServices();
    // Then
    assertThat(kubernetesClient.services().withName("service").get())
      .hasFieldOrPropertyWithValue("metadata.name", "service")
      .extracting(s -> s.getMetadata().getAnnotations().get("jkube/previous-service"))
      .extracting(Serialization::unmarshal)
      .asInstanceOf(InstanceOfAssertFactories.type(Service.class))
      .satisfies(s -> assertThat(s.getMetadata().getAnnotations()).containsEntry("k8s", "io"))
      .extracting("spec.ports").asList()
      .extracting("port")
      .containsExactly(31337);
  }

  @Test
  void createOrReplaceServices_withExistingServiceWithAnnotation_replacesServiceWithPreviousAnnotation() {
    // Given
    kubernetesClient.services().resource(new ServiceBuilder()
      .withNewMetadata()
      .withName("service")
      .addToAnnotations("jkube/previous-service", Serialization.asJson(new ServiceBuilder()
        .withNewMetadata().withName("service").endMetadata()
        .withNewSpec()
        .addNewPort().withPort(42).endPort()
        .endSpec().build()))
      .endMetadata()
      .withNewSpec()
      .addNewPort().withPort(31337).endPort()
      .endSpec().build()).create();
    final RemoteDevelopmentConfig config = RemoteDevelopmentConfig.builder()
      .localService(LocalService.builder().serviceName("service").port(1337).build())
      .build();
    // When
    new LocalServiceManager(new RemoteDevelopmentContext(logger, kubernetesClient, config)).createOrReplaceServices();
    // Then
    assertThat(kubernetesClient.services().withName("service").get())
      .hasFieldOrPropertyWithValue("metadata.name", "service")
      .extracting(s -> s.getMetadata().getAnnotations().get("jkube/previous-service"))
      .extracting(Serialization::unmarshal)
      .isInstanceOf(Service.class)
      .extracting("spec.ports").asList()
      .extracting("port")
      .containsExactly(42);
  }

  @Test
  void createOrReplaceServices_withExistingService_reusesPort() {
    // Given
    kubernetesClient.services().resource(new ServiceBuilder()
      .withNewMetadata().withName("service").endMetadata()
      .withNewSpec()
      .addNewPort().withPort(31337).withNewTargetPort().withValue(42).endTargetPort().endPort()
      .endSpec().build()).create();
    final RemoteDevelopmentConfig config = RemoteDevelopmentConfig.builder()
      .localService(LocalService.builder().serviceName("service").port(1337).build())
      .build();
    // When
    new LocalServiceManager(new RemoteDevelopmentContext(logger, kubernetesClient, config)).createOrReplaceServices();
    // Then
    assertThat(kubernetesClient.services().withName("service").get())
      .hasFieldOrPropertyWithValue("metadata.name", "service")
      .extracting("spec.ports").asList()
      .extracting("targetPort")
      .containsExactly(new IntOrString(42));
  }

  @Test
  void tearDownServices_deletesNewService() {
    // Given
    final Service service = new ServiceBuilder()
      .withNewMetadata().withName("service").endMetadata()
      .withNewSpec()
      .addNewPort().withPort(31337).endPort()
      .endSpec().build();
    kubernetesClient.services().resource(service).create();
    final LocalService localService = LocalService.builder().serviceName("service").port(1337).build();
    final RemoteDevelopmentConfig config = RemoteDevelopmentConfig.builder()
      .localService(localService)
      .build();
    final RemoteDevelopmentContext context = new RemoteDevelopmentContext(logger, kubernetesClient, config);
    context.getManagedServices().put(localService, service);
    // When
    new LocalServiceManager(context).tearDownServices();
    // Then
    assertThat(kubernetesClient.services().withName("service")
      .waitUntilCondition(Objects::isNull, 1, TimeUnit.SECONDS))
      .isNull();
  }

  @Test
  void tearDownServices_restoresOldService() {
    // Given
    final Service service = new ServiceBuilder()
      .withNewMetadata().withName("service")
      .addToAnnotations("jkube/previous-service", Serialization.asJson(new ServiceBuilder()
        .withNewMetadata().withName("service").endMetadata()
        .withNewSpec().addToSelector("old-label-key", "old-label-value").endSpec()
        .build()))
      .endMetadata()
      .withNewSpec()
      .addNewPort().withPort(1337).endPort()
      .endSpec().build();
    kubernetesClient.services().resource(service).create();
    final LocalService localService = LocalService.builder().serviceName("service").port(1337).build();
    final RemoteDevelopmentConfig config = RemoteDevelopmentConfig.builder()
      .localService(localService)
      .build();
    final RemoteDevelopmentContext context = new RemoteDevelopmentContext(logger, kubernetesClient, config);
    context.getManagedServices().put(localService, service);
    // When
    new LocalServiceManager(context).tearDownServices();
    // Then
    assertThat(kubernetesClient.services().withName("service").get())
      .extracting("spec.selector")
      .asInstanceOf(InstanceOfAssertFactories.map(String.class, String.class))
      .containsOnly(entry("old-label-key", "old-label-value"));
  }
}
