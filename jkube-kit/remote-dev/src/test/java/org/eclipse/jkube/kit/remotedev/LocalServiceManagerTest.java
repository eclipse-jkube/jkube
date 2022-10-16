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

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@EnableKubernetesMockClient(crud = true)
class LocalServiceManagerTest {


  private static JKubeServiceHub jKubeServiceHub;
  @SuppressWarnings("unused")
  private KubernetesMockServer mockServer;
  @SuppressWarnings("unused")
  private KubernetesClient kubernetesClient;

  @BeforeEach
  void setUp() {
    mockServer.reset();
    jKubeServiceHub = JKubeServiceHub.builder()
      .platformMode(RuntimeMode.KUBERNETES)
      .log(new KitLogger.StdoutLogger())
      .configuration(JKubeConfiguration.builder().build())
      .clusterAccess(new ClusterAccess(null, null) {
        @SuppressWarnings("unchecked")
        @Override
        public KubernetesClient createDefaultClient() {
          return kubernetesClient;
        }
      })
      .build();
  }

  @Test
  void createOrReplaceServices_createsNewService() {
    // Given
    final RemoteDevelopmentConfig config = RemoteDevelopmentConfig.builder()
      .localService(LocalService.builder().serviceName("service").type("NodePort").port(1337).build())
      .build();
    // When
    new LocalServiceManager(new RemoteDevelopmentContext(jKubeServiceHub, config)).createOrReplaceServices();
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
  void createOrReplaceServices_withExistingService_replacesService() {
    // Given
    kubernetesClient.services().resource(new ServiceBuilder()
      .withNewMetadata().withName("service").endMetadata()
      .withNewSpec()
      .addNewPort().withPort(31337).endPort()
      .endSpec().build()).create();
    final RemoteDevelopmentConfig config = RemoteDevelopmentConfig.builder()
      .localService(LocalService.builder().serviceName("service").port(1337).build())
      .build();
    // When
    new LocalServiceManager(new RemoteDevelopmentContext(jKubeServiceHub, config)).createOrReplaceServices();
    // Then
    assertThat(kubernetesClient.services().withName("service").get())
      .hasFieldOrPropertyWithValue("metadata.name", "service")
      .extracting(s -> s.getMetadata().getAnnotations().get("jkube/previous-service"))
      .extracting(Serialization::unmarshal)
      .isInstanceOf(Service.class)
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
    new LocalServiceManager(new RemoteDevelopmentContext(jKubeServiceHub, config)).createOrReplaceServices();
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
  void tearDownServices_deletesNewService() {
    // Given
    kubernetesClient.services().resource(new ServiceBuilder()
      .withNewMetadata().withName("service").endMetadata()
      .withNewSpec()
      .addNewPort().withPort(31337).endPort()
      .endSpec().build()).create();
    final RemoteDevelopmentConfig config = RemoteDevelopmentConfig.builder()
      .localService(LocalService.builder().serviceName("service").port(1337).build())
      .build();
    // When
    new LocalServiceManager(new RemoteDevelopmentContext(jKubeServiceHub, config)).tearDownServices();
    // Then
    assertThat(kubernetesClient.services().withName("service")
      .waitUntilCondition(Objects::isNull, 1, TimeUnit.SECONDS))
      .isNull();
  }

  @Test
  void tearDownServices_restoresOldService() {
    // Given
    kubernetesClient.services().resource(new ServiceBuilder()
      .withNewMetadata().withName("service")
      .addToAnnotations("jkube/previous-service", Serialization.asJson(new ServiceBuilder()
        .withNewMetadata().withName("service").endMetadata()
        .withNewSpec().addToSelector("old-label-key", "old-label-value").endSpec()
        .build()))
      .endMetadata()
      .withNewSpec()
      .addNewPort().withPort(1337).endPort()
      .endSpec().build()).create();
    final RemoteDevelopmentConfig config = RemoteDevelopmentConfig.builder()
      .localService(LocalService.builder().serviceName("service").port(1337).build())
      .build();
    // When
    new LocalServiceManager(new RemoteDevelopmentContext(jKubeServiceHub, config)).tearDownServices();
    // Then
    assertThat(kubernetesClient.services().withName("service").get())
      .extracting("spec.selector")
      .asInstanceOf(InstanceOfAssertFactories.map(String.class, String.class))
      .containsOnly(entry("old-label-key", "old-label-value"));
  }
}
