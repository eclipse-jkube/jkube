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
package org.eclipse.jkube.kit.resource.service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.ResourceFileType;
import org.eclipse.jkube.kit.common.util.ResourceClassifier;
import org.eclipse.jkube.kit.config.resource.EnricherManager;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;

import io.fabric8.kubernetes.api.model.KubernetesList;
import mockit.Mocked;
import mockit.Verifications;
import org.eclipse.jkube.kit.config.resource.ResourceServiceConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultResourceServiceTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  @SuppressWarnings("unused")
  @Mocked
  private EnricherManager enricherManager;
  @SuppressWarnings("unused")
  @Mocked
  private KitLogger kitLogger;
  @SuppressWarnings("unused")
  @Mocked
  private ResourceConfig resourceConfig;
  @Mocked
  private JavaProject project;

  private File targetDir;
  private ResourceServiceConfig resourceServiceConfig;
  private DefaultResourceService defaultResourceService;

  @Before
  public void init() throws IOException {
    targetDir = temporaryFolder.newFolder("target");
    resourceServiceConfig = ResourceServiceConfig.builder()
        .interpolateTemplateParameters(true)
        .targetDir(targetDir)
        .project(project)
        .resourceFileType(ResourceFileType.yaml)
        .resourceDirs(Collections.singletonList(temporaryFolder.newFolder("resources")))
        .resourceConfig(resourceConfig)
        .build();
    defaultResourceService = new DefaultResourceService(resourceServiceConfig);
  }

  @Test
  public void generateResourcesWithNoResourcesShouldReturnEmpty() throws IOException {
    // When
    final KubernetesList result = defaultResourceService
        .generateResources(PlatformMode.kubernetes, enricherManager, kitLogger);
    // Then
    assertThat(result.getItems()).isEmpty();
  }

  @Test
  public void generateResources_withResources_shouldReturnKubernetesResourceList() throws IOException {
    // Given
    File resourceDir1 = new File(Objects.requireNonNull(getClass().getResource("/jkube/common")).getFile());
    File resourceDir2 = new File(Objects.requireNonNull(getClass().getResource("/jkube/dev")).getFile());
    List<File> resourceDirs = Arrays.asList(resourceDir1, resourceDir2);
    resourceServiceConfig = resourceServiceConfig.toBuilder().resourceDirs(resourceDirs).build();
    defaultResourceService = new DefaultResourceService(resourceServiceConfig);

    // When
    final KubernetesList result = defaultResourceService
        .generateResources(PlatformMode.kubernetes, enricherManager, kitLogger);

    // Then
    assertThat(result.getItems())
        .hasSize(3)
        .containsExactlyInAnyOrder(
            new ConfigMapBuilder().withNewMetadata().withName("test-profile").endMetadata()
                .withData(Collections.singletonMap("type", "test"))
                .build(),
            new ConfigMapBuilder().withNewMetadata().withName("common").endMetadata()
                .withData(Collections.singletonMap("type", "common"))
                .build(),
            new ConfigMapBuilder().withNewMetadata().withName("dev").endMetadata()
                .withData(Collections.singletonMap("type", "dev"))
                .build());
  }

  @SuppressWarnings("AccessStaticViaInstance")
  @Test
  public void writeResources(@Mocked WriteUtil writeUtil, @Mocked TemplateUtil templateUtil) throws IOException {
    // When
    defaultResourceService.writeResources(null, ResourceClassifier.KUBERNETES, kitLogger);
    // Then
    // @formatter:off
    new Verifications() {{
      writeUtil.writeResourcesIndividualAndComposite(
          null, new File(targetDir, "kubernetes"), ResourceFileType.yaml, kitLogger);
      times = 1;
      templateUtil.interpolateTemplateVariables(null, (File)any);
      times = 1;
    }};
    // @formatter:on
  }
}
