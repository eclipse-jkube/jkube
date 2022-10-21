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

package org.eclipse.jkube.kit.config.service.kubernetes;

import java.io.File;
import java.util.Arrays;

import io.fabric8.kubernetes.api.model.APIResource;
import io.fabric8.kubernetes.api.model.APIResourceBuilder;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"AccessStaticViaInstance", "unused"})
class KubernetesUndeployServiceTest {

  @Mock

  private KitLogger logger;

  private JKubeServiceHub jKubeServiceHub;

  private MockedStatic<KubernetesHelper> kubernetesHelper;
  private KubernetesUndeployService kubernetesUndeployService;

  @BeforeEach
  void setUp() {
    logger = mock(KitLogger.class);
    jKubeServiceHub = mock(JKubeServiceHub.class);
    kubernetesHelper = mockStatic(KubernetesHelper.class);
    kubernetesUndeployService = new KubernetesUndeployService(jKubeServiceHub, logger);
  }

  @Test
  void undeploy_withNonexistentManifest_shouldDoNothing() throws Exception {
    // Given
    final File nonexistent = new File("I don't exist");
    // When
    kubernetesUndeployService.undeploy(null, ResourceConfig.builder().build(), nonexistent, null);
    // Then
    verify(logger,times(1)).warn("No such generated manifests found for this project, ignoring.");
  }

  @Test
  void undeploy_withManifest_shouldDeleteAllEntities() throws Exception {
    File file = mock(File.class);
    // Given
    final ResourceConfig resourceConfig = ResourceConfig.builder().namespace("default").build();
    final Namespace namespace = new NamespaceBuilder().withNewMetadata().withName("default").endMetadata().build();
    final Pod pod = new PodBuilder().withNewMetadata().withName("MrPoddington").endMetadata().build();
    final Service service = new Service();
    when(file.exists()).thenReturn(true);
    when(file.isFile()).thenReturn(true);
    when(kubernetesHelper.loadResources(file)).thenReturn(A);
    kubernetesHelper.when(KubernetesHelper.loadResources(file)).thenReturn()
    // When
    kubernetesUndeployService.undeploy(null, resourceConfig, file);
    // Then
    verify(kubernetesHelper, times(3)).getKind((HasMetadata) any());
    verify(jKubeServiceHub, times(1)).getClient().resource(pod).inNamespace("default")
        .withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
    verify(jKubeServiceHub, times(1)).getClient().resource(service).inNamespace("default")
        .withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
    verify(jKubeServiceHub, times(1)).getClient().resource(namespace).inNamespace("default")
        .withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
  }

  @Test
  public void undeployWithManifestShouldDeleteAllEntities() throws Exception {
    try (MockedStatic<KubernetesHelper> mockStatic = Mockito.mockStatic(KubernetesHelper.class)) {
      // Given
      File file = mock(File.class);
      // Given
      final ResourceConfig resourceConfig = ResourceConfig.builder().namespace("default").build();
      final Namespace namespace = new NamespaceBuilder().withNewMetadata().withName("default").endMetadata().build();
      final Pod pod = new PodBuilder().withNewMetadata().withName("MrPoddington").endMetadata().build();
      final Service service = new Service();
      when(file.exists()).thenReturn(true);
      when(file.isFile()).thenReturn(true);
      when(kubernetesHelper.loadResources(file)).thenReturn(Arrays.asList(namespace, pod, service));
      // When
      kubernetesUndeployService.undeploy(null, resourceConfig, file);
      // Then
      verify(kubernetesHelper, times(3)).getKind((HasMetadata) any());
      verify(jKubeServiceHub, times(1)).getClient().resource(pod).inNamespace("default")
              .withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
      verify(jKubeServiceHub, times(1)).getClient().resource(service).inNamespace("default")
              .withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
      verify(jKubeServiceHub, times(1)).getClient().resource(namespace).inNamespace("default")
              .withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
    }
  }

  @Test
  void undeploy_withManifestAndCustomResources_shouldDeleteAllEntities(@TempDir File temporaryFolder) throws Exception {
    ResourceConfig resourceConfig = mock(ResourceConfig.class);
    // Given
    final File manifest = File.createTempFile("temp", ".yml", temporaryFolder);
    final File crManifest = File.createTempFile("temp-cr", ".yml", temporaryFolder);
    final String crdId = "org.eclipse.jkube/v1alpha1#Crd";
    final Service service = new Service();
    final GenericKubernetesResource customResource = new GenericKubernetesResourceBuilder()
        .withApiVersion("org.eclipse.jkube/v1alpha1")
        .withKind("Crd")
        .build();
    customResource.setMetadata(new ObjectMetaBuilder().withName("my-cr").build());
    final APIResource customResourceApiResource = new APIResourceBuilder()
        .withGroup("org.eclipse.jkube")
        .withNamespaced(false)
        .withKind("Crd")
        .withName("crds")
        .withSingularName("crd")
        .build();
    when(kubernetesHelper.loadResources(manifest)).thenReturn(Arrays.asList(service, customResource));
    when(kubernetesHelper.getFullyQualifiedApiGroupWithKind(customResource)).thenReturn(crdId);
    // When
    kubernetesUndeployService.undeploy(null, resourceConfig, manifest);
    // Then
    verify(jKubeServiceHub,times(1)).getClient().genericKubernetesResources("org.eclipse.jkube/v1alpha1", "Crd").inNamespace(null).withName("my-cr").delete();
  }

  @Test
  void undeploy_withManifest_shouldDeleteEntitiesInMultipleNamespaces() throws Exception {
    File file = mock(File.class);
    // Given
    final ResourceConfig resourceConfig = ResourceConfig.builder().build();
    final ConfigMap configMap = new ConfigMapBuilder().withNewMetadata().withName("cm1").withNamespace("ns1").endMetadata().build();
    final Pod pod = new PodBuilder().withNewMetadata().withName("MrPoddington").withNamespace("ns2").endMetadata().build();
    final Service service = new Service();
    when(file.isFile()).thenReturn(true);
    when(file.exists()).thenReturn(true);
    when(kubernetesHelper.loadResources(file)).thenReturn(Arrays.asList(configMap, pod, service));
    when(kubernetesHelper.getNamespace(configMap)).thenReturn("ns1");
    when(kubernetesHelper.getNamespace(pod)).thenReturn("ns2");
    when(kubernetesHelper.getDefaultNamespace()).thenReturn("default");
    // When
    kubernetesUndeployService.undeploy(null, resourceConfig, file);
    // Then
    verify(kubernetesHelper,times(3)).getKind((HasMetadata)any());
    verify(jKubeServiceHub,times(1)).getClient().resource(pod).inNamespace("ns2")
            .withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
    verify(jKubeServiceHub,times(1)).getClient().resource(service).inNamespace("default")
            .withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
    verify(jKubeServiceHub,times(1)).getClient().resource(configMap).inNamespace("ns1")
            .withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
  }
}