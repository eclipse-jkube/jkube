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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.ResourceFileType;
import org.eclipse.jkube.kit.common.util.ResourceClassifier;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.EnricherManager;
import org.eclipse.jkube.kit.config.service.ResourceServiceConfig;

import io.fabric8.kubernetes.api.model.KubernetesList;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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

  private File targetDir;
  private ResourceServiceConfig resourceServiceConfig;
  private DefaultResourceService defaultResourceService;

  @Before
  public void init() throws IOException {
    targetDir = temporaryFolder.newFolder("target");
    resourceServiceConfig = ResourceServiceConfig.builder()
        .interpolateTemplateParameters(true)
        .targetDir(targetDir)
        .resourceFileType(ResourceFileType.yaml)
        .resourceDir(temporaryFolder.newFolder("resources"))
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
