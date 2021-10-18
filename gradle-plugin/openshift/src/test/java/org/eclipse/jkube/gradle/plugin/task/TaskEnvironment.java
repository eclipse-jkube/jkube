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
package org.eclipse.jkube.gradle.plugin.task;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import org.eclipse.jgit.util.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPluginConvention;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedConstruction;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

public class TaskEnvironment extends TemporaryFolder {

  private MockedConstruction<DefaultTask> defaultTaskMockedConstruction;
  Logger logger;
  Project project;

  @Override
  protected void before() throws Throwable {
    super.before();
    project = mock(Project.class, RETURNS_DEEP_STUBS);
    logger = mock(Logger.class);
    when(project.getProjectDir()).thenReturn(getRoot());
    when(project.getBuildDir()).thenReturn(newFolder("build"));
    when(project.getConfigurations().stream()).thenAnswer(i -> Stream.empty());
    when(project.getBuildscript().getConfigurations().stream()).thenAnswer(i -> Stream.empty());
    when(project.getConvention().getPlugin(JavaPluginConvention.class)).thenReturn(mock(JavaPluginConvention.class));
    defaultTaskMockedConstruction = mockConstruction(DefaultTask.class, (mock, ctx) -> {
      when(mock.getProject()).thenReturn(project);
      when(mock.getLogger()).thenReturn(logger);
    });
  }

  @Override
  protected void after() {
    super.after();
    defaultTaskMockedConstruction.close();
  }

  public void withOpenShiftManifest() throws IOException {
    final File manifestsDir = newFolder("build", "classes", "java", "main", "META-INF", "jkube");
    FileUtils.touch(new File(manifestsDir, "openshift.yml").toPath());
  }
}
