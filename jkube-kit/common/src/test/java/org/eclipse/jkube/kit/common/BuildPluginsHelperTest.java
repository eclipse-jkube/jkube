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
package org.eclipse.jkube.kit.common;

import org.eclipse.jkube.kit.api.JKubeBuildPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.ServiceLoader;

import static org.eclipse.jkube.kit.common.assertj.FileAssertions.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BuildPluginsHelperTest {
  private JavaProject javaProject;
  private KitLogger kitLogger;
  private ServiceLoader<JKubeBuildPlugin> mockedServiceLoader;
  private Iterator<JKubeBuildPlugin> jKubeBuildPluginIterator;
  @TempDir
  private File temporaryFolder;

  @BeforeEach
  public void setup() {
    javaProject = mock(JavaProject.class, RETURNS_DEEP_STUBS);
    kitLogger = mock(KitLogger.class, RETURNS_DEEP_STUBS);
    mockedServiceLoader = mock(ServiceLoader.class);
    jKubeBuildPluginIterator = mock(Iterator.class);
    when(mockedServiceLoader.iterator()).thenReturn(jKubeBuildPluginIterator);
  }

  @Test
  void executeBuildPlugins_whenServiceLoaderReturnsNoImplementation_shouldDoNothing() throws IOException {
    // Given
    when(javaProject.getBuildDirectory()).thenReturn(temporaryFolder);

    // When
    BuildPluginsHelper.executeBuildPlugins(javaProject, kitLogger);

    // Then
    assertThat(temporaryFolder).fileTree().contains("docker-extra");
  }

  @Test
  void executeBuildPlugins_whenServiceLoaderReturnsValidImplementation_thenShouldCopyFilesInDockerExtraDir() throws IOException {
    try (MockedStatic<ServiceLoader> serviceLoaderMockedStatic = mockStatic(ServiceLoader.class)) {
      when(jKubeBuildPluginIterator.hasNext()).thenReturn(true).thenReturn(false);
      when(jKubeBuildPluginIterator.next()).thenReturn(new TestJKubePlugin());
      serviceLoaderMockedStatic.when(() -> ServiceLoader.load(any(), (ClassLoader) any()))
          .thenReturn(mockedServiceLoader).thenCallRealMethod();
      // Given
      when(javaProject.getBuildDirectory()).thenReturn(temporaryFolder);

      // When
      BuildPluginsHelper.executeBuildPlugins(javaProject, kitLogger);

      // Then
      assertThat(temporaryFolder).fileTree().contains("docker-extra", "docker-extra/test-file");
    }
  }

  @Test
  void executeBuildPlugins_whenLoadedImplThrowsException_thenLogMessage() throws IOException {
    try (MockedStatic<ServiceLoader> serviceLoaderMockedStatic = mockStatic(ServiceLoader.class)) {
      JKubeBuildPlugin mockedJKubeBuildPlugin = mock(JKubeBuildPlugin.class);
      IOException expectedThrownIOException = new IOException("I/O failure");
      when(mockedJKubeBuildPlugin.getName()).thenReturn("MockedJKubeBuildPlugin");
      doThrow(expectedThrownIOException).when(mockedJKubeBuildPlugin).addExtraFiles(any());
      when(jKubeBuildPluginIterator.hasNext()).thenReturn(true).thenReturn(false);
      when(jKubeBuildPluginIterator.next()).thenReturn(mockedJKubeBuildPlugin);
      serviceLoaderMockedStatic.when(() -> ServiceLoader.load(any(), (ClassLoader) any()))
          .thenReturn(mockedServiceLoader).thenCallRealMethod();
      // Given
      when(javaProject.getBuildDirectory()).thenReturn(temporaryFolder);

      // When
      BuildPluginsHelper.executeBuildPlugins(javaProject, kitLogger);

      // Then
      verify(kitLogger, times(1)).verbose("Failure while extracting files from %s, %s", "MockedJKubeBuildPlugin", expectedThrownIOException);
    }
  }

  private static class TestJKubePlugin implements JKubeBuildPlugin {
    @Override
    public void addExtraFiles(File targetDir) throws IOException {
      File newFileToCreate = new File(targetDir, "test-file");
      if (!newFileToCreate.createNewFile()) {
        throw new IOException("Failure in creating file");
      }
    }

    @Override
    public String getName() {
      return getClass().getName();
    }
  }
}