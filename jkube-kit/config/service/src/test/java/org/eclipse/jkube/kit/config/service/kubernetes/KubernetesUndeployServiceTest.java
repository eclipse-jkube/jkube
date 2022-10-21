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
import java.util.Collections;

import io.fabric8.kubernetes.api.model.APIResource;
import io.fabric8.kubernetes.api.model.APIResourceBuilder;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;
import io.fabric8.kubernetes.client.GracePeriodConfigurable;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NamespaceableResource;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unused", "rawtypes", "unchecked"})
class KubernetesUndeployServiceTest {
  private KitLogger logger;
  private JKubeServiceHub jKubeServiceHub;
  private MockedStatic<KubernetesHelper> kubernetesHelper;
  private KubernetesUndeployService kubernetesUndeployService;
  private KubernetesClient mockedKubernetesClient;
  private NamespaceableResource mockedNamespaceableResource;
  private Resource mockedResource;
  private MixedOperation mockedMixedOperation;
  private GracePeriodConfigurable mockedGracePeriodConfigurable;
  private NonNamespaceOperation mockedNonNamespaceOperation;

  @BeforeEach
  void setUp() {
    logger = mock(KitLogger.class);
    jKubeServiceHub = mock(JKubeServiceHub.class, RETURNS_DEEP_STUBS);
    mockedKubernetesClient = mock(KubernetesClient.class);
    mockedNamespaceableResource = mock(NamespaceableResource.class);
    mockedResource = mock(Resource.class);
    mockedGracePeriodConfigurable = mock(GracePeriodConfigurable.class);
    mockedMixedOperation = mock(MixedOperation.class);
    mockedNonNamespaceOperation = mock(NonNamespaceOperation.class);
    kubernetesHelper = mockStatic(KubernetesHelper.class);
    when(jKubeServiceHub.getClient()).thenReturn(mockedKubernetesClient);
    kubernetesUndeployService = new KubernetesUndeployService(jKubeServiceHub, logger);
  }

  @AfterEach
  void tearDown() {
    kubernetesHelper.close();
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
    // Given
    File file = mock(File.class);
    final ResourceConfig resourceConfig = ResourceConfig.builder().namespace("default").build();
    final Namespace namespace = new NamespaceBuilder().withNewMetadata().withName("default").endMetadata().build();
    final Pod pod = new PodBuilder().withNewMetadata().withName("MrPoddington").endMetadata().build();
    final Service service = new Service();
    when(file.exists()).thenReturn(true);
    when(file.isFile()).thenReturn(true);
    mockKubernetesClientResourceDeleteCall(pod, "default");
    mockKubernetesClientResourceDeleteCall(service, "default");
    mockKubernetesClientResourceDeleteCall(namespace, "default");
    kubernetesHelper.when(() -> KubernetesHelper.loadResources(file)).thenReturn(Arrays.asList(namespace, pod, service));
    // When
    kubernetesUndeployService.undeploy(null, resourceConfig, file);
    // Then
    kubernetesHelper.verify(() -> KubernetesHelper.getKind(any(HasMetadata.class)), times(3));
    verify(jKubeServiceHub, times(3)).getClient();
    verify(mockedKubernetesClient).resource(pod);
    verify(mockedKubernetesClient).resource(namespace);
    verify(mockedKubernetesClient).resource(service);
    verify(mockedNamespaceableResource, times(3)).inNamespace("default");
    verify(mockedResource, times(3)).withPropagationPolicy(DeletionPropagation.BACKGROUND);
    verify(mockedGracePeriodConfigurable, times(3)).delete();
  }

