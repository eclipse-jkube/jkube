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

import org.eclipse.jgit.util.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.internal.plugins.DefaultPluginContainer;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPluginConvention;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedConstruction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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
    when(project.getPlugins()).thenReturn(new DefaultPluginContainer(null, null, null));

    final ConfigurationContainer cc = mock(ConfigurationContainer.class);
    when(project.getConfigurations()).thenReturn(cc);
    List<Configuration> projectConfigurations = new ArrayList<Configuration>();
    when(cc.stream()).thenAnswer(i -> projectConfigurations.stream());
    when(cc.toArray()).thenAnswer(i -> projectConfigurations.toArray());

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

  public void withKubernetesManifest() throws IOException {
    final File manifestsDir = newFolder("build", "classes", "java", "main", "META-INF", "jkube");
    FileUtils.touch(new File(manifestsDir, "kubernetes.yml").toPath());
  }

  public void withKubernetesTemplate() throws IOException {
    newFolder("build", "classes", "java", "main", "META-INF", "jkube", "kubernetes");
  }
}
