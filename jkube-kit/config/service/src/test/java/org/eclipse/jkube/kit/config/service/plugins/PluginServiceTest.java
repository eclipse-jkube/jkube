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
package org.eclipse.jkube.kit.config.service.plugins;


import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.api.JKubePlugin;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.LazyBuilder;
import org.eclipse.jkube.kit.config.resource.ResourceServiceConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PluginServiceTest {

  @TempDir
  File temporaryFolder;

  private KitLogger logger;
  private JKubeServiceHub jKubeServiceHub;

  @BeforeEach
  void setUp() {
    logger = spy(new KitLogger.SilentLogger());
    jKubeServiceHub = new JKubeServiceHub(null, RuntimeMode.KUBERNETES, logger, null,
      JKubeConfiguration.builder()
        .project(JavaProject.builder()
          .outputDirectory(temporaryFolder)
          .build())
        .build(),
      new BuildServiceConfig(), new ResourceServiceConfig(), new LazyBuilder<>(hub -> null), true);
  }

  @Test
  void addFiles_failsIfCantCreateExtraDirectory() throws IOException {
    // Given
    Files.createFile(temporaryFolder.toPath().resolve("jkube-extra"));
    final PluginService pluginService = jKubeServiceHub.getPluginManager().resolvePluginService();
    // When & Then
    assertThatExceptionOfType(JKubeServiceException.class)
        .isThrownBy(pluginService::addExtraFiles)
        .withMessageContaining("Unable to create the jkube-extra directory");
  }

  @Test
  void addFiles_shouldLogDebugMessages() throws JKubeServiceException {
    // When
    jKubeServiceHub.getPluginManager().resolvePluginService().addExtraFiles();
    // Then
    verify(logger, times(3))
      .debug(eq("Adding extra files for plugin %s"), anyString());
    verify(logger, times(2))
      .debug(eq("Extra files for plugin %s added"), anyString());
    verify(logger, times(1))
      .debug(eq("Problem adding extra files for plugin %s: %s"), anyString(), eq("Should fail silently"));
  }

  @Test
  void addFiles_shouldProcessFiles() throws JKubeServiceException {
    // When
    jKubeServiceHub.getPluginManager().resolvePluginService().addExtraFiles();
    // Then
    assertThat(new File(temporaryFolder, "jkube-extra"))
      .isDirectoryContaining(f -> f.getName().equals("file-added"));
  }

  public static final class PluginOne implements JKubePlugin {
    @Override
    public void addExtraFiles(File targetDir) {
      try {
        FileUtils.touch(new File(targetDir, "file-added"));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static final class PluginTwo implements JKubePlugin {
    @Override
    public void addExtraFiles(File targetDir) {
      throw new RuntimeException("Should fail silently");
    }
  }

  public static final class PluginThree implements JKubePlugin {
  }

}
