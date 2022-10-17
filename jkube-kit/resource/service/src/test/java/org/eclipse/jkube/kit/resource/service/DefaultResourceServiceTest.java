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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.ResourceFileType;
import org.eclipse.jkube.kit.common.service.SummaryService;
import org.eclipse.jkube.kit.common.util.ResourceClassifier;
import org.eclipse.jkube.kit.config.resource.EnricherManager;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;

import io.fabric8.kubernetes.api.model.KubernetesList;
import org.eclipse.jkube.kit.config.resource.ResourceServiceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

class DefaultResourceServiceTest {

  private EnricherManager enricherManager;
  private KitLogger kitLogger;
  private File targetDir;
  private ResourceServiceConfig resourceServiceConfig;
  private DefaultResourceService defaultResourceService;
  private SummaryService summaryService;

  @BeforeEach
  void init(@TempDir Path temporaryFolder) throws IOException {
    enricherManager = mock(EnricherManager.class);
    kitLogger = new KitLogger.SilentLogger();
    summaryService = mock(SummaryService.class);
    targetDir = Files.createDirectory(temporaryFolder.resolve("target")).toFile();
    resourceServiceConfig = ResourceServiceConfig.builder()
        .interpolateTemplateParameters(true)
        .targetDir(targetDir)
        .project(JavaProject.builder().build())
        .resourceFileType(ResourceFileType.yaml)
        .resourceDirs(Collections.singletonList(Files.createDirectory(temporaryFolder.resolve("resources")).toFile()))
        .resourceConfig(ResourceConfig.builder().build())
        .build();
    defaultResourceService = new DefaultResourceService(resourceServiceConfig);
  }

  @Test
  void generateResourcesWithNoResourcesShouldReturnEmpty() throws IOException {
    // When
    final KubernetesList result = defaultResourceService
        .generateResources(PlatformMode.kubernetes, enricherManager, kitLogger);
    // Then
    assertThat(result.getItems()).isEmpty();
  }

  @Test
  void generateResources_withResources_shouldReturnKubernetesResourceList() throws IOException {
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

  @Test
  void writeResources() throws IOException {
    try (
      MockedStatic<WriteUtil> writeUtil = mockStatic(WriteUtil.class);
      MockedStatic<TemplateUtil> templateUtil = mockStatic(TemplateUtil.class)
    ) {
      // When
      defaultResourceService.writeResources(null, ResourceClassifier.KUBERNETES, kitLogger, summaryService);
      // Then
      writeUtil.verify(() -> WriteUtil.writeResourcesIndividualAndComposite(isNull(), eq(new File(targetDir, "kubernetes")), eq(ResourceFileType.yaml), eq(kitLogger), eq(summaryService)), times(1));
      templateUtil.verify(() -> TemplateUtil.interpolateTemplateVariables(isNull(), any()), times(1));
    }
  }
}
