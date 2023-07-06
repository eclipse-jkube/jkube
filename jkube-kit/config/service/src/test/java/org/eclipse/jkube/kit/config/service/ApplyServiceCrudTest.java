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
package org.eclipse.jkube.kit.config.service;

import io.fabric8.kubernetes.api.model.APIGroupBuilder;
import io.fabric8.kubernetes.api.model.APIGroupList;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.ProjectBuilder;
import io.fabric8.openshift.api.model.ProjectRequestBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@EnableKubernetesMockClient(crud = true)
class ApplyServiceCrudTest {

  KubernetesMockServer kubernetesMockServer;
  KubernetesClient kubernetesClient;
  private KitLogger log;
  private APIGroupList apiGroupList;
  private ApplyService applyService;

  @BeforeEach
  void setUp() {
    log = spy(new KitLogger.SilentLogger());
    apiGroupList = new APIGroupList();
    final JKubeServiceHub serviceHub = JKubeServiceHub.builder()
      .log(log)
      .configuration(JKubeConfiguration.builder().build())
      .platformMode(RuntimeMode.KUBERNETES)
      .clusterAccess(new ClusterAccess(ClusterConfiguration.from(kubernetesClient.getConfiguration()).build()))
      .build();
    applyService = new ApplyService(serviceHub);
    applyService.setNamespace("default");
    kubernetesMockServer.expect()
      .get()
      .withPath("/apis")
      .andReply(200, rr -> apiGroupList)
      .always();
  }

  @Test
  @DisplayName("apply from List with nested objects, should apply all objects")
  void applyWithNestedObjectsInList() {
    // Given
    final List<? super HasMetadata> toApply = Arrays.asList(
      new PodBuilder().withNewMetadata().withName("a-pod").endMetadata().build(),
      new ConfigMapBuilder().withNewMetadata().withName("a-config-map").endMetadata().build()
    );
    // When
    applyService.apply(toApply, "list.yml");
    // Then
    assertThat(kubernetesClient)
      .returns("a-pod", c -> c.pods().inNamespace("default").withName("a-pod").get().getMetadata().getName())
      .returns("a-config-map", c -> c.configMaps().inNamespace("default").withName("a-config-map").get().getMetadata().getName());
  }

  @Test
  @DisplayName("apply from KubernetesList with nested objects, should apply all objects")
  void applyWithNestedObjectsInKubernetesList() {
    // Given
    final KubernetesList toApply = new KubernetesListBuilder()
      .addToItems(new PodBuilder().withNewMetadata().withName("a-pod").endMetadata().build())
      .addToItems(new ConfigMapBuilder().withNewMetadata().withName("a-config-map").endMetadata().build())
      .build();
    // When
    applyService.apply(toApply, "list.yml");
    // Then
    assertThat(kubernetesClient)
      .returns("a-pod", c -> c.pods().inNamespace("default").withName("a-pod").get().getMetadata().getName())
      .returns("a-config-map", c -> c.configMaps().inNamespace("default").withName("a-config-map").get().getMetadata().getName());
  }

  @Test
  @DisplayName("apply from recursive List with nested objects, should apply only objects")
  void applyWithNestedObjectsInRecursiveList() {
    // Given
    final List<Object> toApply = new ArrayList<>();
    toApply.add(new PodBuilder().withNewMetadata().withName("a-pod").endMetadata().build());
    toApply.add(new ConfigMapBuilder().withNewMetadata().withName("a-config-map").endMetadata().build());
    //noinspection CollectionAddedToSelf
    toApply.add(toApply);
    // When
    applyService.apply(toApply, "list.yml");
    // Then
    assertThat(kubernetesClient)
      .returns("a-pod", c -> c.pods().inNamespace("default").withName("a-pod").get().getMetadata().getName())
      .returns("a-config-map", c -> c.configMaps().inNamespace("default").withName("a-config-map").get().getMetadata().getName());
  }

