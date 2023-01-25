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
package org.eclipse.jkube.quickstart.kit.docker;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jkube.kit.build.service.docker.DockerServiceHub;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.RunImageConfiguration;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.config.image.build.Arguments;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

public class Main {

  public static void main(String[] args) {
    final KitLogger kitLogger = new KitLogger.StdoutLogger();
    kitLogger.info("Initiating default JKube configuration and required services...");
    kitLogger.info(" - Creating Docker Build Service Configuration");
    final BuildServiceConfig dockerBuildServiceConfig = BuildServiceConfig.builder().build();
    kitLogger.info(" - Creating configuration for JKube");
    kitLogger.info(" - Current working directory is: %s", getProjectDir().toFile().toString());
    final JKubeConfiguration configuration = JKubeConfiguration.builder()
        .project(JavaProject.builder()
            .baseDirectory(getProjectDir().toFile())
            .build())
        .outputDirectory("target")
        .build();

    kitLogger.info("Creating configuration for example Docker Image");
    final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
        .name("jkube-example")
        .run(RunImageConfiguration.builder().user("1000").build())
        .build(BuildConfiguration.builder()
            .putEnv("MY_VAR", "value")
            .putEnv("MY_OTHER_VAR", "true")
            .label("maintainer", "JKube Devs")
            .port("80/tcp")
            .maintainer("JKube Devs")
            .from("busybox")
            .assembly(AssemblyConfiguration.builder()
                .targetDir("/")
                .layer(Assembly.builder()
                    .fileSet(AssemblyFileSet.builder()
                        .directory(new File("static"))
                        .build())
                    .build())
                .build()
            )
            .cmd(Arguments.builder().shell("/bin/sh").build())
            .build())
        .build();
    try (
        JKubeServiceHub jKubeServiceHub = JKubeServiceHub.builder()
            .log(kitLogger)
            .configuration(configuration)
            .platformMode(RuntimeMode.KUBERNETES)
            .dockerServiceHub(DockerServiceHub.newInstance(kitLogger))
            .buildServiceConfig(dockerBuildServiceConfig)
            .build()) {
      jKubeServiceHub.getBuildService().build(imageConfiguration);
      final String imageId = jKubeServiceHub.getDockerServiceHub().getDockerAccess().getImageId("jkube-example");
      kitLogger.info("Docker image built successfully (%s)!", imageId);
      System.exit(0);
    } catch (Exception ex) {
      kitLogger.error("Error occurred: '%s'", ex.getMessage());
    }
  }

  private static Path getProjectDir() {
    final Path currentWorkDir = Paths.get("");
    if (currentWorkDir.toAbsolutePath().endsWith("docker-image")) {
      return currentWorkDir.toAbsolutePath();
    }
    return currentWorkDir.resolve("quickstarts").resolve("kit").resolve("docker-image");
  }
}
