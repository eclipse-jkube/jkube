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

import org.eclipse.jkube.kit.common.util.ClassUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import static org.eclipse.jkube.kit.common.assertj.FileAssertions.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class BuildPluginsHelperTest {
  private JavaProject javaProject;
  private KitLogger kitLogger;
  @TempDir
  File temporaryFolder;

  @BeforeEach
  public void setup() {
    javaProject = mock(JavaProject.class, RETURNS_DEEP_STUBS);
    kitLogger = mock(KitLogger.class, RETURNS_DEEP_STUBS);
  }

  @Test
  void executeBuildPlugins_whenNoDescriptor_shouldDoNothing() {
    // Given
    try (MockedStatic<ClassUtil> classUtilMockedStatic = mockStatic(ClassUtil.class)) {
      classUtilMockedStatic.when(() -> ClassUtil.getResourcesInContextClassloader("META-INF/maven/org.eclipse.jkube/jkube-plugin"))
          .thenReturn(Collections.emptyEnumeration());
      // When
      BuildPluginsHelper.executeBuildPlugins(javaProject, kitLogger);

      // Then
      assertThat(temporaryFolder).isEmptyDirectory();
    }
  }


  @Test
  void executeBuildPlugins_withDescriptor_shouldDoNothing() throws IOException {
    // Given
    try (MockedStatic<ClassUtil> classUtilMockedStatic = mockStatic(ClassUtil.class)) {
      when(javaProject.getBuildDirectory()).thenReturn(temporaryFolder);
      classUtilMockedStatic.when(() -> ClassUtil.getResourcesInContextClassloader("META-INF/maven/org.eclipse.jkube/jkube-plugin"))
          .thenReturn(Collections.enumeration(Collections.singletonList(new URL("jar:file:/tmp/run-java-sh-1.3.8.jar!/META-INF/maven/org.eclipse.jkube/jkube-plugin"))));
      // When
      BuildPluginsHelper.executeBuildPlugins(javaProject, kitLogger);

      // Then
      assertThat(temporaryFolder).fileTree().contains("docker-extra");
    }
  }
}