  @Test
  @DisplayName("apply in services mode, should apply only services")
  void applyInServicesMode() {
    // Given
    final KubernetesList toApply = new KubernetesListBuilder()
      .addToItems(new PodBuilder().withNewMetadata().withName("a-pod").endMetadata().build())
      .addToItems(new ServiceBuilder().withNewMetadata().withName("a-service").endMetadata().build())
      .build();
    applyService.setServicesOnlyMode(true);
    // When
    applyService.apply(toApply, "list.yml");
    // Then
    assertThat(kubernetesClient)
      .returns(null, c -> c.pods().inNamespace("default").withName("a-pod").get())
      .returns("a-service", c -> c.services().inNamespace("default").withName("a-service").get().getMetadata().getName());
  }

  @Test
  @DisplayName("apply with new resource and creation disabled, should do nothing")
  void applyWithNewResourceAndCreationDisabled() {
    // Given
    final Pod toApply = new PodBuilder().withNewMetadata()
      .withName("a-pod")
      .endMetadata().build();
    applyService.setAllowCreate(false);
    // When
    applyService.apply(toApply, "pod.yml");
    // Then
    assertThat(kubernetesClient.pods().inNamespace("default").withName("a-pod").get())
      .isNull();
    verify(log).warn("Creation disabled so not creating a pod from %s in namespace %s with name %s",
      "pod.yml", "default", "a-pod");
  }

  @Test
  @DisplayName("apply with existing resource, should update the resource")
  void applyWithExistingResource() {
    // Given
    final Pod toApply = new PodBuilder().withNewMetadata()
      .withName("a-pod")
      .endMetadata().build();
    final Pod original = kubernetesClient.resource(toApply).inNamespace("default").create();
    // When
    applyService.apply(toApply, "pod.yml");
    // Then
    assertThat(kubernetesClient.pods().inNamespace("default").withName("a-pod").get())
      .hasFieldOrPropertyWithValue("metadata.uid", original.getMetadata().getUid())
      .hasFieldOrPropertyWithValue("metadata.creationTimestamp", original.getMetadata().getCreationTimestamp());
  }

  @Test
  @DisplayName("apply with existing resource, in recreate mode, should delete and recreate resource")
  void applyWithExistingResourceInRecreateMode() {
    // Given
    final Pod toApply = new PodBuilder().withNewMetadata()
      .withName("a-pod")
      .endMetadata().build();
    final Pod original = kubernetesClient.resource(toApply).inNamespace("default").create();
    applyService.setRecreateMode(true);
    // When
    applyService.apply(toApply, "pod.yml");
    // Then
    assertThat(kubernetesClient.pods().inNamespace("default").withName("a-pod").get())
      .extracting("metadata.uid")
      .isNotEqualTo(original.getMetadata().getUid());
  }

  @Test
  @DisplayName("apply with Project, in vanilla Kubernetes, should do nothing")
  void applyProjectInKubernetes() {
    // Given
    final Project toApply = new ProjectBuilder().withNewMetadata()
      .withName("a-project")
      .endMetadata().build();
    // When
    applyService.apply(toApply, "project.yml");
    // Then
    assertThat(kubernetesClient.adapt(OpenShiftClient.class).projects().withName("a-project").get())
      .isNull();
    verify(log).warn("Cannot check for Project a-project as not running against OpenShift!");
  }

  @Test
  @DisplayName("apply with Project, in OpenShift, should request Project")
  void applyProjectInOpenShift() {
    // Given
    final Project toApply = new ProjectBuilder().withNewMetadata()
      .withName("a-project")
      .endMetadata().build();
    apiGroupList.getGroups().add(new APIGroupBuilder().withName("build.openshift.io").withApiVersion("v1").build());
    // When
    applyService.apply(toApply, "project.yml");
    // Then
    // Hack to retrieve the ProjectRequest from the mock API server (the real OpenShift API won't allow the get operation)
    assertThat(kubernetesClient.resource(new ProjectRequestBuilder().withNewMetadata().withName("a-project").endMetadata().build()).get())
      .isNotNull();
    verify(log).info(startsWith("Created ProjectRequest:"));
  }
}