  @Test
  void undeploy_withManifestAndCustomResources_shouldDeleteAllEntities(@TempDir File temporaryFolder) throws Exception {
    // Given
    ResourceConfig resourceConfig = mock(ResourceConfig.class);
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
    mockKubernetesClientResourceDeleteCall(service, null);
    mockKubernetesClientGenericResourceGetCall(customResource, "my-cr");
    mockKubernetesClientGenericResourceDeleteCall(customResource, null, "my-cr");
    kubernetesHelper.when(() -> KubernetesHelper.loadResources(manifest)).thenReturn(Arrays.asList(service, customResource));
    kubernetesHelper.when(() -> KubernetesHelper.getFullyQualifiedApiGroupWithKind(customResource)).thenReturn(crdId);
    // When
    kubernetesUndeployService.undeploy(null, resourceConfig, manifest);
    // Then
    verify(jKubeServiceHub,times(3)).getClient();
    verify(mockedKubernetesClient).resource(service);
    verify(mockedKubernetesClient, times(2)).genericKubernetesResources("org.eclipse.jkube/v1alpha1", "Crd");
    verify(mockedNamespaceableResource).inNamespace(null);
    verify(mockedMixedOperation, times(2)).inNamespace(null);
    verify(mockedNonNamespaceOperation, times(2)).withName("my-cr");
    verify(mockedGracePeriodConfigurable).delete();
    verify(mockedResource).get();
    verify(mockedResource).delete();
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
    kubernetesHelper.when(() -> KubernetesHelper.loadResources(file)).thenReturn(Arrays.asList(configMap, pod, service));
    kubernetesHelper.when(() -> KubernetesHelper.getNamespace(configMap)).thenReturn("ns1");
    kubernetesHelper.when(() -> KubernetesHelper.getNamespace(pod)).thenReturn("ns2");
    mockKubernetesClientResourceDeleteCall(pod, "ns2");
    mockKubernetesClientResourceDeleteCall(service, "default");
    mockKubernetesClientResourceDeleteCall(configMap, "ns1");
    kubernetesHelper.when(KubernetesHelper::getDefaultNamespace).thenReturn("default");
    // When
    kubernetesUndeployService.undeploy(null, resourceConfig, file);
    // Then
    kubernetesHelper.verify(() -> KubernetesHelper.getKind(any(HasMetadata.class)), times(3));
    verify(jKubeServiceHub, times(3)).getClient();
    verify(mockedKubernetesClient).resource(pod);
    verify(mockedKubernetesClient).resource(configMap);
    verify(mockedKubernetesClient).resource(service);
    verify(mockedNamespaceableResource, times(1)).inNamespace("default");
    verify(mockedNamespaceableResource, times(1)).inNamespace("ns1");
    verify(mockedNamespaceableResource, times(1)).inNamespace("ns2");
    verify(mockedResource, times(3)).withPropagationPolicy(DeletionPropagation.BACKGROUND);
    verify(mockedGracePeriodConfigurable, times(3)).delete();
  }

  private <T extends HasMetadata> void mockKubernetesClientResourceDeleteCall(T kubernetesResource, String namespace) {
    when(mockedKubernetesClient.resource(kubernetesResource)).thenReturn(mockedNamespaceableResource);
    when(mockedNamespaceableResource.inNamespace(namespace)).thenReturn(mockedResource);
    when(mockedResource.withPropagationPolicy(any())).thenReturn(mockedGracePeriodConfigurable);
    when(mockedGracePeriodConfigurable.delete()).thenReturn(Collections.emptyList());
  }

  private void mockKubernetesClientGenericResourceDeleteCall(GenericKubernetesResource customResource, String namespace, String name) {
    when(mockedKubernetesClient.genericKubernetesResources(anyString(), anyString())).thenReturn(mockedMixedOperation);
    when(mockedMixedOperation.inNamespace(namespace)).thenReturn(mockedNonNamespaceOperation);
    when(mockedNonNamespaceOperation.withName(name)).thenReturn(mockedResource);
    when(mockedResource.delete()).thenReturn(Collections.emptyList());
  }

  private void mockKubernetesClientGenericResourceGetCall(GenericKubernetesResource customResource, String name) {
    when(mockedKubernetesClient.genericKubernetesResources(anyString(), anyString())).thenReturn(mockedMixedOperation);
    when(mockedMixedOperation.inNamespace(null)).thenReturn(mockedNonNamespaceOperation);
    when(mockedNonNamespaceOperation.withName(name)).thenReturn(mockedResource);
    when(mockedResource.get()).thenReturn(mock(GenericKubernetesResource.class));
  }
}