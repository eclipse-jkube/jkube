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
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SuppressWarnings({"unused"})
@EnableKubernetesMockClient(crud = true)
class KubernetesUndeployServiceTest {

  @TempDir
  private Path tempDir;
  private KubernetesClient kubernetesClient;
  private KubernetesMockServer mockServer;
  private KitLogger logger;
  private KubernetesUndeployService kubernetesUndeployService;

  @BeforeEach
  void setUp() {
    logger = spy(new KitLogger.SilentLogger());
    final JKubeServiceHub jKubeServiceHub = JKubeServiceHub.builder()
      .log(logger)
      .platformMode(RuntimeMode.KUBERNETES)
      .configuration(JKubeConfiguration.builder().build())
      .clusterAccess(new ClusterAccess(logger, ClusterConfiguration.from(kubernetesClient.getConfiguration()).namespace("test").build()))
      .build();
    kubernetesUndeployService = new KubernetesUndeployService(jKubeServiceHub, logger);
  }

  @Test
  void undeploy_withNonexistentManifest_shouldDoNothing() throws Exception {
    // Given
    final File nonexistent = new File("I don't exist");
    // When
    kubernetesUndeployService.undeploy(null, ResourceConfig.builder().build(), nonexistent, null);
    // Then
    verify(logger, times(1))
      .warn("No such generated manifests found for this project, ignoring.");
  }

  @Test
  void undeploy_withManifest_shouldDeleteAllEntities() throws Exception {
    // Given
    final ResourceConfig resourceConfig = ResourceConfig.builder().namespace("default").build();
    final Namespace namespace = new NamespaceBuilder().withNewMetadata().withName("default").endMetadata().build();
    final Pod pod = new PodBuilder().withNewMetadata().withName("MrPoddington").endMetadata().build();
    final Service service = new ServiceBuilder().withNewMetadata().withName("atYourService").endMetadata().build();
    final ConfigMap control = new ConfigMapBuilder().withNewMetadata().withName("control").endMetadata().build();
    for (HasMetadata entity : new HasMetadata[]{namespace, pod, service, control}) {
      kubernetesClient.resource(entity).inNamespace("default").create();
    }
    final File manifest = serializedManifest(namespace, pod, service);
    // When
    kubernetesUndeployService.undeploy(null, resourceConfig, manifest);
    // Then
    assertThat(kubernetesClient.namespaces().withName("default").get()).isNull();
    assertThat(kubernetesClient.pods().inNamespace("default").withName("MrPoddington").get()).isNull();
    assertThat(kubernetesClient.services().inNamespace("default").withName("atYourService").get()).isNull();
    assertThat(kubernetesClient.configMaps().inNamespace("default").withName("control").get()).isNotNull();
  }

  @Test
  void undeploy_withManifestAndCustomResources_shouldDeleteAllEntities() throws Exception {
    // Given
    final CustomResourceDefinitionContext context = new CustomResourceDefinitionContext.Builder()
      .withGroup("org.eclipse.jkube")
      .withVersion("v1alpha1")
      .withPlural("crds")
      .withKind("Crd")
      .build();
    mockServer.expectCustomResource(context);
    final ResourceConfig resourceConfig = ResourceConfig.builder().build();
    final String crdId = "org.eclipse.jkube/v1alpha1#Crd";
    final GenericKubernetesResource customResource = new GenericKubernetesResourceBuilder()
        .withApiVersion("org.eclipse.jkube/v1alpha1")
        .withKind("Crd")
        .withMetadata(new ObjectMetaBuilder().withName("my-cr").build())
        .build();
    final GenericKubernetesResource control = new GenericKubernetesResourceBuilder(customResource)
      .editMetadata().withName("control").endMetadata().build();
    for (GenericKubernetesResource entity : new GenericKubernetesResource[]{customResource, control}) {
      kubernetesClient.genericKubernetesResources(context).resource(entity).create();
    }
    final File manifest = serializedManifest(customResource);
    // When
    kubernetesUndeployService.undeploy(null, resourceConfig, manifest);
    // Then
    assertThat(kubernetesClient.genericKubernetesResources(context).withName("my-cr").get()).isNull();
    assertThat(kubernetesClient.genericKubernetesResources(context).withName("control").get()).isNotNull();
  }

  @Test
  void undeploy_withManifest_shouldDeleteEntitiesInMultipleNamespaces() throws Exception {
    // Given
    final ResourceConfig resourceConfig = ResourceConfig.builder().build();
    final ConfigMap configMap = new ConfigMapBuilder().withNewMetadata().withName("cm1")
      .withNamespace("ns1").endMetadata().build();
    final Pod pod = new PodBuilder().withNewMetadata().withName("MrPoddington")
      .withNamespace("ns2").endMetadata().build();
    final Service service = new ServiceBuilder().withNewMetadata().withName("atYourService").endMetadata().build();
    final ConfigMap control = new ConfigMapBuilder().withNewMetadata().withName("control")
      .withNamespace("ns2").endMetadata().build();
    for (HasMetadata entity : new HasMetadata[]{configMap, pod, service, control}) {
      kubernetesClient.resource(entity).create();
    }
    final File manifest = serializedManifest(configMap, pod, service);
    // When
    kubernetesUndeployService.undeploy(null, resourceConfig, manifest);
    // Then
    assertThat(kubernetesClient.configMaps().inNamespace("ns1").withName("cm1").get()).isNull();
    assertThat(kubernetesClient.pods().inNamespace("ns2").withName("MrPoddington").get()).isNull();
    assertThat(kubernetesClient.services().inNamespace("test").withName("atYourService").get()).isNull();
    assertThat(kubernetesClient.configMaps().inNamespace("ns2").withName("control").get()).isNotNull();
  }

  private File serializedManifest(HasMetadata... resources) throws IOException {
    final File file = Files.createFile(tempDir.resolve("kubernetes.yml")).toFile();
    FileUtils.write(file,
      Serialization.asJson(new KubernetesListBuilder().addToItems(resources).build()),
      Charset.defaultCharset());
    return file;
  }
}
