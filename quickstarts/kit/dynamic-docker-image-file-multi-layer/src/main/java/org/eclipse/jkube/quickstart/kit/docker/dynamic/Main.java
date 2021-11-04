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
package org.eclipse.jkube.quickstart.kit.docker.dynamic;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jkube.kit.build.service.docker.ServiceHub;
import org.eclipse.jkube.kit.build.service.docker.ServiceHubFactory;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

public class Main {

  private static final String IMAGE_NAME = "test-from-dynamic-dockerfile";
  public static void main(String[] args) {
    final KitLogger kitLogger = new KitLogger.StdoutLogger();
    kitLogger.info("Initiating default JKube configuration and required services...");
    kitLogger.info(" - Creating Docker Service Hub");
    final ServiceHub serviceHub = new ServiceHubFactory().createServiceHub(kitLogger);
    kitLogger.info(" - Creating Docker Build Service Configuration");
    final BuildServiceConfig dockerBuildServiceConfig = BuildServiceConfig.builder().build();
    kitLogger.info(" - Creating configuration for JKube");
    kitLogger.info(" - Current working directory is: %s", getProjectDir().toFile().toString());
    final JKubeConfiguration configuration = JKubeConfiguration.builder()
        .project(JavaProject.builder()
            .baseDirectory(getProjectDir().toFile())
            .build())
        .outputDirectory("target")
        .sourceDirectory(getProjectDir().toFile().getAbsolutePath())
        .build();

    JKubeServiceHub.JKubeServiceHubBuilder buildBuilder = JKubeServiceHub.builder()
        .log(kitLogger)
        .configuration(configuration)
        .platformMode(RuntimeMode.KUBERNETES)
        .dockerServiceHub(serviceHub)
        .buildServiceConfig(dockerBuildServiceConfig);
    try ( JKubeServiceHub jKubeServiceHub = buildBuilder.build()) {
      jKubeServiceHub.getBuildService().build(
          new DynamicDockerfileGenerator(kitLogger, getProjectDir(), IMAGE_NAME)
              .generateImageConfiguration());
      final String imageId = jKubeServiceHub.getDockerServiceHub().getDockerAccess().getImageId(IMAGE_NAME);
      kitLogger.info("Docker image built successfully (%s)!", imageId);
      System.exit(0);
    } catch (Exception ex) {
      ex.printStackTrace();
      kitLogger.error("Error occurred: '%s'", ex.getMessage());
    }
  }


  /**
   * Workaround so that this example can be invoked from different places (Inline run from IDE, Maven Exec plugin, etc.)
   */
  private static Path getProjectDir() {
    final Path currentWorkDir = Paths.get("");
    if (currentWorkDir.toAbsolutePath().endsWith("dynamic-docker-image-file-multi-layer")) {
      return currentWorkDir.toAbsolutePath();
    }
    return currentWorkDir.resolve("kit").resolve("dynamic-docker-image-file-multi-layer");
  }
}
