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

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import mockit.Mocked;
import mockit.Verifications;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.doDeleteAndWait;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KubernetesClientUtilTest {

  @Mocked
  private KubernetesClient kubernetesClient;

  @Test
  public void doDeleteAndWait_withExistingResource_shouldDeleteAndReachWaitLimit() {
    // Given
    GenericKubernetesResource resource = new GenericKubernetesResourceBuilder()
        .withApiVersion("org.eclipse.jkube/v1beta1")
        .withKind("JKubeCustomResource")
        .withNewMetadata().withName("name").endMetadata()
        .build();
    // When
    doDeleteAndWait(kubernetesClient, resource, "namespace",  2L);
    // Then
    // @formatter:off
    new Verifications(){{
      kubernetesClient.genericKubernetesResources("org.eclipse.jkube/v1beta1", "JKubeCustomResource").inNamespace("namespace").withName("name").delete(); times = 1;
    }};
    // @formatter:on
  }

  @Test
  public void applicableNamespace_whenNamespaceProvidedViaConfiguration_shouldReturnProvidedNamespace() {
    // Given
    String namespaceViaPluginConfigOrJkubeNamespaceProperty = "ns1";

    // When
    String resolvedNamespace = KubernetesClientUtil.applicableNamespace(null, namespaceViaPluginConfigOrJkubeNamespaceProperty, null);

    // Then
    assertThat(resolvedNamespace).isEqualTo(namespaceViaPluginConfigOrJkubeNamespaceProperty);
  }

  @Test
  public void applicableNamespace_whenNamespaceInResourceMetadata_shouldReturnProvidedNamespace() {
    // Given
    Pod pod = new PodBuilder()
        .withNewMetadata().withNamespace("test").endMetadata()
        .build();

    // When
    String resolvedNamespace = KubernetesClientUtil.applicableNamespace(pod, null, null);

    // Then
    assertThat(resolvedNamespace).isEqualTo("test");
  }

  @Test
  public void applicableNamespace_whenNamespaceProvidedViaClusterAccess_shouldReturnProvidedNamespace() {
    // Given
    ClusterAccess mockedClusterAccess = mock(ClusterAccess.class, RETURNS_DEEP_STUBS);
    when(mockedClusterAccess.getNamespace()).thenReturn("ns1");

    // When
    String resolvedNamespace = KubernetesClientUtil.applicableNamespace(null, null, null, mockedClusterAccess);

    // Then
    assertThat(resolvedNamespace).isEqualTo("ns1");
  }

  @Test
  public void applicableNamespace_whenNamespaceProvidedViaResourceConfiguration_shouldReturnProvidedNamespace() {
    // Given
    ResourceConfig resourceConfig = ResourceConfig.builder().namespace("ns1").build();

    // When
    String resolvedNamespace = KubernetesClientUtil.applicableNamespace(null, null, resourceConfig, null);

    // Then
    assertThat(resolvedNamespace).isEqualTo("ns1");
  }

  @Test
  public void resolveFallbackNamespace_whenNamespaceProvidedViaResourceConfiguration_shouldReturnProvidedNamespace() {
    // Given
    ResourceConfig resourceConfig = ResourceConfig.builder().namespace("ns1").build();

    // When
    String resolvedNamespace = KubernetesClientUtil.resolveFallbackNamespace(resourceConfig, null);

    // Then
    assertThat(resolvedNamespace).isEqualTo(resourceConfig.getNamespace());
  }

  @Test
  public void resolveFallbackNamespace_whenNamespaceProvidedViaClusterAccess_shouldReturnProvidedNamespace() {
    // Given
    ClusterAccess mockedClusterAccess = mock(ClusterAccess.class, RETURNS_DEEP_STUBS);
    when(mockedClusterAccess.getNamespace()).thenReturn("ns1");

    // When
    String resolvedNamespace = KubernetesClientUtil.resolveFallbackNamespace(null, mockedClusterAccess);

    // Then
    assertThat(resolvedNamespace).isEqualTo("ns1");
  }

}