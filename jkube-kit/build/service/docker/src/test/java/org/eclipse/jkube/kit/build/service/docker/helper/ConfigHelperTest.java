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
package org.eclipse.jkube.kit.build.service.docker.helper;

import mockit.Expectations;
import mockit.Mocked;
import org.eclipse.jkube.kit.build.service.docker.config.handler.ImageConfigResolver;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class ConfigHelperTest {
  @Mocked
  private ImageConfigResolver imageConfigResolver;

  @Mocked
  private KitLogger logger;

  @Mocked
  private JavaProject javaProject;

  @Test
  public void initImageConfiguration_withSimpleImageConfiguration_shouldReturnImageConfiguration() {
    // Given
    ImageConfiguration dummyImageConfiguration = ImageConfiguration.builder()
        .name("foo/bar:latest")
        .build(BuildConfiguration.builder()
            .from("foobase:latest")
            .build())
        .build();
    List<ImageConfiguration> images = new ArrayList<>();
    images.add(dummyImageConfiguration);
    new Expectations() {{
      imageConfigResolver.resolve(dummyImageConfiguration, javaProject);
      result = dummyImageConfiguration;

      javaProject.getBaseDirectory();
      result = new File("dummydir");
    }};

    // When
    List<ImageConfiguration> resolvedImages = ConfigHelper.initImageConfiguration("1.12", new Date(), javaProject, images, imageConfigResolver, logger, null, configs -> configs);

    // Then
    assertThat(resolvedImages)
        .isNotNull()
        .hasSize(1)
        .element(0).isEqualTo(dummyImageConfiguration);
  }

  @Test
  public void initImageConfiguration_withSimpleDockerFileInProjectBaseDir_shouldCreateImageConfiguration() {
    List<ImageConfiguration> images = new ArrayList<>();
    File dockerFile = new File(getClass().getResource("/dummy-javaproject/Dockerfile").getFile());
    new Expectations() {{
      javaProject.getBaseDirectory();
      result = dockerFile.getParentFile();

      javaProject.getProperties();
      result = new Properties();

      javaProject.getGroupId();
      result = "org.eclipse.jkube";

      javaProject.getArtifactId();
      result = "test-java-project";

      javaProject.getVersion();
      result = "0.0.1-SNAPSHOT";
    }};

    // When
    List<ImageConfiguration> resolvedImages = ConfigHelper.initImageConfiguration("1.12", new Date(), javaProject, images, imageConfigResolver, logger, null, configs -> configs);

    // Then
    assertThat(resolvedImages)
        .isNotNull()
        .hasSize(1)
        .element(0)
        .hasFieldOrPropertyWithValue("name", "jkube/test-java-project:latest")
        .hasFieldOrPropertyWithValue("build.dockerFile", dockerFile)
        .hasFieldOrPropertyWithValue("build.ports", Collections.singletonList("8080"));
  }

  @Test
  public void initImageConfiguration_withSimpleDockerFileModeEnabledAndImageConfigurationWithNoBuild_shouldModifyExistingImageConfiguration() {
    ImageConfiguration dummyImageConfiguration = ImageConfiguration.builder()
        .name("imageconfiguration-no-build:latest")
        .build();
    List<ImageConfiguration> images = new ArrayList<>();
    images.add(dummyImageConfiguration);
    File dockerFile = new File(getClass().getResource("/dummy-javaproject/Dockerfile").getFile());
    new Expectations() {{
      imageConfigResolver.resolve(dummyImageConfiguration, javaProject);
      result = dummyImageConfiguration;

      javaProject.getBaseDirectory();
      result = dockerFile.getParentFile();

      javaProject.getProperties();
      result = new Properties();
    }};

    // When
    List<ImageConfiguration> resolvedImages = ConfigHelper.initImageConfiguration("1.12", new Date(), javaProject, images, imageConfigResolver, logger, null, configs -> configs);

    // Then
    assertThat(resolvedImages)
        .isNotNull()
        .hasSize(1)
        .element(0)
        .hasFieldOrPropertyWithValue("name", "imageconfiguration-no-build:latest")
        .hasFieldOrPropertyWithValue("build.dockerFile", dockerFile)
        .hasFieldOrPropertyWithValue("build.ports", Collections.singletonList("8080"));
  }
}
