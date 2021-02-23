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
import java.util.HashSet;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinitionListBuilder;
import org.eclipse.jkube.kit.common.GenericCustomResource;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings({"ResultOfMethodCallIgnored", "AccessStaticViaInstance", "unused"})
public class KubernetesUndeployServiceTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Mocked
  private KitLogger logger;
  @Mocked
  private JKubeServiceHub jKubeServiceHub;
  @Mocked
  private KubernetesHelper kubernetesHelper;
  private KubernetesUndeployService kubernetesUndeployService;

  @Before
  public void setUp() {
    kubernetesUndeployService = new KubernetesUndeployService(jKubeServiceHub, logger);
  }

  @Test
  public void undeployWithNonexistentManifestShouldDoNothing() throws Exception {
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
  public void undeployWithManifestShouldDeleteAllEntities(@Mocked File file) throws Exception {
    // Given
    final ResourceConfig resourceConfig = ResourceConfig.builder().namespace("default").build();
    final Namespace namespace = new NamespaceBuilder().withNewMetadata().withName("default").endMetadata().build();
    final Pod pod = new PodBuilder().withNewMetadata().withName("MrPoddington").endMetadata().build();
    final Service service = new Service();
    // @formatter:off
    new Expectations() {{
      file.exists(); result = true;
      file.isFile(); result = true;
      kubernetesHelper.loadResources(file);
      result = new HashSet<>(Arrays.asList(namespace, pod, service));
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
  public void undeployWithManifestAndCustomResourcesShouldDeleteAllEntities(
      @Mocked ResourceConfig resourceConfig) throws Exception {
    // Given
    final File manifest = temporaryFolder.newFile("temp.yml");
    final File crManifest = temporaryFolder.newFile("temp-cr.yml");
    final String crdId = "org.eclipse.jkube/v1alpha1#Crd";
    final Service service = new Service();
    final GenericCustomResource customResource = new GenericCustomResource();
    customResource.setMetadata(new ObjectMetaBuilder().withName("my-cr").build());
    final CustomResourceDefinition crd = new CustomResourceDefinitionBuilder()
        .withNewMetadata().withName(crdId).endMetadata()
        .withNewSpec().withGroup("org.eclipse.jkube").withVersion("v1alpha1").withScope("Cluster")
            .withNewNames()
            .withKind("Crd")
            .withPlural("crds")
            .endNames().endSpec()
        .withKind(crdId).build();
    // @formatter:off
    new Expectations() {{
      kubernetesHelper.loadResources(manifest); result = new HashSet<>(Arrays.asList(service, customResource));
      jKubeServiceHub.getClient().apiextensions().v1beta1().customResourceDefinitions().list(); result = new CustomResourceDefinitionListBuilder().withItems(crd).build();
      kubernetesHelper.getFullyQualifiedApiGroupWithKind((CustomResourceDefinitionContext)any);
      result = crdId;
    }};
    // @formatter:on
    // When
    kubernetesUndeployService.undeploy(null, resourceConfig, manifest);
    // Then
    // @formatter:off
    new Verifications() {{
      jKubeServiceHub.getClient().customResource((CustomResourceDefinitionContext)any).delete("my-cr");
      times = 1;
    }};
    // @formatter:on
  }

}