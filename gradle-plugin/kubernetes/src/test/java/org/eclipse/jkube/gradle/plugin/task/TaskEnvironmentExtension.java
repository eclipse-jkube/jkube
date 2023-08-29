/*
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

import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.internal.plugins.DefaultPluginContainer;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPluginConvention;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.MockedConstruction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

public class TaskEnvironmentExtension implements BeforeEachCallback, AfterEachCallback {
  Project project;
  Logger logger;
  private File temporaryFolder;
  private MockedConstruction<DefaultTask> defaultTaskMockedConstruction;

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    temporaryFolder = Files.createTempDirectory("junit").toFile();
    project = mock(Project.class, RETURNS_DEEP_STUBS);
    logger = mock(Logger.class);
    when(project.getProjectDir()).thenReturn(temporaryFolder);
    when(project.getBuildDir()).thenReturn(Files.createDirectory(temporaryFolder.toPath().resolve("build")).toFile());
    when(project.getPlugins()).thenReturn(new DefaultPluginContainer(null, null, null));

    final ConfigurationContainer cc = mock(ConfigurationContainer.class);
    when(project.getConfigurations()).thenReturn(cc);
    List<Configuration> projectConfigurations = new ArrayList<>();
    when(cc.stream()).thenAnswer(i -> projectConfigurations.stream());
    when(cc.toArray()).thenAnswer(i -> projectConfigurations.toArray());

    when(project.getBuildscript().getConfigurations().stream()).thenAnswer(i -> Stream.empty());
    when(project.getConvention().getPlugin(JavaPluginConvention.class)).thenReturn(mock(JavaPluginConvention.class));
    when(project.getGradle().getStartParameter().getTaskNames()).thenReturn(Collections.emptyList());
    when(project.getGradle().getStartParameter().getSystemPropertiesArgs()).thenReturn(Collections.emptyMap());
    defaultTaskMockedConstruction = mockConstruction(DefaultTask.class, (mock, ctx) -> {
      when(mock.getProject()).thenReturn(project);
      when(mock.getLogger()).thenReturn(logger);
    });
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    defaultTaskMockedConstruction.close();
    FileUtils.deleteDirectory(temporaryFolder);
  }

  public File getRoot() {
    return temporaryFolder;
  }

  public void withKubernetesManifest() throws IOException {
    final File manifestsDir = Files
        .createDirectories(temporaryFolder.toPath().resolve("build").resolve("classes").resolve("java")
            .resolve("main").resolve("META-INF").resolve("jkube"))
        .toFile();
    Files.createFile(manifestsDir.toPath().resolve("kubernetes.yml"));
  }

  public void withKubernetesTemplate() throws IOException {
    Files.createDirectories(temporaryFolder.toPath().resolve("build").resolve("classes").resolve("java")
        .resolve("main").resolve("META-INF").resolve("jkube").resolve("kubernetes"));
  }

}
