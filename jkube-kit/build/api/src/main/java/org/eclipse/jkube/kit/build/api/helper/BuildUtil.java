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
package org.eclipse.jkube.kit.build.api.helper;

import org.eclipse.jkube.kit.build.api.assembly.AssemblyManager;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class BuildUtil {

  private BuildUtil() {}

  /**
   * Extract base from image from BuildConfiguration
   * @param buildConfig from which to extract the base from image
   * @return the from image provided in the BuildConfiguration, DATA default in case there are no assemblies or null
   */
  public static String extractBaseFromConfiguration(@Nonnull BuildConfiguration buildConfig) {
    String fromImage;
    fromImage = buildConfig.getFrom();
    if (fromImage == null && buildConfig.getAssembly() == null) {
      fromImage = AssemblyManager.DEFAULT_DATA_BASE_IMAGE;
    }
    return fromImage;
  }

  public static String extractBaseFromDockerfile(@Nonnull JKubeConfiguration configuration, @Nonnull BuildConfiguration buildConfig) {
    String fromImage;
    try {
      File fullDockerFilePath = buildConfig.getAbsoluteDockerFilePath(configuration.getSourceDirectory(),
          Optional.ofNullable(configuration.getProject()).map(JavaProject::getBaseDirectory).map(File::toString).orElse(null));
      fromImage = DockerFileUtil.extractBaseImages(
          fullDockerFilePath, configuration.getProperties(), buildConfig.getFilter(), buildConfig.getArgs()).stream().findFirst().orElse(null);
    } catch (IOException e) {
      // Cant extract base image, so we wont try an auto pull. An error will occur later anyway when
      // building the image, so we are passive here.
      fromImage = null;
    }
    return fromImage;
  }
}
