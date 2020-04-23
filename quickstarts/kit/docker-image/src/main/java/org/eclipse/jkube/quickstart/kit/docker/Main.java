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

import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ServiceHub;
import org.eclipse.jkube.kit.build.service.docker.ServiceHubFactory;
import org.eclipse.jkube.kit.build.service.docker.config.RunImageConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.JKubeConfiguration;
import org.eclipse.jkube.kit.config.image.build.Arguments;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

public class Main {
  public static void main(String[] args) {
    final KitLogger kitLogger = new KitLogger.StdoutLogger();
    kitLogger.info("Initiating default JKube configuration and required services...");
    kitLogger.info(" - Creating Docker Service Hub");
    final ServiceHub serviceHub = new ServiceHubFactory().createServiceHub(kitLogger);
    kitLogger.info(" - Creating Docker Build Service Configuration");
    final BuildServiceConfig dockerBuildServiceConfig = BuildServiceConfig.builder().build();
    kitLogger.info(" - Creating configuration for JKube");
    final JKubeConfiguration configuration = JKubeConfiguration.builder()
        .project(JavaProject.builder().build())
        .outputDirectory(new File("target").getAbsolutePath())
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
            .cmd(Arguments.builder().shell("/bin/sh").build())
            .build())
        .build();
    try (
        JKubeServiceHub jKubeServiceHub = JKubeServiceHub.builder()
            .log(kitLogger)
            .configuration(configuration)
            .platformMode(RuntimeMode.kubernetes)
            .dockerServiceHub(serviceHub)
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
}
