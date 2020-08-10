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
import java.util.HashSet;
import java.util.List;

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
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.ProjectBuilder;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"ResultOfMethodCallIgnored", "AccessStaticViaInstance", "unchecked", "unused"})
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
    kubernetesUndeployService.undeploy(null, null, nonexistent, null);
    // Then
    // @formatter:off
    new Verifications() {{
      logger.warn("No such generated manifests found for this project, ignoring."); times = 1;
    }};
    // @formatter:on
  }

  @Test
  public void undeployWithManifestShouldDeleteApplicableEntities(@Mocked File file) throws Exception {
    // Given
    final Namespace namespace = new NamespaceBuilder().withNewMetadata().withName("default").endMetadata().build();
    final Pod pod = new PodBuilder().withNewMetadata().withName("MrPoddington").endMetadata().build();
    final Service service = new Service();
    // @formatter:off
    new Expectations() {{
      file.exists(); result = true;
      file.isFile(); result = true;
      kubernetesHelper.loadResources(file);
      result = new HashSet<>(Arrays.asList(new Project(), namespace, pod, service));
    }};
    // @formatter:on
    // When
    kubernetesUndeployService.undeploy(null, null, file);
    // Then
    // @formatter:off
    new Verifications() {{
      kubernetesHelper.getKind((HasMetadata)any); times = 2;
      jKubeServiceHub.getClient().resource(pod).inNamespace("default")
          .withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
      times = 1;
      jKubeServiceHub.getClient().resource(service).inNamespace("default")
          .withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
      times = 1;
    }};
    // @formatter:on
  }

  @Test
  public void undeployWithManifestAndCustomResourcesShouldDeleteApplicableEntities(
      @Mocked ResourceConfig resourceConfig) throws Exception {
    // Given
    final File manifest = temporaryFolder.newFile("temp.yml");
    final File crManifest = temporaryFolder.newFile("temp-cr.yml");
    final String crdId = "crd";
    final Service service = new Service();
    final CustomResourceDefinition crd = new CustomResourceDefinitionBuilder()
        .withNewMetadata().withName(crdId).endMetadata()
        .withNewSpec().withGroup(crdId).withNewNames().endNames().endSpec()
        .withKind(crdId).build();
    // @formatter:off
    new Expectations() {{
      kubernetesHelper.loadResources(manifest); result = new HashSet<>(Collections.singletonList(service));
      resourceConfig.getCustomResourceDefinitions(); result = Collections.singletonList(crdId);
      jKubeServiceHub.getClient().customResourceDefinitions().withName(crdId).get(); result = crd;
      kubernetesHelper.getCustomResourcesFileToNameMap((File)any, (List<String>)any, logger);
      result = Collections.singletonMap(crManifest, crdId);
      kubernetesHelper.unmarshalCustomResourceFile(crManifest);
      result = Collections.singletonMap("metadata", Collections.singletonMap("name", "my-cr"));
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
  @Test
  public void currentNamespaceWithNamespaceEntityShouldReturnFromNamespace() {
    // Given
    final Pod other = new PodBuilder().withNewMetadata().withName("MrPoddington").endMetadata().build();
    final Namespace namespace = new NamespaceBuilder().withNewMetadata().withName("FromNamespace").endMetadata().build();
    final Project project = new ProjectBuilder().withNewMetadata().withName("FromProject").endMetadata().build();
    // When
    final String result = kubernetesUndeployService.currentNamespace(Arrays.asList(other, namespace, project, null));
    // Then
    assertThat(result).isEqualTo("FromNamespace");
  }

  @Test
  public void currentNamespaceWithNoNamespaceEntityShouldReturnFromProject() {
    // Given
    final Pod other = new PodBuilder().withNewMetadata().withName("MrPoddington").endMetadata().build();
    final Project project = new ProjectBuilder().withNewMetadata().withName("FromProject").endMetadata().build();
    // When
    final String result = kubernetesUndeployService.currentNamespace(Arrays.asList(other, project, null));
    // Then
    assertThat(result).isEqualTo("FromProject");
  }

  @Test
  public void currentNamespaceWithNoValidEntitiesShouldReturnFromConfig() {
    // Given
    final Pod other = new PodBuilder().withNewMetadata().withName("MrPoddington").endMetadata().build();
    // @formatter:off
    new Expectations() {{
      jKubeServiceHub.getClusterAccess().getNamespace(); result = "the-default";
    }};
    // @formatter
    // When
    final String result = kubernetesUndeployService.currentNamespace(Arrays.asList(other, null));
    // Then
    assertThat(result).isEqualTo("the-default");
  }

}