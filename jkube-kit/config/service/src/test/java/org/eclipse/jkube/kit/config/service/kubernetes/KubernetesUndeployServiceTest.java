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
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SuppressWarnings({"ResultOfMethodCallIgnored", "AccessStaticViaInstance", "unused"})
class KubernetesUndeployServiceTest {
  @Mocked
  private KitLogger logger;
  @Mocked
  private JKubeServiceHub jKubeServiceHub;
  @Mocked
  private KubernetesHelper kubernetesHelper;
  private KubernetesUndeployService kubernetesUndeployService;

  @BeforeEach
  void setUp() {
    kubernetesUndeployService = new KubernetesUndeployService(jKubeServiceHub, logger);
  }

  @Test
  void undeploy_withNonexistentManifest_shouldDoNothing() throws Exception {
    // Given
    final File nonexistent = new File("I don't exist");
    // When
    kubernetesUndeployService.undeploy(null, ResourceConfig.builder().build(), nonexistent, null);
    // Then
    // @formatter:off
    new Verifications() {{
      logger.warn("No such generated manifests found for this project, ignoring."); times = 1;
    }};
    // @formatter:on
  }

  @Test
  void undeploy_withManifest_shouldDeleteAllEntities(@Mocked File file) throws Exception {
    // Given
    final ResourceConfig resourceConfig = ResourceConfig.builder().namespace("default").build();
    final Namespace namespace = new NamespaceBuilder().withNewMetadata().withName("default").endMetadata().build();
    final Pod pod = new PodBuilder().withNewMetadata().withName("MrPoddington").endMetadata().build();
    final Service service = new Service();
    // @formatter:off
    new Expectations() {{
      file.exists(); result = true;
      file.isFile(); result = true;
      kubernetesHelper.loadResources(file); result = Arrays.asList(namespace, pod, service);
    }};
    // @formatter:on
    // When
    kubernetesUndeployService.undeploy(null, resourceConfig, file);
    // Then
    // @formatter:off
    new Verifications() {{
      kubernetesHelper.getKind((HasMetadata)any); times = 3;
      jKubeServiceHub.getClient().resource(pod).inNamespace("default")
          .withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
      times = 1;
      jKubeServiceHub.getClient().resource(service).inNamespace("default")
          .withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
      times = 1;
      jKubeServiceHub.getClient().resource(namespace).inNamespace("default")
              .withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
      times = 1;
    }};
    // @formatter:on
  }

  @Test
  void undeploy_withManifestAndCustomResources_shouldDeleteAllEntities(@TempDir File temporaryFolder,
      @Mocked ResourceConfig resourceConfig) throws Exception {
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
    // @formatter:off
    new Expectations() {{
      kubernetesHelper.loadResources(manifest); result = Arrays.asList(service, customResource);
      kubernetesHelper.getFullyQualifiedApiGroupWithKind(customResource);
      result = crdId;
    }};
    // @formatter:on
    // When
    kubernetesUndeployService.undeploy(null, resourceConfig, manifest);
    // Then
    // @formatter:off
    new Verifications() {{
      jKubeServiceHub.getClient().genericKubernetesResources("org.eclipse.jkube/v1alpha1", "Crd").inNamespace(null).withName("my-cr").delete();
      times = 1;
    }};
    // @formatter:on
  }

  @Test
  void undeploy_withManifest_shouldDeleteEntitiesInMultipleNamespaces(@Mocked File file) throws Exception {
    // Given
    final ResourceConfig resourceConfig = ResourceConfig.builder().build();
    final ConfigMap configMap = new ConfigMapBuilder().withNewMetadata().withName("cm1").withNamespace("ns1").endMetadata().build();
    final Pod pod = new PodBuilder().withNewMetadata().withName("MrPoddington").withNamespace("ns2").endMetadata().build();
    final Service service = new Service();
    // @formatter:off
    new Expectations() {{
      file.exists(); result = true;
      file.isFile(); result = true;
      kubernetesHelper.loadResources(file); result = Arrays.asList(configMap, pod, service);
      kubernetesHelper.getNamespace(configMap); result = "ns1";
      kubernetesHelper.getNamespace(pod); result = "ns2";
      KubernetesHelper.getDefaultNamespace(); result = "default";
    }};
    // @formatter:on
    // When
    kubernetesUndeployService.undeploy(null, resourceConfig, file);
    // Then
    // @formatter:off
    new Verifications() {{
      kubernetesHelper.getKind((HasMetadata)any); times = 3;
      jKubeServiceHub.getClient().resource(pod).inNamespace("ns2")
              .withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
      times = 1;
      jKubeServiceHub.getClient().resource(service).inNamespace("default")
              .withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
      times = 1;
      jKubeServiceHub.getClient().resource(configMap).inNamespace("ns1")
              .withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
      times = 1;
    }};
    // @formatter:on
  }

}