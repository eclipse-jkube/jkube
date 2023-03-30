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
package org.eclipse.jkube.quickstart.kit.docker.dynamic;

import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.Arguments;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.DockerFileBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

public class DynamicDockerfileGenerator {

  private static final String ASSEMBLY_DIRECTORY = "preassembled-docker";

  private final KitLogger kitLogger;
  private final Path projectDir;
  private final String imageName;

  public DynamicDockerfileGenerator(KitLogger kitLogger, Path projectDir, String imageName) {
    this.kitLogger = kitLogger;
    this.projectDir = projectDir;
    this.imageName = imageName;
  }

  public ImageConfiguration generateImageConfiguration() throws IOException {
    final File context = assembleContext();
    generateDockerfile(context);
    BuildConfiguration buildConfiguration = initBuildConfiguration(context);
    return ImageConfiguration.builder()
        .name(imageName)
        .build(buildConfiguration)
        .build();
  }

  /**
   * Preassembles all files that should be included in the Docker build.
   *
   * Will create a temporary directory holding these files.
   *
   * Customize this step to add any files you'd like to be part of your image.
   */
  private File assembleContext() throws IOException {
    kitLogger.info("Assembling context for Dockerfile");
    final File contextDirectory = projectDir.resolve("target").resolve(ASSEMBLY_DIRECTORY).toFile();
    FileUtils.copyDirectoryToDirectory(
        projectDir.resolve("src").resolve("main").resolve("static").toFile(),
        contextDirectory
    );
    FileUtils.copyDirectoryToDirectory(
        projectDir.resolve("src").resolve("main").resolve("other-directory").toFile(),
        contextDirectory
    );
    return contextDirectory;
  }

  /**
   * Generates a Dockerfile dynamically with any entries you wish and writes it to the provided directory
   */
  private void generateDockerfile(File targetDirectory) throws IOException {
    kitLogger.info("Generating dynamic Dockerfile");
    new DockerFileBuilder()
        .basedir("/")
        .baseImage("busybox")
        .expose(Collections.singletonList("8080"))
        .env(Collections.singletonMap("JAVA_OPTIONS", "-Xmx1500m"))
        .add("/static/hello-docker.txt", "/home/app/file.txt")
        .add("/static/hello-docker-other.txt", "/home/app/file-2.txt")
        .add("/other-directory/*.txt", "/home/resources/")
        .cmd(Arguments.builder().shell("/bin/sh").build())
        .write(targetDirectory);
  }

  /**
   * Generates a Dockerfile dynamically with any entries you wish and writes it to the provided directory
   */
  private BuildConfiguration initBuildConfiguration(File contextDir) {
    kitLogger.info("Preparing build configuration for Dynamic Dockerfile mode");
    final BuildConfiguration buildConfiguration = BuildConfiguration.builder()
        .contextDir(contextDir.getAbsolutePath())
        .dockerFile("Dockerfile")
        .assembly(AssemblyConfiguration.builder()
            .name("ContextAssembly")
            .targetDir("/")
            .build())
        .build();
    buildConfiguration.initAndValidate();
    return buildConfiguration;
  }
}
